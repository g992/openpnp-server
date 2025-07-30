# WebSocket API для управления перемещением OpenPnP

## Обзор

WebSocket API позволяет управлять перемещением машины OpenPnP в реальном времени через WebSocket соединения. Все команды отправляются в виде текстовых сообщений и возвращают ответы в формате JSON или простого текста.

## Подключение

```javascript
const ws = new WebSocket('ws://localhost:8080/ws');
```

## Команды

### 1. Получение статуса

**Команда:** `getStatus`

**Описание:** Запрашивает текущий статус машины

**Пример:**
```javascript
ws.send('getStatus');
```

**Ответ:** JSON с информацией о статусе машины, осях, головках и фидерах

### 2. Получение списка компонентов

**Команда:** `getHeadMountables`

**Описание:** Получает список всех доступных HeadMountable компонентов (камеры, сопла)

**Пример:**
```javascript
ws.send('getHeadMountables');
```

**Ответ:** `headMountables:id:name:type;id:name:type;...`
- `id` - уникальный идентификатор компонента
- `name` - имя компонента
- `type` - тип компонента (camera/nozzle)

**Пример ответа:**
```
headMountables:C1:Top:camera;N1:N1:nozzle;
```

### 3. Получение текущей позиции

**Команда:** `getPosition:headMountableId`

**Описание:** Получает текущую позицию указанного компонента

**Пример:**
```javascript
ws.send('getPosition:C1');
```

**Ответ:** `success: Текущая позиция ComponentName: X=100.0, Y=200.0, Z=50.0, Rotation=0.0`

### 4. Перемещение в позицию

**Команда:** `move:headMountableId:x:y:z:rotation:speed`

**Описание:** Перемещает указанный компонент в заданную позицию

**Параметры:**
- `headMountableId` - ID камеры или сопла
- `x` - координата X (мм) или "NaN" для пропуска
- `y` - координата Y (мм) или "NaN" для пропуска  
- `z` - координата Z (мм) или "NaN" для пропуска
- `rotation` - угол поворота (градусы) или "NaN" для пропуска
- `speed` - скорость (0.0-1.0, опционально, по умолчанию 0.5)

**Примеры:**
```javascript
// Перемещение камеры в позицию (100, 200, 50, 0)
ws.send('move:C1:100:200:50:0:0.5');

// Перемещение только по X и Y (Z и rotation остаются неизменными)
ws.send('move:N1:150:250:NaN:NaN:0.3');

// Перемещение только по Z
ws.send('move:C1:NaN:NaN:30:NaN:0.7');
```

### 5. Перемещение по отдельной оси

**Команда:** `moveAxis:headMountableId:axisType:coordinate:speed`

**Описание:** Перемещает указанный компонент только по одной оси

**Параметры:**
- `headMountableId` - ID камеры или сопла
- `axisType` - тип оси: "X", "Y", "Z", "ROTATION" или "C"
- `coordinate` - целевая координата
- `speed` - скорость (0.0-1.0, опционально, по умолчанию 0.5)

**Примеры:**
```javascript
// Перемещение по оси X
ws.send('moveAxis:C1:X:100:0.5');

// Перемещение по оси Y
ws.send('moveAxis:N1:Y:200:0.3');

// Перемещение по оси Z
ws.send('moveAxis:C1:Z:50:0.7');

// Поворот (Rotation или C)
ws.send('moveAxis:N1:ROTATION:90:0.4');
```

### 6. Относительное перемещение (Jogging)

**Команда:** `jog:headMountableId:axisType:offset:speed`

**Описание:** Перемещает указанный компонент на заданное смещение от текущей позиции

**Параметры:**
- `headMountableId` - ID камеры или сопла
- `axisType` - тип оси: "X", "Y", "Z", "ROTATION" или "C"
- `offset` - смещение от текущей позиции (может быть положительным или отрицательным)
- `speed` - скорость (0.0-1.0, опционально, по умолчанию 0.5)

**Примеры:**
```javascript
// Перемещение на +5 мм по оси X
ws.send('jog:C1:X:5:0.5');

// Перемещение на -2 мм по оси Y
ws.send('jog:N1:Y:-2:0.3');

// Перемещение на +10 мм по оси Z
ws.send('jog:C1:Z:10:0.7');

// Поворот на +15 градусов
ws.send('jog:N1:ROTATION:15:0.4');

// Поворот на -30 градусов
ws.send('jog:C1:ROTATION:-30:0.6');
```

### 7. Хоминг

**Команда:** `home:target`

**Описание:** Выполняет хоминг машины или компонента

**Параметры:**
- `target` - "all" для хоминга всей машины или ID компонента

**Примеры:**
```javascript
// Хоминг всей машины
ws.send('home:all');

// Хоминг конкретной камеры
ws.send('home:C1');

// Хоминг конкретного сопла
ws.send('home:N1');
```

### 8. Остановка

**Команда:** `stop:target`

**Описание:** Останавливает движение машины или компонента

**Параметры:**
- `target` - "all" для аварийной остановки всей машины или ID компонента

**Примеры:**
```javascript
// Аварийная остановка всей машины
ws.send('stop:all');

// Остановка конкретного компонента
ws.send('stop:C1');
```

### 9. Ping/Pong

**Команда:** `ping`

**Описание:** Проверка соединения

**Пример:**
```javascript
ws.send('ping');
```

**Ответ:** `pong`

## Ответы

### Успешные ответы
```
success: Описание действия
```

### Ошибки
```
error: Описание ошибки
```

## Примеры использования

### JavaScript клиент

```javascript
const ws = new WebSocket('ws://localhost:8080/ws');

ws.onopen = function() {
    console.log('WebSocket соединение установлено');
    
    // Получаем статус
    ws.send('getStatus');
};

ws.onmessage = function(event) {
    const message = event.data;
    
    if (message.startsWith('success:')) {
        console.log('Успех:', message.substring(8));
    } else if (message.startsWith('error:')) {
        console.error('Ошибка:', message.substring(6));
    } else {
        // JSON статус
        try {
            const status = JSON.parse(message);
            console.log('Статус машины:', status);
        } catch (e) {
            console.log('Сообщение:', message);
        }
    }
};

// Примеры команд
function moveCamera(x, y, z, rotation) {
    ws.send(`move:C1:${x}:${y}:${z}:${rotation}:0.5`);
}

function moveNozzle(x, y, z, rotation) {
    ws.send(`move:N1:${x}:${y}:${z}:${rotation}:0.5`);
}

function jogX(offset) {
    ws.send(`jog:C1:X:${offset}:0.5`);
}

function jogY(offset) {
    ws.send(`jog:C1:Y:${offset}:0.5`);
}

function jogZ(offset) {
    ws.send(`jog:C1:Z:${offset}:0.5`);
}

function jogRotation(offset) {
    ws.send(`jog:C1:ROTATION:${offset}:0.5`);
}

function getCurrentPosition() {
    ws.send('getPosition:C1');
}

function homeMachine() {
    ws.send('home:all');
}

function stopMachine() {
    ws.send('stop:all');
}
```

### Python клиент

```python
import websocket
import json

def on_message(ws, message):
    if message.startswith('success:'):
        print(f"Успех: {message[8:]}")
    elif message.startswith('error:'):
        print(f"Ошибка: {message[6:]}")
    else:
        try:
            status = json.loads(message)
            print(f"Статус машины: {status}")
        except:
            print(f"Сообщение: {message}")

def on_error(ws, error):
    print(f"Ошибка WebSocket: {error}")

def on_close(ws, close_status_code, close_msg):
    print("WebSocket соединение закрыто")

def on_open(ws):
    print("WebSocket соединение установлено")
    ws.send("getStatus")

# Создание WebSocket соединения
ws = websocket.WebSocketApp("ws://localhost:8080/ws",
                          on_open=on_open,
                          on_message=on_message,
                          on_error=on_error,
                          on_close=on_close)

# Примеры команд
def move_camera(x, y, z, rotation):
    ws.send(f"move:C1:{x}:{y}:{z}:{rotation}:0.5")

def move_nozzle(x, y, z, rotation):
    ws.send(f"move:N1:{x}:{y}:{z}:{rotation}:0.5")

def home_machine():
    ws.send("home:all")

def stop_machine():
    ws.send("stop:all")

# Запуск WebSocket
ws.run_forever()
```

## Безопасность

- Все команды выполняются через `Machine.execute()` для обеспечения потокобезопасности
- Проверяется состояние машины (включена/выключена) перед выполнением команд
- Валидируются все входные параметры
- Логируются все операции для отладки

## Ограничения

- Машина должна быть включена для выполнения команд перемещения
- Компоненты должны быть правильно сконфигурированы в machine.xml
- Скорость ограничена диапазоном 0.0-1.0
- Координаты должны быть в миллиметрах, углы в градусах

## Получение ID компонентов

Перед использованием команд перемещения необходимо получить список доступных компонентов:

```javascript
// Получить список всех HeadMountable компонентов
ws.send('getHeadMountables');
```

Ответ будет содержать ID, имена и типы всех доступных компонентов:
```
headMountables:C1:Top:camera;N1:N1:nozzle;
```

Используйте полученные ID в командах перемещения:
```javascript
// Используйте реальные ID из ответа getHeadMountables
ws.send('move:C1:100:200:50:0:0.5');  // C1 - ID камеры
ws.send('move:N1:150:250:30:90:0.3'); // N1 - ID сопла

// Относительное перемещение (jogging)
ws.send('jog:C1:X:5:0.5');    // Перемещение на +5 мм по X
ws.send('jog:C1:Y:-2:0.3');   // Перемещение на -2 мм по Y
ws.send('jog:N1:Z:10:0.7');   // Перемещение на +10 мм по Z
ws.send('jog:N1:ROTATION:15:0.4'); // Поворот на +15 градусов
```

## Отладка

Все операции логируются с уровнем INFO или ERROR. Проверьте логи OpenPnP для диагностики проблем. 