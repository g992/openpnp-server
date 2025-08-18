package org.openpnp.api.services;

import org.openpnp.api.exceptions.MachineNotEnabledException;
import org.openpnp.api.models.machine.*;
import org.openpnp.model.Configuration;
import org.openpnp.spi.*;
import org.openpnp.spi.base.AbstractAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Сервис для работы с машиной OpenPnP
 */
public class MachineService {

    /**
     * Получить текущий статус машины
     */
    public MachineStatus getMachineStatus() throws Exception {
        try {
            Configuration config = Configuration.get();
            if (config == null) {
                throw new Exception("Конфигурация не инициализирована");
            }

            Machine machine = config.getMachine();
            if (machine == null) {
                throw new Exception("Машина не сконфигурирована в файле конфигурации. Проверьте machine.xml");
            }
        } catch (Exception e) {
            throw new Exception("Ошибка при получении статуса машины: " + e.getMessage(), e);
        }

        Configuration config = Configuration.get();
        Machine machine = config.getMachine();

        MachineStatus status = new MachineStatus();
        status.setEnabled(machine.isEnabled());
        status.setBusy(machine.isBusy());

        status.setHomed(machine.isHomed());

        // Получаем информацию об осях
        List<AxisInfo> axisInfoList = new ArrayList<>();
        for (Axis axis : machine.getAxes()) {
            AxisInfo axisInfo = new AxisInfo();
            axisInfo.setId(axis.getId());
            axisInfo.setName(axis.getName());
            axisInfo.setType(axis.getType().toString());

            // Проверяем если ось поддерживает координаты
            if (axis instanceof CoordinateAxis) {
                CoordinateAxis coordAxis = (CoordinateAxis) axis;
                axisInfo.setPosition(coordAxis.getCoordinate());
                axisInfo.setHomed(true); // TODO: Добавить правильную проверку хоминга

                // Проверяем если ось контроллера для получения единиц измерения
                if (axis instanceof ControllerAxis) {
                    ControllerAxis controllerAxis = (ControllerAxis) axis;
                    axisInfo.setUnit(controllerAxis.getUnits().toString());
                } else {
                    axisInfo.setUnit("mm");
                }
            } else {
                // Для других типов осей устанавливаем значения по умолчанию
                axisInfo.setPosition(0.0);
                axisInfo.setUnit("mm");
                axisInfo.setHomed(false);
            }

            axisInfoList.add(axisInfo);
        }
        status.setAxes(axisInfoList);

        // Получаем информацию о головках
        List<HeadInfo> headInfoList = new ArrayList<>();
        for (Head head : machine.getHeads()) {
            HeadInfo headInfo = new HeadInfo();
            headInfo.setId(head.getId());
            headInfo.setName(head.getName());

            // Получаем списки ID компонентов головки
            List<String> nozzleIds = new ArrayList<>();
            for (Nozzle nozzle : head.getNozzles()) {
                nozzleIds.add(nozzle.getId());
            }
            headInfo.setNozzleIds(nozzleIds);

            List<String> cameraIds = new ArrayList<>();
            for (Camera camera : head.getCameras()) {
                cameraIds.add(camera.getId());
            }
            headInfo.setCameraIds(cameraIds);

            List<String> actuatorIds = new ArrayList<>();
            for (Actuator actuator : head.getActuators()) {
                actuatorIds.add(actuator.getId());
            }
            headInfo.setActuatorIds(actuatorIds);

            headInfoList.add(headInfo);
        }
        status.setHeads(headInfoList);

        // Получаем информацию о фидерах
        List<FeederInfo> feederInfoList = new ArrayList<>();
        for (Feeder feeder : machine.getFeeders()) {
            FeederInfo feederInfo = new FeederInfo();
            feederInfo.setId(feeder.getId());
            feederInfo.setName(feeder.getName());
            feederInfo.setEnabled(feeder.isEnabled());

            // Получаем тип фидера
            feederInfo.setType(feeder.getClass().getSimpleName());

            // Получаем ID части, если задана
            if (feeder.getPart() != null) {
                feederInfo.setPartId(feeder.getPart().getId());
            }

            feederInfoList.add(feederInfo);
        }
        status.setFeeders(feederInfoList);

        // Получаем тип планировщика движения
        if (machine.getMotionPlanner() != null) {
            status.setMotionPlannerType(machine.getMotionPlanner().getClass().getSimpleName());
        }

        return status;
    }

    /**
     * Включить машину
     */
    public void enableMachine() throws Exception {
        try {
            Configuration config = Configuration.get();
            if (config == null) {
                throw new Exception("Конфигурация не инициализирована");
            }

            Machine machine = config.getMachine();
            if (machine == null) {
                throw new Exception("Машина не сконфигурирована в файле конфигурации");
            }

            // Проверяем драйверы
            List<Driver> drivers = machine.getDrivers();
            if (drivers == null || drivers.isEmpty()) {
                throw new Exception("Не настроены драйверы машины. Проверьте конфигурацию machine.xml");
            }

            // Проверяем планировщик движения
            if (machine.getMotionPlanner() == null) {
                throw new Exception("Не настроен планировщик движения (MotionPlanner)");
            }

            // Включаем машину
            machine.setEnabled(true);

            // Проверяем что машина действительно включилась
            if (!machine.isEnabled()) {
                throw new Exception("Машина не смогла включиться. Проверьте подключение оборудования и драйверы");
            }

        } catch (Exception e) {
            throw new Exception("Ошибка при включении машины: " + e.getMessage(), e);
        }
    }

    /**
     * Выключить машину
     */
    public void disableMachine() throws Exception {
        try {
            Configuration config = Configuration.get();
            if (config == null) {
                throw new Exception("Конфигурация не инициализирована");
            }

            Machine machine = config.getMachine();
            if (machine == null) {
                throw new Exception("Машина не сконфигурирована");
            }

            machine.setEnabled(false);

        } catch (Exception e) {
            throw new Exception("Ошибка при выключении машины: " + e.getMessage(), e);
        }
    }

    /**
     * Выполнить хоминг машины
     */
    public void homeMachine() throws Exception {
        Machine machine = Configuration.get().getMachine();
        if (machine == null) {
            throw new Exception("Машина не сконфигурирована");
        }

        if (!machine.isEnabled()) {
            throw new MachineNotEnabledException();
        }

        // Выполняем хоминг через планировщик движения
        machine.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                machine.home();
                return null;
            }
        });
    }

    /**
     * Выполнить аварийную остановку
     */
    public void emergencyStop() throws Exception {
        Machine machine = Configuration.get().getMachine();
        if (machine == null) {
            throw new Exception("Машина не сконфигурирована");
        }

        // Останавливаем все драйверы
        for (Driver driver : machine.getDrivers()) {
            try {
                // Здесь должен быть вызов метода аварийной остановки драйвера
                // Пока используем setEnabled(false) как временное решение
                driver.setEnabled(false);
            } catch (Exception e) {
                // Игнорируем ошибки отдельных драйверов при аварийной остановке
            }
        }

        // Выключаем машину
        machine.setEnabled(false);
    }
}