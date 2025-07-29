package org.openpnp.api;

import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.openpnp.api.controllers.HealthController;
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

        } catch (Exception e) {
            Logger.error("Ошибка запуска API сервера", e);
            throw new RuntimeException("Не удалось запустить API сервер", e);
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
            Logger.info("OpenPnP API сервер остановлен");
        }
    }
}