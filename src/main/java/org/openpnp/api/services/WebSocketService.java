package org.openpnp.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import org.openpnp.api.models.machine.MachineStatus;
import org.pmw.tinylog.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления WebSocket подключениями и отправки обновлений статуса
 * машины
 */
public class WebSocketService {

    private static final ConcurrentHashMap<String, WsContext> connections = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final MachineService machineService = new MachineService();

    /**
     * Добавить новое WebSocket подключение
     */
    public static void addConnection(WsContext ctx) {
        String connectionId = ctx.sessionId();
        connections.put(connectionId, ctx);
        Logger.info("WebSocket подключение добавлено: " + connectionId);

        // Отправляем текущий статус сразу после подключения
        sendStatusUpdate(ctx);
    }

    /**
     * Удалить WebSocket подключение
     */
    public static void removeConnection(WsContext ctx) {
        String connectionId = ctx.sessionId();
        connections.remove(connectionId);
        Logger.info("WebSocket подключение удалено: " + connectionId);
    }

    /**
     * Отправить обновление статуса всем подключенным клиентам
     */
    public static void broadcastStatusUpdate() {
        try {
            MachineStatus status = machineService.getMachineStatus();
            String statusJson = objectMapper.writeValueAsString(status);

            connections.values().forEach(ctx -> {
                try {
                    ctx.send(statusJson);
                } catch (Exception e) {
                    Logger.error("Ошибка отправки WebSocket сообщения: " + e.getMessage());
                    // Удаляем проблемное подключение
                    removeConnection(ctx);
                }
            });

        } catch (Exception e) {
            Logger.error("Ошибка получения статуса машины для WebSocket: " + e.getMessage());
        }
    }

    /**
     * Отправить обновление статуса конкретному клиенту
     */
    private static void sendStatusUpdate(WsContext ctx) {
        try {
            MachineStatus status = machineService.getMachineStatus();
            String statusJson = objectMapper.writeValueAsString(status);
            ctx.send(statusJson);
        } catch (Exception e) {
            Logger.error("Ошибка отправки статуса клиенту: " + e.getMessage());
        }
    }

    /**
     * Получить количество активных подключений
     */
    public static int getConnectionCount() {
        return connections.size();
    }

    /**
     * Запустить реалтаймовые WebSocket обновления
     */
    public static void startPeriodicUpdates() {
        Logger.info("Запущен реалтаймовый режим WebSocket обновлений (только при изменениях)");
    }

    /**
     * Остановить реалтаймовые WebSocket обновления
     */
    public static void stopPeriodicUpdates() {
        Logger.info("Реалтаймовые WebSocket обновления остановлены");
    }

    /**
     * Очистить все подключения
     */
    public static void cleanup() {
        connections.clear();
        stopPeriodicUpdates();
        Logger.info("WebSocket сервис очищен");
    }
}