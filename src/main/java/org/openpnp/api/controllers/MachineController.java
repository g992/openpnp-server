package org.openpnp.api.controllers;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.openpnp.api.exceptions.MachineNotEnabledException;
import org.openpnp.api.models.ApiResponse;
import org.openpnp.api.models.machine.MachineStatus;
import org.openpnp.api.services.MachineService;

/**
 * Контроллер для управления машиной OpenPnP
 */
public class MachineController {

    private static final MachineService machineService = new MachineService();

    @OpenApi(path = "/api/machine/status", methods = HttpMethod.GET, summary = "Получить статус машины", description = "Возвращает текущий статус машины и её компонентов", tags = {
            "Machine" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = MachineStatus.class))
            })
    public static void getStatus(Context ctx) {
        try {
            MachineStatus status = machineService.getMachineStatus();
            ApiResponse<MachineStatus> response = ApiResponse.success("Статус машины получен", status);
            ctx.json(response);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при получении статуса машины: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/machine/enable", methods = HttpMethod.POST, summary = "Включить машину", description = "Включает машину для работы", tags = {
            "Machine" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void enable(Context ctx) {
        try {
            machineService.enableMachine();
            ApiResponse<Void> response = ApiResponse.success("Машина успешно включена");
            ctx.json(response);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при включении машины: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/machine/disable", methods = HttpMethod.POST, summary = "Выключить машину", description = "Выключает машину и останавливает все операции", tags = {
            "Machine" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void disable(Context ctx) {
        try {
            machineService.disableMachine();
            ApiResponse<Void> response = ApiResponse.success("Машина успешно выключена");
            ctx.json(response);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при выключении машины: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/machine/home", methods = HttpMethod.POST, summary = "Выполнить хоминг", description = "Выполняет процедуру хоминга для всех осей машины", tags = {
            "Machine" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void home(Context ctx) {
        try {
            machineService.homeMachine();
            ApiResponse<Void> response = ApiResponse.success("Хоминг выполнен успешно");
            ctx.json(response);
        } catch (MachineNotEnabledException e) {
            ApiResponse<Void> response = ApiResponse.error(e.getMessage());
            ctx.json(response).status(e.getStatusCode());
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при выполнении хоминга: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/machine/emergency-stop", methods = HttpMethod.POST, summary = "Аварийная остановка", description = "Выполняет аварийную остановку всех операций машины", tags = {
            "Machine" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void emergencyStop(Context ctx) {
        try {
            machineService.emergencyStop();
            ApiResponse<Void> response = ApiResponse.success("Аварийная остановка выполнена");
            ctx.json(response);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при аварийной остановке: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }
}