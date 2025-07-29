package org.openpnp.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import io.javalin.http.Context;
import org.openpnp.api.services.CameraStreamService;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер для обработки WebSocket стримов камер
 */
public class CameraStreamController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Обработчик подключения WebSocket стрима камеры
     */
    public static void onConnect(WsContext ctx) {
        Logger.info("WebSocket стрим камеры подключен: " + ctx.sessionId());

        // Отправляем список доступных камер
        sendAvailableCameras(ctx);
    }

    /**
     * Обработчик отключения WebSocket стрима камеры
     */
    public static void onClose(WsContext ctx) {
        // Останавливаем стрим при отключении
        CameraStreamService.stopCameraStream(ctx);
        Logger.info("WebSocket стрим камеры отключен: " + ctx.sessionId());
    }

    /**
     * Обработчик сообщений WebSocket стрима камеры
     */
    public static void onMessage(WsMessageContext ctx) {
        try {
            String message = ctx.message();
            Logger.debug("Получено WebSocket сообщение стрима камеры: " + message);

            // Парсим JSON сообщение
            try {
                var jsonNode = objectMapper.readTree(message);
                String command = jsonNode.get("command").asText();

                switch (command) {
                    case "start_stream":
                        handleStartStream(ctx, jsonNode);
                        break;
                    case "stop_stream":
                        handleStopStream(ctx);
                        break;
                    case "ping":
                        handlePing(ctx);
                        break;
                    case "get_cameras":
                        sendAvailableCameras(ctx);
                        break;
                    case "get_stream_info":
                        sendStreamInfo(ctx);
                        break;
                    default:
                        sendError(ctx, "Неизвестная команда: " + command);
                        break;
                }
            } catch (Exception e) {
                // Если не JSON, пробуем простые команды
                if ("ping".equals(message)) {
                    handlePing(ctx);
                } else if ("stop_stream".equals(message)) {
                    handleStopStream(ctx);
                } else {
                    sendError(ctx, "Неверный формат сообщения: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Logger.error("Ошибка обработки WebSocket сообщения стрима камеры: " + e.getMessage());
            sendError(ctx, "Ошибка обработки сообщения: " + e.getMessage());
        }
    }

    /**
     * Обработка команды начала стрима
     */
    private static void handleStartStream(WsMessageContext ctx, com.fasterxml.jackson.databind.JsonNode jsonNode) {
        try {
            CameraStreamService.StreamRequest request = new CameraStreamService.StreamRequest();

            // Получаем параметры из JSON
            if (jsonNode.has("cameraId")) {
                request.cameraId = jsonNode.get("cameraId").asText();
            }

            if (jsonNode.has("fps")) {
                request.fps = jsonNode.get("fps").asInt();
            } else {
                request.fps = 10; // FPS по умолчанию
            }

            if (jsonNode.has("quality")) {
                request.quality = jsonNode.get("quality").asText();
            } else {
                request.quality = "medium"; // Качество по умолчанию
            }

            // Запускаем стрим
            CameraStreamService.startCameraStream(ctx, request);

        } catch (Exception e) {
            Logger.error("Ошибка запуска стрима: " + e.getMessage());
            sendError(ctx, "Ошибка запуска стрима: " + e.getMessage());
        }
    }

    /**
     * Обработка команды остановки стрима
     */
    private static void handleStopStream(WsMessageContext ctx) {
        try {
            CameraStreamService.stopCameraStream(ctx);

            // Отправляем подтверждение остановки
            var response = objectMapper.createObjectNode();
            response.put("type", "stream_stopped");
            response.put("timestamp", System.currentTimeMillis());

            ctx.send(objectMapper.writeValueAsString(response));
            Logger.info("Стрим камеры остановлен для сессии: " + ctx.sessionId());

        } catch (Exception e) {
            Logger.error("Ошибка остановки стрима: " + e.getMessage());
            sendError(ctx, "Ошибка остановки стрима: " + e.getMessage());
        }
    }

    /**
     * Обработка команды ping
     */
    private static void handlePing(WsMessageContext ctx) {
        try {
            var response = objectMapper.createObjectNode();
            response.put("type", "pong");
            response.put("timestamp", System.currentTimeMillis());

            ctx.send(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            Logger.error("Ошибка отправки pong: " + e.getMessage());
        }
    }

    /**
     * Отправка списка доступных камер
     */
    private static void sendAvailableCameras(WsContext ctx) {
        try {
            Machine machine = Configuration.get().getMachine();
            List<CameraInfo> cameras = new ArrayList<>();

            // Получаем камеры машины
            for (Camera camera : machine.getCameras()) {
                CameraInfo info = new CameraInfo();
                info.id = camera.getId();
                info.name = camera.getName();
                info.type = camera.getClass().getSimpleName();
                info.width = camera.getWidth();
                info.height = camera.getHeight();
                info.location = "machine";
                cameras.add(info);
            }

            // Получаем камеры головок
            for (var head : machine.getHeads()) {
                for (Camera camera : head.getCameras()) {
                    CameraInfo info = new CameraInfo();
                    info.id = camera.getId();
                    info.name = camera.getName();
                    info.type = camera.getClass().getSimpleName();
                    info.width = camera.getWidth();
                    info.height = camera.getHeight();
                    info.location = "head:" + head.getName();
                    cameras.add(info);
                }
            }

            var response = objectMapper.createObjectNode();
            response.put("type", "cameras_list");
            response.put("timestamp", System.currentTimeMillis());
            response.set("cameras", objectMapper.valueToTree(cameras));

            ctx.send(objectMapper.writeValueAsString(response));

        } catch (Exception e) {
            Logger.error("Ошибка получения списка камер: " + e.getMessage());
            sendError(ctx, "Ошибка получения списка камер: " + e.getMessage());
        }
    }

    /**
     * Отправка информации о стриме
     */
    private static void sendStreamInfo(WsContext ctx) {
        try {
            var response = objectMapper.createObjectNode();
            response.put("type", "stream_info");
            response.put("timestamp", System.currentTimeMillis());
            response.put("active_streams", CameraStreamService.getActiveStreamCount());
            response.set("streams", objectMapper.valueToTree(CameraStreamService.getActiveStreams()));

            ctx.send(objectMapper.writeValueAsString(response));

        } catch (Exception e) {
            Logger.error("Ошибка получения информации о стриме: " + e.getMessage());
            sendError(ctx, "Ошибка получения информации о стриме: " + e.getMessage());
        }
    }

    /**
     * Отправка сообщения об ошибке
     */
    private static void sendError(WsContext ctx, String error) {
        try {
            var errorResponse = objectMapper.createObjectNode();
            errorResponse.put("type", "error");
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("error", error);

            ctx.send(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            Logger.error("Ошибка отправки сообщения об ошибке: " + e.getMessage());
        }
    }

    /**
     * REST API: Получить список камер
     */
    public static void getCameras(Context ctx) {
        try {
            Machine machine = Configuration.get().getMachine();
            List<CameraInfo> cameras = new ArrayList<>();

            // Получаем камеры машины
            for (Camera camera : machine.getCameras()) {
                CameraInfo info = new CameraInfo();
                info.id = camera.getId();
                info.name = camera.getName();
                info.type = camera.getClass().getSimpleName();
                info.width = camera.getWidth();
                info.height = camera.getHeight();
                info.location = "machine";
                cameras.add(info);
            }

            // Получаем камеры головок
            for (var head : machine.getHeads()) {
                for (Camera camera : head.getCameras()) {
                    CameraInfo info = new CameraInfo();
                    info.id = camera.getId();
                    info.name = camera.getName();
                    info.type = camera.getClass().getSimpleName();
                    info.width = camera.getWidth();
                    info.height = camera.getHeight();
                    info.location = "head:" + head.getName();
                    cameras.add(info);
                }
            }

            ctx.json(cameras);

        } catch (Exception e) {
            Logger.error("Ошибка получения списка камер: " + e.getMessage());
            ctx.status(500).json(new ErrorResponse("Ошибка получения списка камер: " + e.getMessage()));
        }
    }

    /**
     * REST API: Получить информацию о стримах
     */
    public static void getStreamInfo(Context ctx) {
        try {
            var info = new StreamInfo();
            info.activeStreams = CameraStreamService.getActiveStreamCount();
            info.streams = CameraStreamService.getActiveStreams();

            ctx.json(info);

        } catch (Exception e) {
            Logger.error("Ошибка получения информации о стримах: " + e.getMessage());
            ctx.status(500).json(new ErrorResponse("Ошибка получения информации о стримах: " + e.getMessage()));
        }
    }

    /**
     * Информация о камере
     */
    public static class CameraInfo {
        public String id;
        public String name;
        public String type;
        public int width;
        public int height;
        public String location;
    }

    /**
     * Информация о стримах
     */
    public static class StreamInfo {
        public int activeStreams;
        public java.util.Map<String, String> streams;
    }

    /**
     * Ответ об ошибке
     */
    public static class ErrorResponse {
        public final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}