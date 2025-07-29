package org.openpnp.api;

import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.openpnp.api.controllers.HealthController;
import org.openpnp.api.controllers.MachineController;
import org.openpnp.api.controllers.JobController;
import org.openpnp.api.controllers.DiagnosticsController;
import org.openpnp.api.controllers.WebSocketController;
import org.openpnp.api.controllers.CameraStreamController;
import org.openpnp.api.services.WebSocketService;
import org.openpnp.api.services.CameraStreamService;
import org.pmw.tinylog.Logger;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Сервер OpenPnP API
 */
public class OpenPnPApiServer {

    private Javalin app;

    public void start(int port) {
        try {
            app = Javalin.create(config -> {
                // Настраиваем OpenAPI плагин
                config.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                    pluginConfig.withDefinitionConfiguration((version, definition) -> {
                        definition.withOpenApiInfo(info -> {
                            info.setTitle("OpenPnP API");
                            info.setDescription("REST API для управления OpenPnP машиной");
                            info.setVersion("1.0.0");
                        });
                    });
                }));

                // Добавляем Swagger UI
                config.registerPlugin(new SwaggerPlugin());

                // Добавляем ReDoc
                config.registerPlugin(new ReDocPlugin());

                // Настраиваем маршруты
                config.router.apiBuilder(() -> {
                    // Корневая страница
                    get("/", HealthController::getRoot);

                    // API эндпоинты
                    path("/api", () -> {
                        get("/", HealthController::getApiInfo);
                        get("/health", HealthController::getHealth);
                        get("/ping", HealthController::ping);

                        // Управление машиной
                        path("/machine", () -> {
                            get("/status", MachineController::getStatus);
                            post("/enable", MachineController::enable);
                            post("/disable", MachineController::disable);
                            post("/home", MachineController::home);
                            post("/emergency-stop", MachineController::emergencyStop);
                        });

                        // Управление заданиями
                        path("/job", () -> {
                            get("/info", JobController::getJobInfo);
                            get("/status", JobController::getJobStatus);
                            post("/load", JobController::loadJob);
                            post("/start", JobController::startJob);
                            post("/pause", JobController::pauseJob);
                            post("/stop", JobController::stopJob);
                        });

                        // Диагностика
                        path("/diagnostics", () -> {
                            get("/configuration", DiagnosticsController::checkConfiguration);
                            get("/machine-detailed", DiagnosticsController::checkMachineDetailed);
                        });

                        // Управление WebSocket
                        path("/websocket", () -> {
                            get("/info", WebSocketController::getWebSocketInfo);
                        });

                        // Управление камерами
                        path("/cameras", () -> {
                            get("/", CameraStreamController::getCameras);
                            get("/streams", CameraStreamController::getStreamInfo);
                        });
                    });

                    // WebSocket эндпоинт для обновлений статуса машины
                    ws("/ws/machine-status", ws -> {
                        ws.onConnect(WebSocketController::onConnect);
                        ws.onClose(WebSocketController::onClose);
                        ws.onMessage(WebSocketController::onMessage);
                    });

                    // WebSocket эндпоинт для стримов камер
                    ws("/ws/camera-stream", ws -> {
                        ws.onConnect(CameraStreamController::onConnect);
                        ws.onClose(CameraStreamController::onClose);
                        ws.onMessage(CameraStreamController::onMessage);
                    });
                });

                // Настраиваем CORS для разработки
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.anyHost();
                    });
                });

            }).start(port);

            Logger.info("OpenPnP API сервер запущен на порту " + port);
            Logger.info("Swagger UI доступен по адресу: http://localhost:" + port + "/swagger");
            Logger.info("ReDoc доступен по адресу: http://localhost:" + port + "/redoc");
            Logger.info("API доступен по адресу: http://localhost:" + port + "/api");
            Logger.info("WebSocket статуса машины доступен по адресу: ws://localhost:" + port + "/ws/machine-status");
            Logger.info("WebSocket стримов камер доступен по адресу: ws://localhost:" + port + "/ws/camera-stream");

            // Запускаем WebSocket сервисы
            WebSocketService.startPeriodicUpdates();

        } catch (Exception e) {
            Logger.error("Ошибка запуска API сервера", e);
            throw new RuntimeException("Не удалось запустить API сервер", e);
        }
    }

    public void stop() {
        if (app != null) {
            // Останавливаем WebSocket сервисы
            WebSocketService.cleanup();
            CameraStreamService.cleanup();

            app.stop();
            Logger.info("OpenPnP API сервер остановлен");
        }
    }
}