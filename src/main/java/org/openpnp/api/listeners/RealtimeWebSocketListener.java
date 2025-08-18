package org.openpnp.api.listeners;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openpnp.api.services.WebSocketService;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.pmw.tinylog.Logger;

/**
 * Слушатель для реалтаймовых WebSocket обновлений
 * Отслеживает изменения координат осей и других свойств машины
 */
public class RealtimeWebSocketListener implements MachineListener, PropertyChangeListener {

    private final ConcurrentHashMap<String, Object> lastValues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, Runnable> pendingUpdates = new ConcurrentHashMap<>();

    private Machine machine;
    private boolean isInitialized = false;

    /**
     * Инициализировать слушатель для машины
     */
    public void initialize(Machine machine) {
        if (isInitialized) {
            cleanup();
        }

        this.machine = machine;

        // Добавляем слушатель событий машины
        machine.addListener(this);

        // Добавляем слушатели изменений свойств для осей
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                addAxisListener((CoordinateAxis) axis);
            }
        }

        // Добавляем слушатель изменений списка осей
        if (machine instanceof AbstractModelObject) {
            ((AbstractModelObject) machine).addPropertyChangeListener("axes", this);
        }

        isInitialized = true;
        Logger.info("RealtimeWebSocketListener инициализирован для машины");
    }

    /**
     * Добавить слушатель для оси
     */
    private void addAxisListener(CoordinateAxis axis) {
        String axisId = axis.getId();

        // Сохраняем начальные значения
        lastValues.put(axisId + "_coordinate", axis.getCoordinate());

        if (axis instanceof ControllerAxis) {
            ControllerAxis controllerAxis = (ControllerAxis) axis;
            lastValues.put(axisId + "_driverCoordinate", controllerAxis.getDriverCoordinate());
        }

        // Добавляем слушатель изменений свойств
        if (axis instanceof AbstractModelObject) {
            ((AbstractModelObject) axis).addPropertyChangeListener(this);
        }
    }

    /**
     * Удалить слушатель для оси
     */
    private void removeAxisListener(CoordinateAxis axis) {
        String axisId = axis.getId();
        lastValues.remove(axisId + "_coordinate");
        lastValues.remove(axisId + "_driverCoordinate");

        if (axis instanceof AbstractModelObject) {
            ((AbstractModelObject) axis).removePropertyChangeListener(this);
        }
    }

    /**
     * Проверить изменения и отправить обновление если нужно
     */
    private void checkAndSendUpdate(String key, Object newValue) {
        Object oldValue = lastValues.get(key);

        if (oldValue == null || !oldValue.equals(newValue)) {
            lastValues.put(key, newValue);
            scheduleUpdate("property_change_" + key);
        }
    }

    /**
     * Запланировать обновление с дебаунсингом
     */
    private void scheduleUpdate(String updateKey) {
        // Отменяем предыдущее обновление для этого ключа
        Runnable existingUpdate = pendingUpdates.remove(updateKey);
        if (existingUpdate != null) {
            // К сожалению, ScheduledExecutorService не позволяет отменить конкретную задачу
            // Поэтому мы просто игнорируем предыдущие обновления
        }

        // Создаем новое обновление
        Runnable update = () -> {
            try {
                WebSocketService.broadcastStatusUpdate();
                pendingUpdates.remove(updateKey);
            } catch (Exception e) {
                Logger.error("Ошибка отправки реалтаймового обновления: " + e.getMessage());
            }
        };

        pendingUpdates.put(updateKey, update);

        // Запускаем обновление с небольшой задержкой для дебаунсинга
        debounceExecutor.schedule(update, 50, TimeUnit.MILLISECONDS);
    }

    /**
     * Очистить ресурсы
     */
    public void cleanup() {
        if (machine != null) {
            machine.removeListener(this);

            // Удаляем слушатели осей
            for (Axis axis : machine.getAxes()) {
                if (axis instanceof CoordinateAxis) {
                    removeAxisListener((CoordinateAxis) axis);
                }
            }

            // Удаляем слушатель изменений списка осей
            if (machine instanceof AbstractModelObject) {
                ((AbstractModelObject) machine).removePropertyChangeListener("axes", this);
            }
        }

        lastValues.clear();
        pendingUpdates.clear();
        debounceExecutor.shutdown();

        try {
            if (!debounceExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                debounceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            debounceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        isInitialized = false;
        Logger.info("RealtimeWebSocketListener очищен");
    }

    // ========== MachineListener методы ==========

    @Override
    public void machineHeadActivity(Machine machine, org.openpnp.spi.Head head) {
        // Проверяем изменения координат всех осей головки
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                CoordinateAxis coordAxis = (CoordinateAxis) axis;
                checkAndSendUpdate(axis.getId() + "_coordinate", coordAxis.getCoordinate());

                if (axis instanceof ControllerAxis) {
                    ControllerAxis controllerAxis = (ControllerAxis) axis;
                    checkAndSendUpdate(axis.getId() + "_driverCoordinate", controllerAxis.getDriverCoordinate());
                }
            }
        }
    }

    @Override
    public void machineTargetedUserAction(Machine machine, org.openpnp.spi.HeadMountable hm, boolean jogging) {
        // При пользовательских действиях проверяем все оси
        machineHeadActivity(machine, hm.getHead());
    }

    @Override
    public void machineActuatorActivity(Machine machine, org.openpnp.spi.Actuator actuator) {
        // При активности актуатора отправляем обновление
        scheduleUpdate("actuator_activity");
    }

    @Override
    public void machineEnabled(Machine machine) {
        Logger.info("Машина включена - отправка реалтаймового WebSocket обновления");
        scheduleUpdate("machine_enabled");
    }

    @Override
    public void machineEnableFailed(Machine machine, String reason) {
        Logger.warn("Ошибка включения машины: " + reason + " - отправка реалтаймового WebSocket обновления");
        scheduleUpdate("machine_enable_failed");
    }

    @Override
    public void machineAboutToBeDisabled(Machine machine, String reason) {
        Logger.info("Машина будет выключена: " + reason + " - отправка реалтаймового WebSocket обновления");
        scheduleUpdate("machine_about_to_be_disabled");
    }

    @Override
    public void machineDisabled(Machine machine, String reason) {
        Logger.info("Машина выключена: " + reason + " - отправка реалтаймового WebSocket обновления");
        scheduleUpdate("machine_disabled");
    }

    @Override
    public void machineDisableFailed(Machine machine, String reason) {
        Logger.warn("Ошибка выключения машины: " + reason + " - отправка реалтаймового WebSocket обновления");
        scheduleUpdate("machine_disable_failed");
    }

    @Override
    public void machineHomed(Machine machine, boolean isHomed) {
        Logger.info("Машина " + (isHomed ? "выполнила хоминг" : "не выполнена хоминг")
                + " - отправка реалтаймового WebSocket обновления");
        scheduleUpdate("machine_homed");
    }

    @Override
    public void machineBusy(Machine machine, boolean busy) {
        // При изменении состояния занятости отправляем обновление
        scheduleUpdate("machine_busy");
    }

    // ========== PropertyChangeListener методы ==========

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        Object source = evt.getSource();

        if (source instanceof CoordinateAxis) {
            CoordinateAxis axis = (CoordinateAxis) source;
            String axisId = axis.getId();

            switch (propertyName) {
                case "coordinate":
                    checkAndSendUpdate(axisId + "_coordinate", evt.getNewValue());
                    break;
                case "driverCoordinate":
                    checkAndSendUpdate(axisId + "_driverCoordinate", evt.getNewValue());
                    break;
            }
        } else if ("axes".equals(propertyName) && source instanceof AbstractModelObject) {
            // При изменении списка осей переинициализируем слушатели
            AbstractModelObject abstractMachine = (AbstractModelObject) source;

            // Удаляем старые слушатели
            for (Axis axis : machine.getAxes()) {
                if (axis instanceof CoordinateAxis) {
                    removeAxisListener((CoordinateAxis) axis);
                }
            }

            // Добавляем новые слушатели
            for (Axis axis : machine.getAxes()) {
                if (axis instanceof CoordinateAxis) {
                    addAxisListener((CoordinateAxis) axis);
                }
            }

            scheduleUpdate("axes_changed");
        }
    }
}