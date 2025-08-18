package org.openpnp.api.exceptions;

/**
 * Исключение выбрасывается когда попытка выполнить операцию с неактивной
 * машиной
 */
public class MachineNotEnabledException extends ApiException {
    public MachineNotEnabledException() {
        super(400, "MACHINE_NOT_ENABLED", "Машина не включена. Включите машину перед выполнением операций.");
    }

    public MachineNotEnabledException(String message) {
        super(400, "MACHINE_NOT_ENABLED", message);
    }
}