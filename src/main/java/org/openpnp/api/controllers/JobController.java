package org.openpnp.api.controllers;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.openpnp.api.exceptions.JobNotLoadedException;
import org.openpnp.api.exceptions.MachineNotEnabledException;
import org.openpnp.api.models.ApiResponse;
import org.openpnp.api.models.job.JobInfo;
import org.openpnp.api.models.job.JobStatus;
import org.openpnp.api.services.JobService;

/**
 * Контроллер для управления заданиями OpenPnP
 */
public class JobController {

    private static final JobService jobService = new JobService();

    @OpenApi(path = "/api/job/info", methods = HttpMethod.GET, summary = "Получить информацию о задании", description = "Возвращает информацию о текущем загруженном задании", tags = {
            "Job" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = JobInfo.class)),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void getJobInfo(Context ctx) {
        try {
            JobInfo jobInfo = jobService.getCurrentJobInfo();
            ApiResponse<JobInfo> response = ApiResponse.success("Информация о задании получена", jobInfo);
            ctx.json(response);
        } catch (JobNotLoadedException e) {
            ApiResponse<Void> response = ApiResponse.error(e.getMessage());
            ctx.json(response).status(e.getStatusCode());
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse
                    .error("Ошибка при получении информации о задании: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/job/status", methods = HttpMethod.GET, summary = "Получить статус задания", description = "Возвращает текущий статус выполнения задания", tags = {
            "Job" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = JobStatus.class)),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void getJobStatus(Context ctx) {
        try {
            JobStatus jobStatus = jobService.getJobStatus();
            ApiResponse<JobStatus> response = ApiResponse.success("Статус задания получен", jobStatus);
            ctx.json(response);
        } catch (JobNotLoadedException e) {
            ApiResponse<Void> response = ApiResponse.error(e.getMessage());
            ctx.json(response).status(e.getStatusCode());
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при получении статуса задания: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/job/load", methods = HttpMethod.POST, summary = "Загрузить задание", description = "Загружает задание из указанного файла", tags = {
            "Job" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void loadJob(Context ctx) {
        try {
            String filePath = ctx.formParam("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                ApiResponse<Void> response = ApiResponse.error("Не указан путь к файлу задания");
                ctx.json(response).status(400);
                return;
            }

            jobService.loadJob(filePath);
            ApiResponse<Void> response = ApiResponse.success("Задание успешно загружено");
            ctx.json(response);
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при загрузке задания: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/job/start", methods = HttpMethod.POST, summary = "Запустить задание", description = "Запускает выполнение загруженного задания", tags = {
            "Job" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void startJob(Context ctx) {
        try {
            jobService.startJob();
            ApiResponse<Void> response = ApiResponse.success("Задание запущено");
            ctx.json(response);
        } catch (JobNotLoadedException | MachineNotEnabledException e) {
            ApiResponse<Void> response = ApiResponse.error(e.getMessage());
            ctx.json(response).status(e.getStatusCode());
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при запуске задания: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/job/pause", methods = HttpMethod.POST, summary = "Приостановить задание", description = "Приостанавливает выполнение задания", tags = {
            "Job" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void pauseJob(Context ctx) {
        try {
            jobService.pauseJob();
            ApiResponse<Void> response = ApiResponse.success("Задание приостановлено");
            ctx.json(response);
        } catch (JobNotLoadedException e) {
            ApiResponse<Void> response = ApiResponse.error(e.getMessage());
            ctx.json(response).status(e.getStatusCode());
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при приостановке задания: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }

    @OpenApi(path = "/api/job/stop", methods = HttpMethod.POST, summary = "Остановить задание", description = "Останавливает выполнение задания", tags = {
            "Job" }, responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = ApiResponse.class)),
                    @OpenApiResponse(status = "500", content = @OpenApiContent(from = ApiResponse.class))
            })
    public static void stopJob(Context ctx) {
        try {
            jobService.stopJob();
            ApiResponse<Void> response = ApiResponse.success("Задание остановлено");
            ctx.json(response);
        } catch (JobNotLoadedException e) {
            ApiResponse<Void> response = ApiResponse.error(e.getMessage());
            ctx.json(response).status(e.getStatusCode());
        } catch (Exception e) {
            ApiResponse<Void> response = ApiResponse.error("Ошибка при остановке задания: " + e.getMessage());
            ctx.json(response).status(500);
        }
    }
}