package org.openpnp.api.controllers;

import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import io.javalin.http.Context;
import org.openpnp.api.services.WebSocketService;
import org.pmw.tinylog.Logger;

/**
 * Контроллер для обработки WebSocket подключений
 */
public class WebSocketController {

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
            } else {
                Logger.warn("Неизвестная WebSocket команда: " + message);
            }

        } catch (Exception e) {
            Logger.error("Ошибка обработки WebSocket сообщения: " + e.getMessage());
        }
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