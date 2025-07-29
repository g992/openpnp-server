package org.openpnp.api.controllers;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.openpnp.Main;
import org.openpnp.api.models.ApiResponse;
import org.openpnp.api.models.PingResponse;
import org.openpnp.model.Configuration;

/**
 * Контроллер для проверки состояния системы
 */
public class HealthController {

    private static final long startTime = System.currentTimeMillis();

    /**
     * Ping endpoint для проверки доступности API
     */
    @OpenApi(path = "/api/ping", methods = {
            HttpMethod.GET }, summary = "Проверка доступности API", description = "Возвращает информацию о состоянии системы OpenPnP", tags = {
                    "Health" }, responses = {
                            @OpenApiResponse(status = "200", description = "Успешный ответ", content = @OpenApiContent(from = ApiResponse.class))
                    })
    public static void ping(Context ctx) {
        try {
            // Получаем версию OpenPnP
            String version = Main.getVersion();

            // Вычисляем время работы
            long uptimeMs = System.currentTimeMillis() - startTime;

            // Проверяем статус машины (безопасно)
            Boolean machineEnabled = null;
            try {
                if (Configuration.get() != null && Configuration.get().getMachine() != null) {
                    machineEnabled = Configuration.get().getMachine().isEnabled();
                }
            } catch (Exception e) {
                // Игнорируем ошибки, если конфигурация не загружена
            }

            // Создаем ответ
            PingResponse pingData = new PingResponse(
                    "OK",
                    version,
                    uptimeMs,
                    machineEnabled);

            ApiResponse response = ApiResponse.success("OpenPnP API доступно", pingData);

            ctx.json(response);

        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @OpenApi(path = "/api/health", methods = {
            HttpMethod.GET }, summary = "Детальная проверка здоровья системы", description = "Возвращает подробную информацию о состоянии всех компонентов системы", tags = {
                    "Health" }, responses = {
                            @OpenApiResponse(status = "200", description = "Система работает нормально"),
                            @OpenApiResponse(status = "503", description = "Система недоступна")
                    })
    public static void health(Context ctx) {
        try {
            boolean isHealthy = true;
            StringBuilder healthInfo = new StringBuilder();

            // Проверяем конфигурацию
            if (Configuration.get() == null) {
                isHealthy = false;
                healthInfo.append("Конфигурация не загружена. ");
            } else {
                healthInfo.append("Конфигурация: OK. ");
            }

            // Проверяем машину
            try {
                if (Configuration.get() != null && Configuration.get().getMachine() != null) {
                    healthInfo.append("Машина: инициализирована. ");
                } else {
                    isHealthy = false;
                    healthInfo.append("Машина: не инициализирована. ");
                }
            } catch (Exception e) {
                isHealthy = false;
                healthInfo.append("Машина: ошибка - ").append(e.getMessage()).append(". ");
            }

            if (isHealthy) {
                ctx.json(ApiResponse.success("Система здорова", healthInfo.toString()));
            } else {
                ctx.status(503).json(ApiResponse.error("Система нездорова: " + healthInfo.toString()));
            }

        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Ошибка проверки здоровья: " + e.getMessage()));
        }
    }
}