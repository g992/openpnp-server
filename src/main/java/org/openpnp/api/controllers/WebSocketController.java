package org.openpnp.api.controllers;

import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import io.javalin.http.Context;
import org.openpnp.api.services.WebSocketService;
import org.openpnp.api.services.MachineService;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Head;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.MotionPlanner;
import org.pmw.tinylog.Logger;

import java.util.concurrent.Callable;

/**
 * Контроллер для обработки WebSocket подключений
 */
public class WebSocketController {

    private static final MachineService machineService = new MachineService();

    /**
     * Обработчик подключения WebSocket
     */
    public static void onConnect(WsContext ctx) {
        WebSocketService.addConnection(ctx);
    }

    /**
     * Обработчик отключения WebSocket
     */
    public static void onClose(WsContext ctx) {
        WebSocketService.removeConnection(ctx);
    }

    /**
     * Обработчик сообщений WebSocket
     */
    public static void onMessage(WsMessageContext ctx) {
        try {
            String message = ctx.message();
            Logger.debug("Получено WebSocket сообщение: " + message);

            // Обрабатываем команды от клиента
            if ("ping".equals(message)) {
                ctx.send("pong");
            } else if ("getStatus".equals(message)) {
                // Отправляем текущий статус по запросу
                WebSocketService.broadcastStatusUpdate();
            } else if ("getHeadMountables".equals(message)) {
                // Получаем список доступных HeadMountable компонентов
                handleGetHeadMountables(ctx);
            } else if (message.startsWith("getPosition:")) {
                // Получаем текущую позицию компонента
                handleGetPositionCommand(ctx, message);
            } else if (message.startsWith("move:")) {
                // Обработка команд перемещения
                handleMoveCommand(ctx, message);
            } else if (message.startsWith("moveAxis:")) {
                // Обработка команд перемещения по отдельным осям
                handleMoveAxisCommand(ctx, message);
            } else if (message.startsWith("jog:")) {
                // Обработка команд относительного перемещения (jogging)
                handleJogCommand(ctx, message);
            } else if (message.startsWith("home:")) {
                // Обработка команд хоминга
                handleHomeCommand(ctx, message);
            } else if (message.startsWith("stop:")) {
                // Обработка команд остановки
                handleStopCommand(ctx, message);
            } else {
                Logger.warn("Неизвестная WebSocket команда: " + message);
                ctx.send("error: Неизвестная команда");
            }

        } catch (Exception e) {
            Logger.error("Ошибка обработки WebSocket сообщения: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Обработка команды получения списка HeadMountable компонентов
     */
    private static void handleGetHeadMountables(WsMessageContext ctx) {
        try {
            Machine machine = Configuration.get().getMachine();
            if (machine == null) {
                ctx.send("error: Машина не инициализирована");
                return;
            }

            StringBuilder response = new StringBuilder();
            response.append("headMountables:");

            for (Head head : machine.getHeads()) {
                // Добавляем камеры
                for (Camera camera : head.getCameras()) {
                    response.append(camera.getId()).append(":").append(camera.getName()).append(":camera;");
                }
                // Добавляем сопла
                for (Nozzle nozzle : head.getNozzles()) {
                    response.append(nozzle.getId()).append(":").append(nozzle.getName()).append(":nozzle;");
                }
            }

            ctx.send(response.toString());
            Logger.info("Отправлен список HeadMountable компонентов");

        } catch (Exception e) {
            Logger.error("Ошибка получения списка HeadMountable: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Обработка команд перемещения в позицию
     * Формат: move:headMountableId:x:y:z:rotation:speed
     */
    private static void handleMoveCommand(WsMessageContext ctx, String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 6) {
                ctx.send("error: Неверный формат команды move. Ожидается: move:headMountableId:x:y:z:rotation:speed");
                return;
            }

            String headMountableId = parts[1];
            double x = parseCoordinate(parts[2]);
            double y = parseCoordinate(parts[3]);
            double z = parseCoordinate(parts[4]);
            double rotation = parseCoordinate(parts[5]);
            double speed = parts.length > 6 ? Double.parseDouble(parts[6]) : 0.5;

            Machine machine = Configuration.get().getMachine();
            if (machine == null) {
                ctx.send("error: Машина не инициализирована");
                return;
            }

            if (!machine.isEnabled()) {
                ctx.send("error: Машина не включена");
                return;
            }

            HeadMountable headMountable = findHeadMountable(machine, headMountableId);
            if (headMountable == null) {
                ctx.send("error: HeadMountable с ID '" + headMountableId + "' не найден");
                return;
            }

            // Создаем новую позицию
            Location targetLocation = new Location(LengthUnit.Millimeters, x, y, z, rotation);

            // Выполняем перемещение через машину
            machine.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    headMountable.moveTo(targetLocation, speed);
                    return null;
                }
            });

            ctx.send("success: Перемещение выполнено");
            Logger.info("Выполнено перемещение " + headMountable.getName() + " в позицию " + targetLocation);

        } catch (Exception e) {
            Logger.error("Ошибка выполнения команды move: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Обработка команд перемещения по отдельным осям
     * Формат: moveAxis:headMountableId:axisType:coordinate:speed
     */
    private static void handleMoveAxisCommand(WsMessageContext ctx, String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 5) {
                ctx.send(
                        "error: Неверный формат команды moveAxis. Ожидается: moveAxis:headMountableId:axisType:coordinate:speed");
                return;
            }

            String headMountableId = parts[1];
            String axisType = parts[2];
            double coordinate = Double.parseDouble(parts[3]);
            double speed = parts.length > 4 ? Double.parseDouble(parts[4]) : 0.5;

            Machine machine = Configuration.get().getMachine();
            if (machine == null) {
                ctx.send("error: Машина не инициализирована");
                return;
            }

            if (!machine.isEnabled()) {
                ctx.send("error: Машина не включена");
                return;
            }

            HeadMountable headMountable = findHeadMountable(machine, headMountableId);
            if (headMountable == null) {
                ctx.send("error: HeadMountable с ID '" + headMountableId + "' не найден");
                return;
            }

            // Получаем текущую позицию
            Location currentLocation = headMountable.getLocation();
            Location targetLocation;

            // Создаем новую позицию, изменяя только указанную ось
            switch (axisType.toUpperCase()) {
                case "X":
                    targetLocation = currentLocation.derive(coordinate, null, null, null);
                    break;
                case "Y":
                    targetLocation = currentLocation.derive(null, coordinate, null, null);
                    break;
                case "Z":
                    targetLocation = currentLocation.derive(null, null, coordinate, null);
                    break;
                case "ROTATION":
                case "C":
                    targetLocation = currentLocation.derive(null, null, null, coordinate);
                    break;
                default:
                    ctx.send("error: Неизвестный тип оси: " + axisType);
                    return;
            }

            // Выполняем перемещение через машину
            machine.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    headMountable.moveTo(targetLocation, speed);
                    return null;
                }
            });

            ctx.send("success: Перемещение по оси " + axisType + " выполнено");
            Logger.info("Выполнено перемещение " + headMountable.getName() + " по оси " + axisType + " в позицию "
                    + coordinate);

        } catch (Exception e) {
            Logger.error("Ошибка выполнения команды moveAxis: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Обработка команд относительного перемещения (jogging)
     * Формат: jog:headMountableId:axisType:offset:speed
     */
    private static void handleJogCommand(WsMessageContext ctx, String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 5) {
                ctx.send(
                        "error: Неверный формат команды jog. Ожидается: jog:headMountableId:axisType:offset:speed");
                return;
            }

            String headMountableId = parts[1];
            String axisType = parts[2];
            double offset = Double.parseDouble(parts[3]);
            double speed = parts.length > 4 ? Double.parseDouble(parts[4]) : 0.5;

            Machine machine = Configuration.get().getMachine();
            if (machine == null) {
                ctx.send("error: Машина не инициализирована");
                return;
            }

            if (!machine.isEnabled()) {
                ctx.send("error: Машина не включена");
                return;
            }

            HeadMountable headMountable = findHeadMountable(machine, headMountableId);
            if (headMountable == null) {
                ctx.send("error: HeadMountable с ID '" + headMountableId + "' не найден");
                return;
            }

            // Получаем текущую позицию
            Location currentLocation = headMountable.getLocation();
            Location targetLocation;

            // Создаем новую позицию, добавляя смещение к текущей позиции
            switch (axisType.toUpperCase()) {
                case "X":
                    double newX = currentLocation.getX() + offset;
                    targetLocation = currentLocation.derive(newX, null, null, null);
                    break;
                case "Y":
                    double newY = currentLocation.getY() + offset;
                    targetLocation = currentLocation.derive(null, newY, null, null);
                    break;
                case "Z":
                    double newZ = currentLocation.getZ() + offset;
                    targetLocation = currentLocation.derive(null, null, newZ, null);
                    break;
                case "ROTATION":
                case "C":
                    double newRotation = currentLocation.getRotation() + offset;
                    targetLocation = currentLocation.derive(null, null, null, newRotation);
                    break;
                default:
                    ctx.send("error: Неизвестный тип оси: " + axisType);
                    return;
            }

            // Выполняем перемещение через машину
            machine.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    headMountable.moveTo(targetLocation, speed);
                    return null;
                }
            });

            ctx.send("success: Относительное перемещение по оси " + axisType + " на " + offset + " выполнено");
            Logger.info("Выполнено относительное перемещение " + headMountable.getName() + " по оси " + axisType
                    + " на " + offset + " (с " + currentLocation + " в " + targetLocation + ")");

        } catch (Exception e) {
            Logger.error("Ошибка выполнения команды jog: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Обработка команд хоминга
     * Формат: home:all или home:headMountableId
     */
    private static void handleHomeCommand(WsMessageContext ctx, String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 2) {
                ctx.send("error: Неверный формат команды home. Ожидается: home:all или home:headMountableId");
                return;
            }

            String target = parts[1];
            Machine machine = Configuration.get().getMachine();
            if (machine == null) {
                ctx.send("error: Машина не инициализирована");
                return;
            }

            if (!machine.isEnabled()) {
                ctx.send("error: Машина не включена");
                return;
            }

            if ("all".equals(target)) {
                // Хоминг всей машины
                machineService.homeMachine();
                ctx.send("success: Хоминг машины выполнен");
                Logger.info("Выполнен хоминг всей машины");
            } else {
                // Хоминг конкретного HeadMountable
                HeadMountable headMountable = findHeadMountable(machine, target);
                if (headMountable == null) {
                    ctx.send("error: HeadMountable с ID '" + target + "' не найден");
                    return;
                }

                // Для HeadMountable выполняем хоминг через машину
                machine.execute(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        // Перемещаем в безопасную Z позицию
                        headMountable.moveToSafeZ();
                        return null;
                    }
                });

                ctx.send("success: Хоминг " + headMountable.getName() + " выполнен");
                Logger.info("Выполнен хоминг " + headMountable.getName());
            }

        } catch (Exception e) {
            Logger.error("Ошибка выполнения команды home: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Обработка команд остановки
     * Формат: stop:all или stop:headMountableId
     */
    private static void handleStopCommand(WsMessageContext ctx, String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 2) {
                ctx.send("error: Неверный формат команды stop. Ожидается: stop:all или stop:headMountableId");
                return;
            }

            String target = parts[1];
            Machine machine = Configuration.get().getMachine();
            if (machine == null) {
                ctx.send("error: Машина не инициализирована");
                return;
            }

            if ("all".equals(target)) {
                // Остановка всей машины
                machineService.emergencyStop();
                ctx.send("success: Аварийная остановка выполнена");
                Logger.info("Выполнена аварийная остановка машины");
            } else {
                // Остановка конкретного HeadMountable
                HeadMountable headMountable = findHeadMountable(machine, target);
                if (headMountable == null) {
                    ctx.send("error: HeadMountable с ID '" + target + "' не найден");
                    return;
                }

                // Ожидаем завершения движения
                machine.execute(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        headMountable.waitForCompletion(MotionPlanner.CompletionType.WaitForStillstand);
                        return null;
                    }
                });

                ctx.send("success: Остановка " + headMountable.getName() + " выполнена");
                Logger.info("Выполнена остановка " + headMountable.getName());
            }

        } catch (Exception e) {
            Logger.error("Ошибка выполнения команды stop: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Обработка команд получения позиции компонента
     * Формат: getPosition:headMountableId
     */
    private static void handleGetPositionCommand(WsMessageContext ctx, String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 2) {
                ctx.send("error: Неверный формат команды getPosition. Ожидается: getPosition:headMountableId");
                return;
            }

            String headMountableId = parts[1];
            Machine machine = Configuration.get().getMachine();
            if (machine == null) {
                ctx.send("error: Машина не инициализирована");
                return;
            }

            if (!machine.isEnabled()) {
                ctx.send("error: Машина не включена");
                return;
            }

            HeadMountable headMountable = findHeadMountable(machine, headMountableId);
            if (headMountable == null) {
                ctx.send("error: HeadMountable с ID '" + headMountableId + "' не найден");
                return;
            }

            // Получаем текущую позицию
            Location currentLocation = headMountable.getLocation();
            ctx.send("success: Текущая позиция " + headMountable.getName() + ": X=" + currentLocation.getX() + ", Y="
                    + currentLocation.getY() + ", Z=" + currentLocation.getZ() + ", Rotation="
                    + currentLocation.getRotation());
            Logger.info("Отправлена текущая позиция " + headMountable.getName());

        } catch (Exception e) {
            Logger.error("Ошибка получения позиции компонента: " + e.getMessage());
            ctx.send("error: " + e.getMessage());
        }
    }

    /**
     * Поиск HeadMountable по ID или имени
     */
    private static HeadMountable findHeadMountable(Machine machine, String identifier) {
        for (Head head : machine.getHeads()) {
            // Ищем среди камер
            for (Camera camera : head.getCameras()) {
                if (camera.getId().equals(identifier) || camera.getName().equals(identifier)) {
                    return camera;
                }
            }
            // Ищем среди сопел
            for (Nozzle nozzle : head.getNozzles()) {
                if (nozzle.getId().equals(identifier) || nozzle.getName().equals(identifier)) {
                    return nozzle;
                }
            }
        }
        return null;
    }

    /**
     * Парсинг координаты с поддержкой NaN
     */
    private static double parseCoordinate(String value) {
        if ("NaN".equals(value) || "null".equals(value) || value.trim().isEmpty()) {
            return Double.NaN;
        }
        return Double.parseDouble(value);
    }

    /**
     * Получить информацию о WebSocket сервисе
     */
    public static void getWebSocketInfo(Context ctx) {
        try {
            ctx.json(new WebSocketInfo(WebSocketService.getConnectionCount()));
        } catch (Exception e) {
            Logger.error("Ошибка получения информации о WebSocket: " + e.getMessage());
            ctx.status(500).json(new ErrorResponse("Ошибка получения информации о WebSocket"));
        }
    }

    /**
     * Внутренний класс для информации о WebSocket
     */
    private static class WebSocketInfo {
        public final int connectionCount;

        public WebSocketInfo(int connectionCount) {
            this.connectionCount = connectionCount;
        }
    }

    /**
     * Внутренний класс для ответов об ошибках
     */
    private static class ErrorResponse {
        public final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    /**
     * Внутренний класс для успешных ответов
     */
    private static class SuccessResponse {
        public final String message;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}