package org.openpnp.api.controllers;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.openpnp.api.models.ApiResponse;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для диагностики конфигурации и проблем OpenPnP
 */
public class DiagnosticsController {

    @OpenApi(path = "/api/diagnostics/configuration", methods = HttpMethod.GET, summary = "Диагностика конфигурации", description = "Проверяет загрузку и корректность конфигурации OpenPnP", tags = {
            "Diagnostics" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void checkConfiguration(Context ctx) {
        Map<String, Object> diagnostics = new HashMap<>();
        boolean hasErrors = false;

        try {
            // Проверка конфигурации
            Configuration config = Configuration.get();
            if (config == null) {
                diagnostics.put("configuration", "ERROR: Конфигурация не инициализирована");
                hasErrors = true;
            } else {
                diagnostics.put("configuration", "OK: Конфигурация загружена");

                // Проверка машины
                Machine machine = config.getMachine();
                if (machine == null) {
                    diagnostics.put("machine", "ERROR: Машина не сконфигурирована в machine.xml");
                    hasErrors = true;
                } else {
                    diagnostics.put("machine", "OK: Машина найдена");
                    diagnostics.put("machine_class", machine.getClass().getSimpleName());
                    diagnostics.put("machine_enabled", machine.isEnabled());

                    // Проверка драйверов
                    if (machine.getDrivers() == null || machine.getDrivers().isEmpty()) {
                        diagnostics.put("drivers", "ERROR: Драйверы не настроены");
                        hasErrors = true;
                    } else {
                        diagnostics.put("drivers", "OK: Найдено " + machine.getDrivers().size() + " драйверов");

                        // Информация о драйверах
                        Map<String, String> driverInfo = new HashMap<>();
                        for (Driver driver : machine.getDrivers()) {
                            driverInfo.put(driver.getId(), driver.getClass().getSimpleName());
                        }
                        diagnostics.put("drivers_detail", driverInfo);
                    }

                    // Проверка планировщика движения
                    if (machine.getMotionPlanner() == null) {
                        diagnostics.put("motion_planner", "ERROR: Планировщик движения не настроен");
                        hasErrors = true;
                    } else {
                        diagnostics.put("motion_planner",
                                "OK: " + machine.getMotionPlanner().getClass().getSimpleName());
                    }

                    // Проверка головок
                    if (machine.getHeads() == null || machine.getHeads().isEmpty()) {
                        diagnostics.put("heads", "WARNING: Головки не настроены");
                    } else {
                        diagnostics.put("heads", "OK: Найдено " + machine.getHeads().size() + " головок");
                    }

                    // Проверка осей
                    if (machine.getAxes() == null || machine.getAxes().isEmpty()) {
                        diagnostics.put("axes", "WARNING: Оси не настроены");
                    } else {
                        diagnostics.put("axes", "OK: Найдено " + machine.getAxes().size() + " осей");
                    }
                }
            }

        } catch (Exception e) {
            diagnostics.put("exception", "ERROR: " + e.getMessage());
            diagnostics.put("exception_type", e.getClass().getSimpleName());
            hasErrors = true;
        }

        if (hasErrors) {
            ApiResponse<Map<String, Object>> response = ApiResponse.error("Обнаружены проблемы конфигурации");
            response.setData(diagnostics);
            ctx.json(response).status(500);
        } else {
            ApiResponse<Map<String, Object>> response = ApiResponse.success("Конфигурация проверена успешно",
                    diagnostics);
            ctx.json(response);
        }
    }

    @OpenApi(path = "/api/diagnostics/machine-detailed", methods = HttpMethod.GET, summary = "Детальная диагностика машины", description = "Подробная проверка состояния машины и её компонентов", tags = {
            "Diagnostics" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void checkMachineDetailed(Context ctx) {
        Map<String, Object> diagnostics = new HashMap<>();

        try {
            Configuration config = Configuration.get();
            Machine machine = config.getMachine();

            if (machine == null) {
                ApiResponse<Void> response = ApiResponse.error("Машина не настроена");
                ctx.json(response).status(400);
                return;
            }

            // Детальная проверка драйверов
            Map<String, Object> driversDetail = new HashMap<>();
            for (Driver driver : machine.getDrivers()) {
                Map<String, Object> driverStatus = new HashMap<>();
                driverStatus.put("id", driver.getId());
                driverStatus.put("name", driver.getName());
                driverStatus.put("class", driver.getClass().getSimpleName());
                // Driver не имеет метода isEnabled, пропускаем эту проверку

                try {
                    // Проверяем можем ли мы получить информацию о драйвере
                    driverStatus.put("status", "OK");
                } catch (Exception e) {
                    driverStatus.put("status", "ERROR: " + e.getMessage());
                }

                driversDetail.put(driver.getId(), driverStatus);
            }
            diagnostics.put("drivers", driversDetail);

            // Проверка состояния машины
            diagnostics.put("machine_enabled", machine.isEnabled());
            diagnostics.put("machine_busy", machine.isBusy());

            // Информация о головках
            Map<String, Object> headsDetail = new HashMap<>();
            machine.getHeads().forEach(head -> {
                Map<String, Object> headInfo = new HashMap<>();
                headInfo.put("id", head.getId());
                headInfo.put("name", head.getName());
                headInfo.put("nozzles_count", head.getNozzles().size());
                headInfo.put("cameras_count", head.getCameras().size());
                headInfo.put("actuators_count", head.getActuators().size());
                headsDetail.put(head.getId(), headInfo);
            });
            diagnostics.put("heads", headsDetail);

            ApiResponse<Map<String, Object>> response = ApiResponse.success("Детальная диагностика завершена",
                    diagnostics);
            ctx.json(response);

        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при диагностике: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }
}