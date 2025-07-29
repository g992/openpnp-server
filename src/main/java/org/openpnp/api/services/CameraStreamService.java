package org.openpnp.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.pmw.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис для управления WebSocket стримами камер
 */
public class CameraStreamService {

    private static final ConcurrentHashMap<String, CameraStreamSession> sessions = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * Запрос на стрим камеры
     */
    public static class StreamRequest {
        public String cameraId;
        public int fps;
        public String quality; // "low", "medium", "high"
    }

    /**
     * Сессия стрима камеры
     */
    private static class CameraStreamSession {
        private final String sessionId;
        private final String cameraId;
        private final WsContext wsContext;
        private final int fps;
        private final String quality;
        private final ScheduledFuture<?> streamTask;
        private final AtomicBoolean isActive;
        private long lastErrorTime = 0;
        private static final long ERROR_THROTTLE_MS = 5000; // 5 секунд между ошибками

        public CameraStreamSession(String sessionId, String cameraId, WsContext wsContext, int fps, String quality) {
            this.sessionId = sessionId;
            this.cameraId = cameraId;
            this.wsContext = wsContext;
            this.fps = fps;
            this.quality = quality;
            this.isActive = new AtomicBoolean(true);

            // Запускаем поток стрима
            this.streamTask = scheduler.scheduleAtFixedRate(
                    this::streamFrame,
                    0,
                    1000 / fps,
                    TimeUnit.MILLISECONDS);

            Logger.info("Создана сессия стрима камеры: {} для камеры: {} с FPS: {}", sessionId, cameraId, fps);
        }

        private void streamFrame() {
            if (!isActive.get()) {
                return;
            }

            try {
                Machine machine = Configuration.get().getMachine();
                Camera camera = machine.getCamera(cameraId);

                if (camera == null) {
                    // Проверяем камеры в головках
                    for (var head : machine.getHeads()) {
                        camera = head.getCamera(cameraId);
                        if (camera != null) {
                            break;
                        }
                    }
                }

                if (camera == null) {
                    sendError("Камера с ID '" + cameraId + "' не найдена");
                    return;
                }

                // Захватываем изображение
                BufferedImage image = camera.capture();
                if (image == null) {
                    sendError("Не удалось захватить изображение с камеры");
                    return;
                }

                // Проверяем валидность изображения
                if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                    sendError("Получено изображение с некорректными размерами: " + image.getWidth() + "x"
                            + image.getHeight());
                    return;
                }

                // Логируем информацию об изображении для диагностики
                Logger.debug("Камера {}: тип изображения: {}, размер: {}x{}, цветовая модель: {}, битовая глубина: {}",
                        cameraId, image.getType(), image.getWidth(), image.getHeight(),
                        image.getColorModel().getClass().getSimpleName(),
                        image.getColorModel().getPixelSize());

                // Конвертируем в Base64
                String base64Image;
                try {
                    base64Image = convertToBase64(image, quality);
                } catch (Exception e) {
                    Logger.warn("Не удалось конвертировать изображение камеры {}, используем тестовое изображение: {}",
                            cameraId, e.getMessage());

                    // Создаем тестовое изображение
                    BufferedImage testImage = createTestImage(image.getWidth(), image.getHeight());
                    base64Image = convertToBase64(testImage, quality);
                }

                // Отправляем данные
                CameraFrameData frameData = new CameraFrameData();
                frameData.type = "frame";
                frameData.cameraId = cameraId;
                frameData.timestamp = System.currentTimeMillis();
                frameData.image = base64Image;
                frameData.width = image.getWidth();
                frameData.height = image.getHeight();
                frameData.fps = fps;

                String json = objectMapper.writeValueAsString(frameData);
                wsContext.send(json);

            } catch (Exception e) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastErrorTime > ERROR_THROTTLE_MS) {
                    Logger.error("Ошибка стрима камеры {}: {}", cameraId, e.getMessage(), e);
                    sendError("Ошибка стрима: " + e.getMessage());
                    lastErrorTime = currentTime;
                } else {
                    Logger.debug("Ошибка стрима камеры {} (подавлена): {}", cameraId, e.getMessage());
                }
            }
        }

        private String convertToBase64(BufferedImage image, String quality) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Настройки качества
            float compressionQuality = 0.8f; // по умолчанию
            switch (quality) {
                case "low":
                    compressionQuality = 0.5f;
                    break;
                case "medium":
                    compressionQuality = 0.8f;
                    break;
                case "high":
                    compressionQuality = 0.95f;
                    break;
            }

            // Принудительно конвертируем изображение в стандартный формат
            BufferedImage standardImage = forceConvertToStandard(image);

            // Конвертируем изображение в RGB формат если необходимо
            BufferedImage rgbImage = convertToRGB(standardImage);

            try {
                // Конвертируем в JPEG с настраиваемым качеством
                javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(compressionQuality);

                javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(rgbImage, null, null), param);
                writer.dispose();
                ios.close();

                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (Exception e) {
                Logger.warn("Ошибка при конвертации в JPEG, пробуем альтернативный метод: {}", e.getMessage());

                // Альтернативный метод - используем стандартный ImageIO.write
                baos.reset();
                if (ImageIO.write(rgbImage, "jpeg", baos)) {
                    return Base64.getEncoder().encodeToString(baos.toByteArray());
                } else {
                    // Если JPEG не работает, пробуем PNG
                    Logger.warn("JPEG не работает, пробуем PNG формат");
                    baos.reset();
                    if (ImageIO.write(rgbImage, "png", baos)) {
                        return Base64.getEncoder().encodeToString(baos.toByteArray());
                    } else {
                        throw new Exception("Не удалось конвертировать изображение ни в JPEG, ни в PNG");
                    }
                }
            }
        }

        /**
         * Конвертирует изображение в RGB формат для совместимости с JPEG
         */
        private BufferedImage convertToRGB(BufferedImage image) {
            // Всегда конвертируем для надежности
            Logger.debug("Конвертируем изображение из типа {} в RGB", image.getType());

            try {
                // Создаем новое RGB изображение
                BufferedImage rgbImage = new BufferedImage(
                        image.getWidth(),
                        image.getHeight(),
                        BufferedImage.TYPE_INT_RGB);

                // Копируем пиксели через Graphics2D
                java.awt.Graphics2D g2d = rgbImage.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();

                return rgbImage;
            } catch (Exception e) {
                Logger.warn("Ошибка при конвертации изображения типа {}: {}", image.getType(), e.getMessage());

                // Пробуем альтернативный метод - создаем изображение из данных пикселей
                try {
                    return createRGBFromPixels(image);
                } catch (Exception e2) {
                    Logger.warn("Альтернативная конвертация также не удалась: {}", e2.getMessage());

                    // Создаем пустое RGB изображение как fallback
                    BufferedImage fallbackImage = new BufferedImage(
                            image.getWidth(),
                            image.getHeight(),
                            BufferedImage.TYPE_INT_RGB);

                    // Заполняем черным цветом
                    java.awt.Graphics2D g2d = fallbackImage.createGraphics();
                    g2d.setColor(java.awt.Color.BLACK);
                    g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
                    g2d.dispose();

                    return fallbackImage;
                }
            }
        }

        /**
         * Альтернативный метод конвертации через данные пикселей
         */
        private BufferedImage createRGBFromPixels(BufferedImage image) {
            int width = image.getWidth();
            int height = image.getHeight();

            BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // Получаем данные пикселей
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            // Устанавливаем пиксели в RGB изображение
            rgbImage.setRGB(0, 0, width, height, pixels, 0, width);

            return rgbImage;
        }

        /**
         * Принудительно конвертирует изображение в стандартный формат
         */
        private BufferedImage forceConvertToStandard(BufferedImage image) {
            try {
                // Создаем новое изображение с принудительной конвертацией
                BufferedImage converted = new BufferedImage(
                        image.getWidth(),
                        image.getHeight(),
                        BufferedImage.TYPE_INT_RGB);

                // Используем более надежный метод копирования
                java.awt.Graphics2D g2d = converted.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                        java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();

                return converted;
            } catch (Exception e) {
                Logger.warn("Принудительная конвертация не удалась: {}", e.getMessage());
                return image; // Возвращаем оригинал если не удалось
            }
        }

        /**
         * Создает тестовое изображение с информацией о камере
         */
        private BufferedImage createTestImage(int width, int height) {
            BufferedImage testImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = testImage.createGraphics();

            // Заполняем фон
            g2d.setColor(java.awt.Color.DARK_GRAY);
            g2d.fillRect(0, 0, width, height);

            // Добавляем текст
            g2d.setColor(java.awt.Color.WHITE);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
            String text = "Camera: " + cameraId;
            java.awt.FontMetrics fm = g2d.getFontMetrics();
            int textX = (width - fm.stringWidth(text)) / 2;
            int textY = height / 2;
            g2d.drawString(text, textX, textY);

            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            String info = "Stream Error - Using Test Image";
            fm = g2d.getFontMetrics();
            textX = (width - fm.stringWidth(info)) / 2;
            textY = height / 2 + 20;
            g2d.drawString(info, textX, textY);

            g2d.dispose();
            return testImage;
        }

        private void sendError(String error) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastErrorTime > ERROR_THROTTLE_MS) {
                try {
                    CameraFrameData errorData = new CameraFrameData();
                    errorData.type = "error";
                    errorData.cameraId = cameraId;
                    errorData.timestamp = currentTime;
                    errorData.error = error;

                    String json = objectMapper.writeValueAsString(errorData);
                    wsContext.send(json);
                    lastErrorTime = currentTime;
                } catch (Exception e) {
                    Logger.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage());
                }
            }
        }

        public void stop() {
            isActive.set(false);
            if (streamTask != null && !streamTask.isCancelled()) {
                streamTask.cancel(true);
            }
            Logger.info("Остановлена сессия стрима камеры: {} для камеры: {}", sessionId, cameraId);
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getCameraId() {
            return cameraId;
        }

        public WsContext getWsContext() {
            return wsContext;
        }
    }

    /**
     * Данные кадра камеры
     */
    public static class CameraFrameData {
        public String type; // "frame" или "error"
        public String cameraId;
        public long timestamp;
        public String image; // Base64 изображение
        public int width;
        public int height;
        public int fps;
        public String error; // Сообщение об ошибке
    }

    /**
     * Начать стрим камеры
     */
    public static void startCameraStream(WsContext ctx, StreamRequest request) {
        String sessionId = ctx.sessionId();

        // Проверяем параметры
        if (request.cameraId == null || request.cameraId.isEmpty()) {
            sendError(ctx, "ID камеры не указан");
            return;
        }

        if (request.fps <= 0 || request.fps > 30) {
            request.fps = 10; // FPS по умолчанию
        }

        if (request.quality == null || request.quality.isEmpty()) {
            request.quality = "medium";
        }

        // Останавливаем существующую сессию для этой камеры
        stopCameraStream(sessionId);

        // Создаем новую сессию
        CameraStreamSession session = new CameraStreamSession(
                sessionId,
                request.cameraId,
                ctx,
                request.fps,
                request.quality);

        sessions.put(sessionId, session);

        // Отправляем подтверждение
        sendStreamStarted(ctx, request.cameraId, request.fps, request.quality);
    }

    /**
     * Остановить стрим камеры
     */
    public static void stopCameraStream(String sessionId) {
        CameraStreamSession session = sessions.remove(sessionId);
        if (session != null) {
            session.stop();
        }
    }

    /**
     * Остановить стрим камеры по контексту
     */
    public static void stopCameraStream(WsContext ctx) {
        stopCameraStream(ctx.sessionId());
    }

    /**
     * Получить список активных сессий стрима
     */
    public static java.util.Map<String, String> getActiveStreams() {
        java.util.Map<String, String> activeStreams = new ConcurrentHashMap<>();
        sessions.forEach((sessionId, session) -> {
            activeStreams.put(sessionId, session.getCameraId());
        });
        return activeStreams;
    }

    /**
     * Получить количество активных стримов
     */
    public static int getActiveStreamCount() {
        return sessions.size();
    }

    /**
     * Получить количество активных стримов для конкретной камеры
     */
    public static int getActiveStreamCountForCamera(String cameraId) {
        return (int) sessions.values().stream()
                .filter(session -> session.getCameraId().equals(cameraId))
                .count();
    }

    /**
     * Отправить сообщение об ошибке
     */
    private static void sendError(WsContext ctx, String error) {
        try {
            CameraFrameData errorData = new CameraFrameData();
            errorData.type = "error";
            errorData.timestamp = System.currentTimeMillis();
            errorData.error = error;

            String json = objectMapper.writeValueAsString(errorData);
            ctx.send(json);
        } catch (Exception e) {
            Logger.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage());
        }
    }

    /**
     * Отправить подтверждение начала стрима
     */
    private static void sendStreamStarted(WsContext ctx, String cameraId, int fps, String quality) {
        try {
            CameraFrameData startData = new CameraFrameData();
            startData.type = "stream_started";
            startData.cameraId = cameraId;
            startData.timestamp = System.currentTimeMillis();
            startData.fps = fps;

            String json = objectMapper.writeValueAsString(startData);
            ctx.send(json);

            Logger.info("Стрим камеры {} запущен с FPS: {} и качеством: {}", cameraId, fps, quality);
        } catch (Exception e) {
            Logger.error("Ошибка отправки подтверждения стрима: {}", e.getMessage());
        }
    }

    /**
     * Очистить все сессии
     */
    public static void cleanup() {
        sessions.values().forEach(CameraStreamSession::stop);
        sessions.clear();
        Logger.info("CameraStreamService очищен");
    }
}