package org.openpnp.api;

import io.javalin.Javalin;
import org.openpnp.Main;
import org.openpnp.api.controllers.HealthController;
import org.pmw.tinylog.Logger;

/**
 * Главный веб-сервер API для OpenPnP
 */
public class OpenPnPApiServer {

    private Javalin app;
    private final int port;
    private boolean running = false;

    public OpenPnPApiServer(int port) {
        this.port = port;
    }

    /**
     * Инициализация сервера
     */
    private void initializeServer() {
        app = Javalin.create(config -> {
            // Базовая конфигурация
            config.http.defaultContentType = "application/json";

            // Логирование запросов
            config.plugins.enableDevLogging();
        });

        // Добавляем простые CORS заголовки
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });

        // Обработка OPTIONS запросов для CORS
        app.options("/*", ctx -> ctx.result("OK"));

        setupRoutes();
        setupErrorHandling();
    }

    /**
     * Настройка маршрутов
     */
    private void setupRoutes() {
        // Основные health endpoints
        app.get("/api/ping", HealthController::ping);
        app.get("/api/health", HealthController::health);

        // Корневой endpoint с информацией об API
        app.get("/api", ctx -> {
            ctx.json(new java.util.HashMap<String, Object>() {
                {
                    put("name", "OpenPnP API");
                    put("version", Main.getVersion());
                    put("description", "REST API для управления OpenPnP системой");
                    put("endpoints", new java.util.HashMap<String, String>() {
                        {
                            put("ping", "/api/ping");
                            put("health", "/api/health");
                        }
                    });
                }
            });
        });

        // Корневая страница
        app.get("/", ctx -> {
            ctx.html("<h1>OpenPnP API</h1><p>API запущен и готов к использованию!</p>" +
                    "<ul><li><a href='/api'>/api</a> - информация об API</li>" +
                    "<li><a href='/api/ping'>/api/ping</a> - проверка доступности</li>" +
                    "<li><a href='/api/health'>/api/health</a> - проверка здоровья</li></ul>");
        });

        Logger.info("API routes configured");
    }

    /**
     * Настройка обработки ошибок
     */
    private void setupErrorHandling() {
        // 404 - Not Found
        app.error(404, ctx -> {
            ctx.json(new java.util.HashMap<String, Object>() {
                {
                    put("success", false);
                    put("message", "Endpoint не найден: " + ctx.path());
                    put("timestamp", java.time.Instant.now().toString());
                }
            });
        });

        // 500 - Internal Server Error
        app.error(500, ctx -> {
            ctx.json(new java.util.HashMap<String, Object>() {
                {
                    put("success", false);
                    put("message", "Внутренняя ошибка сервера");
                    put("timestamp", java.time.Instant.now().toString());
                }
            });
        });

        // Общий обработчик исключений
        app.exception(Exception.class, (e, ctx) -> {
            Logger.error("API Exception: " + e.getMessage(), e);
            ctx.status(500).json(new java.util.HashMap<String, Object>() {
                {
                    put("success", false);
                    put("message", "Ошибка: " + e.getMessage());
                    put("timestamp", java.time.Instant.now().toString());
                }
            });
        });
    }

    /**
     * Запуск сервера
     */
    public void start() {
        if (running) {
            Logger.warn("API сервер уже запущен");
            return;
        }

        try {
            initializeServer();
            app.start(port);
            running = true;
            Logger.info("OpenPnP API сервер запущен на порту " + port);
            Logger.info("API доступен по адресу: http://localhost:" + port);
            Logger.info("Ping endpoint: http://localhost:" + port + "/api/ping");
        } catch (Exception e) {
            Logger.error("Ошибка запуска API сервера: " + e.getMessage(), e);
            throw new RuntimeException("Не удалось запустить API сервер", e);
        }
    }

    /**
     * Остановка сервера
     */
    public void stop() {
        if (!running) {
            Logger.warn("API сервер уже остановлен");
            return;
        }

        try {
            if (app != null) {
                app.stop();
                running = false;
                Logger.info("OpenPnP API сервер остановлен");
            }
        } catch (Exception e) {
            Logger.error("Ошибка остановки API сервера: " + e.getMessage(), e);
        }
    }

    /**
     * Проверка статуса сервера
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Получение порта сервера
     */
    public int getPort() {
        return port;
    }
}