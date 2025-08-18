package org.openpnp.api.exceptions;

/**
 * Исключение выбрасывается когда попытка выполнить операцию с незагруженным
 * заданием
 */
public class JobNotLoadedException extends ApiException {
    public JobNotLoadedException() {
        super(400, "JOB_NOT_LOADED", "Задание не загружено. Загрузите задание перед выполнением операций.");
    }

    public JobNotLoadedException(String message) {
        super(400, "JOB_NOT_LOADED", message);
    }
}