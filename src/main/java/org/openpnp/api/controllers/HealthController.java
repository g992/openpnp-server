package org.openpnp.api.controllers;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.openpnp.Main;
import org.openpnp.api.models.ApiInfo;
import org.openpnp.api.models.ApiResponse;
import org.openpnp.api.models.SystemInfo;
import org.openpnp.model.Configuration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для проверки состояния системы и общей информации API
 */
public class HealthController {

    private static final long startTime = System.currentTimeMillis();

    @OpenApi(path = "/api/health", methods = HttpMethod.GET, summary = "Проверка состояния системы", description = "Возвращает информацию о состоянии API сервера", tags = {
            "Health" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void getHealth(Context ctx) {
        try {
            ApiResponse<Void> response = ApiResponse.success("OpenPnP API работает корректно");
            ctx.json(response);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при проверке состояния: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/ping", methods = HttpMethod.GET, summary = "Пинг сервера", description = "Возвращает информацию о системе и времени работы", tags = {
            "Health" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void ping(Context ctx) {
        try {
            String version = Main.getVersion();
            long uptimeMs = System.currentTimeMillis() - startTime;
            boolean machineEnabled = isMachineEnabled();
            String timestamp = Instant.now().toString();

            SystemInfo systemInfo = new SystemInfo(version, uptimeMs, machineEnabled, timestamp);
            ApiResponse<SystemInfo> response = ApiResponse.success("Pong", systemInfo);

            ctx.json(response);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при выполнении ping: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api", methods = HttpMethod.GET, summary = "Информация об API", description = "Возвращает общую информацию об API и доступных эндпоинтах", tags = {
            "Info" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiInfo.class))
            })
    public static void getApiInfo(Context ctx) {
        try {
            Map<String, String> endpoints = new HashMap<>();
            endpoints.put("/api/health", "Проверка состояния системы");
            endpoints.put("/api/ping", "Пинг сервера с информацией о системе");
            endpoints.put("/api", "Информация об API");
            endpoints.put("/swagger", "Swagger UI документация");
            endpoints.put("/redoc", "ReDoc документация");
            endpoints.put("/api/machine/*", "Управление машиной (статус, включение, хоминг)");
            endpoints.put("/api/job/*", "Управление заданиями (загрузка, запуск, мониторинг)");
            endpoints.put("/api/diagnostics/*", "Диагностика конфигурации и проблем");

            ApiInfo apiInfo = new ApiInfo(
                    "OpenPnP API",
                    "REST API для управления OpenPnP машиной",
                    Main.getVersion(),
                    endpoints);

            ctx.json(apiInfo);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при получении информации об API: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/", methods = HttpMethod.GET, summary = "Корневая страница", description = "Возвращает информационную страницу API", tags = {
            "Info" }, responses = {
                    @OpenApiResponse(status = "200")
            })
    public static void getRoot(Context ctx) {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<title>OpenPnP API</title>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }" +
                ".container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
                +
                "h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }" +
                ".endpoint { background: #ecf0f1; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #3498db; }"
                +
                ".endpoint code { background: #34495e; color: white; padding: 2px 6px; border-radius: 3px; }" +
                "a { color: #3498db; text-decoration: none; }" +
                "a:hover { text-decoration: underline; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<h1>🤖 OpenPnP API Server</h1>" +
                "<p>Добро пожаловать в REST API сервер OpenPnP!</p>" +

                "<h2>📖 Документация</h2>" +
                "<p>" +
                "<a href=\"/swagger\" target=\"_blank\">🔗 Swagger UI</a> | " +
                "<a href=\"/redoc\" target=\"_blank\">🔗 ReDoc</a>" +
                "</p>" +

                "<h2>🛠 Доступные эндпоинты</h2>" +

                "<div class=\"endpoint\">" +
                "<strong><code>GET /api/health</code></strong><br>" +
                "Проверка состояния системы" +
                "</div>" +

                "<div class=\"endpoint\">" +
                "<strong><code>GET /api/ping</code></strong><br>" +
                "Информация о системе и времени работы" +
                "</div>" +

                "<div class=\"endpoint\">" +
                "<strong><code>GET /api</code></strong><br>" +
                "Общая информация об API" +
                "</div>" +

                "<div class=\"endpoint\">" +
                "<strong><code>GET /api/machine/status</code></strong><br>" +
                "Статус машины и её компонентов" +
                "</div>" +

                "<div class=\"endpoint\">" +
                "<strong><code>POST /api/machine/enable</code></strong><br>" +
                "Включить машину" +
                "</div>" +

                "<div class=\"endpoint\">" +
                "<strong><code>GET /api/diagnostics/configuration</code></strong><br>" +
                "Диагностика конфигурации системы" +
                "</div>" +

                "<p><strong>🔧 При проблемах с включением машины, сначала проверьте:</strong><br>" +
                "1. <a href=\"/api/diagnostics/configuration\" target=\"_blank\">Диагностику конфигурации</a><br>" +
                "2. <a href=\"/api/machine/status\" target=\"_blank\">Статус машины</a></p>" +

                "<p><em>Версия: " + Main.getVersion() + "</em></p>" +
                "</div>" +
                "</body>" +
                "</html>";

        ctx.html(html);
    }

    /**
     * Проверяет, включена ли машина OpenPnP
     */
    private static boolean isMachineEnabled() {
        try {
            Configuration config = Configuration.get();
            return config != null && config.getMachine() != null && config.getMachine().isEnabled();
        } catch (Exception e) {
            // Если не удается определить состояние машины, считаем что отключена
            return false;
        }
    }
}