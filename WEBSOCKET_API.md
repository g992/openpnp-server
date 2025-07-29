# WebSocket API для OpenPnP

## Обзор

WebSocket API позволяет получать обновления статуса машины OpenPnP в реальном времени без необходимости постоянного опроса REST API.

## Подключение

### WebSocket URL
```
ws://localhost:8080/ws/machine-status
```

### Подключение через JavaScript
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/machine-status');

ws.onopen = function(event) {
    console.log('WebSocket подключение установлено');
};

ws.onmessage = function(event) {
    const status = JSON.parse(event.data);
    console.log('Получен статус машины:', status);
};

ws.onclose = function(event) {
    console.log('WebSocket соединение закрыто');
};

ws.onerror = function(error) {
    console.error('Ошибка WebSocket:', error);
};
```

## Команды

### Отправка команд клиентом

Клиент может отправлять команды на сервер:

- `ping` - проверка соединения (сервер отвечает `pong`)
- `getStatus` - запрос текущего статуса машины

```javascript
ws.send('ping');
ws.send('getStatus');
```

## Формат данных

### Статус машины (JSON)

```json
{
  "enabled": true,
  "busy": false,
  "homed": true,
  "motionPlannerType": "ReferenceMotionPlanner",
  "axes": [
    {
      "id": "X",
      "name": "X Axis",
      "type": "X",
      "position": 100.5,
      "unit": "mm",
      "homed": true
    },
    {
      "id": "Y", 
      "name": "Y Axis",
      "type": "Y",
      "position": 50.2,
      "unit": "mm",
      "homed": true
    }
  ],
  "heads": [
    {
      "id": "head1",
      "name": "Head 1",
      "nozzleIds": ["nozzle1", "nozzle2"],
      "cameraIds": ["camera1"],
      "actuatorIds": ["actuator1"]
    }
  ],
  "feeders": [
    {
      "id": "feeder1",
      "name": "Strip Feeder 1",
      "enabled": true,
      "type": "ReferenceStripFeeder",
      "partId": "part1"
    }
  ]
}
```

## События

WebSocket автоматически отправляет обновления при следующих событиях:

- Включение/выключение машины
- Изменение состояния занятости
- Выполнение хоминга
- Активность головки
- Активность актуатора
- Пользовательские действия
- Периодические обновления (каждую секунду при наличии подключений)

## Примеры использования

### Простой мониторинг статуса

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/machine-status');

ws.onmessage = function(event) {
    const status = JSON.parse(event.data);
    
    // Обновляем UI
    document.getElementById('enabled').textContent = status.enabled ? 'Включена' : 'Выключена';
    document.getElementById('busy').textContent = status.busy ? 'Занята' : 'Свободна';
    
    // Обновляем позиции осей
    status.axes.forEach(axis => {
        const element = document.getElementById(`axis-${axis.id}`);
        if (element) {
            element.textContent = `${axis.position} ${axis.unit}`;
        }
    });
};
```

### Обработка ошибок и переподключение

```javascript
let ws = null;
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;

function connect() {
    ws = new WebSocket('ws://localhost:8080/ws/machine-status');
    
    ws.onopen = function() {
        console.log('Подключено');
        reconnectAttempts = 0;
    };
    
    ws.onclose = function() {
        console.log('Соединение закрыто');
        
        // Попытка переподключения
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            setTimeout(connect, 3000);
        }
    };
    
    ws.onerror = function(error) {
        console.error('Ошибка WebSocket:', error);
    };
}

connect();
```

### Запрос статуса по требованию

```javascript
function requestStatus() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send('getStatus');
    }
}

// Запрашиваем статус каждые 5 секунд
setInterval(requestStatus, 5000);
```

### Управление частотой обновлений

```javascript
// Установить частоту обновлений на 200 мс (5 раз в секунду)
fetch('/api/websocket/update-interval?interval=200', {
    method: 'POST'
})
.then(response => response.json())
.then(data => console.log(data.message));

// Получить информацию о текущих настройках
fetch('/api/websocket/info')
.then(response => response.json())
.then(data => {
    console.log(`Подключений: ${data.connectionCount}`);
    console.log(`Интервал: ${data.updateIntervalMs} мс`);
    console.log(`Частота: ${(1000 / data.updateIntervalMs).toFixed(1)} обновлений/сек`);
});
```

### Режимы работы

#### Реалтаймовый режим (по умолчанию)
- Обновления отправляются только при изменениях состояния машины
- Максимальная эффективность и минимальная нагрузка на сеть
- Идеально для мониторинга в реальном времени

#### Периодический режим
- **100 мс (10 обновлений/сек)** - для высокоточного мониторинга движения
- **200 мс (5 обновлений/сек)** - для плавного отображения позиций
- **500 мс (2 обновления/сек)** - для общего мониторинга
- **1000 мс (1 обновление/сек)** - для экономии ресурсов

## Тестирование

Для тестирования WebSocket API используйте файл `websocket-test.html`:

1. Запустите OpenPnP с включенным API сервером
2. Откройте `websocket-test.html` в браузере
3. Нажмите "Подключиться" для установки WebSocket соединения
4. Наблюдайте за обновлениями статуса в реальном времени

## Настройка

### Системные свойства

- `openpnp.api.enabled=true` - включить API сервер (по умолчанию: true)
- `openpnp.api.port=8080` - порт API сервера (по умолчанию: 8080)
- `openpnp.websocket.update.interval=1000` - интервал обновлений WebSocket в миллисекундах (по умолчанию: 1000)
- `openpnp.websocket.realtime.mode=true` - реалтаймовый режим обновлений (по умолчанию: true)

### Пример запуска

```bash
# Реалтаймовый режим (по умолчанию) - обновления только при изменениях
java -Dopenpnp.api.enabled=true -Dopenpnp.api.port=8080 -jar openpnp.jar

# Периодический режим с обновлениями каждые 500 мс (2 раза в секунду)
java -Dopenpnp.api.enabled=true -Dopenpnp.api.port=8080 -Dopenpnp.websocket.realtime.mode=false -Dopenpnp.websocket.update.interval=500 -jar openpnp.jar

# Периодический режим с обновлениями каждые 100 мс (10 раз в секунду)
java -Dopenpnp.api.enabled=true -Dopenpnp.api.port=8080 -Dopenpnp.websocket.realtime.mode=false -Dopenpnp.websocket.update.interval=100 -jar openpnp.jar
```

### Динамическое управление частотой

#### Получить информацию о WebSocket
```bash
GET /api/websocket/info
```

Ответ:
```json
{
  "connectionCount": 2,
  "updateIntervalMs": 500,
  "realtimeMode": true
}
```

#### Установить частоту обновлений
```bash
POST /api/websocket/update-interval?interval=200
```

Ответ:
```json
{
  "message": "Частота обновлений изменена на 200 мс"
}
```

**Ограничения:**
- Минимальный интервал: 100 мс (10 обновлений в секунду)
- Максимальный интервал: не ограничен (но рекомендуется не более 10 секунд)
- В реалтаймовом режиме обновления отправляются только при изменениях состояния машины

### Проблемы подключения

1. Убедитесь, что API сервер запущен
2. Проверьте порт (по умолчанию 8080)
3. Проверьте логи OpenPnP на наличие ошибок

### Отсутствие обновлений

1. Убедитесь, что машина сконфигурирована
2. Проверьте, что WebSocket слушатель инициализирован
3. Проверьте логи на наличие ошибок в MachineService

### Высокое потребление ресурсов

1. Уменьшите частоту обновлений в WebSocketService
2. Ограничьте количество подключений
3. Используйте фильтрацию событий 