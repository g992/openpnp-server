# WebSocket API для стримов камер OpenPnP

## Обзор

WebSocket API для стримов камер позволяет получать изображения с камер OpenPnP в реальном времени через WebSocket соединение. Поддерживается множественные подключения к разным камерам с настраиваемым фреймрейтом и качеством изображения.

## Подключение

### WebSocket URL
```
ws://localhost:8080/ws/camera-stream
```

### Подключение через JavaScript
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/camera-stream');

ws.onopen = function(event) {
    console.log('WebSocket подключение установлено');
};

ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    console.log('Получены данные:', data);
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

Клиент может отправлять JSON команды на сервер:

#### Запуск стрима камеры
```javascript
ws.send(JSON.stringify({
    command: "start_stream",
    cameraId: "CAM001",
    fps: 10,
    quality: "medium"
}));
```

**Параметры:**
- `cameraId` (обязательный) - ID камеры
- `fps` (опциональный) - кадров в секунду (1-30, по умолчанию 10)
- `quality` (опциональный) - качество изображения ("low", "medium", "high", по умолчанию "medium")

#### Остановка стрима
```javascript
ws.send(JSON.stringify({
    command: "stop_stream"
}));
```

#### Получение списка камер
```javascript
ws.send(JSON.stringify({
    command: "get_cameras"
}));
```

#### Получение информации о стримах
```javascript
ws.send(JSON.stringify({
    command: "get_stream_info"
}));
```

#### Проверка соединения
```javascript
ws.send(JSON.stringify({
    command: "ping"
}));
```

## Формат данных

### Список камер (JSON)
```json
{
  "type": "cameras_list",
  "timestamp": 1640995200000,
  "cameras": [
    {
      "id": "CAM001",
      "name": "Bottom Camera",
      "type": "OpenPnpCaptureCamera",
      "width": 640,
      "height": 480,
      "location": "machine"
    },
    {
      "id": "CAM002", 
      "name": "Up Camera",
      "type": "OpenCvCamera",
      "width": 1280,
      "height": 720,
      "location": "head:Head1"
    }
  ]
}
```

### Кадр камеры (JSON)
```json
{
  "type": "frame",
  "cameraId": "CAM001",
  "timestamp": 1640995200000,
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQ...",
  "width": 640,
  "height": 480,
  "fps": 10
}
```

### Подтверждение запуска стрима
```json
{
  "type": "stream_started",
  "cameraId": "CAM001",
  "timestamp": 1640995200000,
  "fps": 10
}
```

### Подтверждение остановки стрима
```json
{
  "type": "stream_stopped",
  "timestamp": 1640995200000
}
```

### Сообщение об ошибке
```json
{
  "type": "error",
  "timestamp": 1640995200000,
  "error": "Камера с ID 'CAM001' не найдена"
}
```

### Ответ на ping
```json
{
  "type": "pong",
  "timestamp": 1640995200000
}
```

### Информация о стримах
```json
{
  "type": "stream_info",
  "timestamp": 1640995200000,
  "active_streams": 2,
  "streams": {
    "session1": "CAM001",
    "session2": "CAM002"
  }
}
```

## REST API

### Получение списка камер
```
GET /api/cameras/
```

**Ответ:**
```json
[
  {
    "id": "CAM001",
    "name": "Bottom Camera",
    "type": "OpenPnpCaptureCamera",
    "width": 640,
    "height": 480,
    "location": "machine"
  }
]
```

### Получение информации о стримах
```
GET /api/cameras/streams
```

**Ответ:**
```json
{
  "activeStreams": 2,
  "streams": {
    "session1": "CAM001",
    "session2": "CAM002"
  }
}
```

## Примеры использования

### JavaScript клиент
```javascript
class CameraStreamClient {
    constructor(url) {
        this.url = url;
        this.ws = null;
        this.onFrameCallback = null;
    }

    connect() {
        this.ws = new WebSocket(this.url);
        
        this.ws.onopen = () => {
            console.log('Подключено к стриму камер');
        };
        
        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            
            if (data.type === 'frame' && this.onFrameCallback) {
                this.onFrameCallback(data);
            }
        };
        
        this.ws.onclose = () => {
            console.log('Соединение закрыто');
        };
    }

    startStream(cameraId, fps = 10, quality = 'medium') {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                command: 'start_stream',
                cameraId: cameraId,
                fps: fps,
                quality: quality
            }));
        }
    }

    stopStream() {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                command: 'stop_stream'
            }));
        }
    }

    onFrame(callback) {
        this.onFrameCallback = callback;
    }

    disconnect() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// Использование
const client = new CameraStreamClient('ws://localhost:8080/ws/camera-stream');
client.connect();

client.onFrame((frameData) => {
    // Отображаем изображение
    const img = document.getElementById('camera-image');
    img.src = 'data:image/jpeg;base64,' + frameData.image;
    console.log(`Получен кадр с камеры ${frameData.cameraId}, FPS: ${frameData.fps}`);
});

// Запускаем стрим
client.startStream('CAM001', 15, 'high');
```

### Python клиент
```python
import websocket
import json
import base64
from PIL import Image
import io

class CameraStreamClient:
    def __init__(self, url):
        self.url = url
        self.ws = None
        
    def on_message(self, ws, message):
        data = json.loads(message)
        
        if data['type'] == 'frame':
            # Декодируем Base64 изображение
            image_data = base64.b64decode(data['image'])
            image = Image.open(io.BytesIO(image_data))
            
            # Сохраняем или обрабатываем изображение
            image.save(f"frame_{data['cameraId']}_{data['timestamp']}.jpg")
            print(f"Сохранен кадр с камеры {data['cameraId']}")
    
    def on_error(self, ws, error):
        print(f"Ошибка: {error}")
    
    def on_close(self, ws, close_status_code, close_msg):
        print("Соединение закрыто")
    
    def on_open(self, ws):
        print("Подключено к стриму камер")
        
        # Запускаем стрим
        ws.send(json.dumps({
            "command": "start_stream",
            "cameraId": "CAM001",
            "fps": 10,
            "quality": "medium"
        }))
    
    def connect(self):
        websocket.enableTrace(True)
        self.ws = websocket.WebSocketApp(
            self.url,
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        self.ws.run_forever()

# Использование
client = CameraStreamClient("ws://localhost:8080/ws/camera-stream")
client.connect()
```

## Настройки качества

### Уровни качества изображения:
- **low** - сжатие JPEG 50%, подходит для медленных соединений
- **medium** - сжатие JPEG 80%, оптимальный баланс качества и размера
- **high** - сжатие JPEG 95%, максимальное качество

### Рекомендуемые настройки FPS:
- **1-5 FPS** - для статичных изображений, мониторинга
- **10-15 FPS** - для обычного просмотра
- **20-30 FPS** - для плавного видео (требует быстрого соединения)

## Ограничения

1. **Максимальный FPS**: 30 кадров в секунду
2. **Максимальное количество одновременных стримов**: ограничено ресурсами системы
3. **Размер изображения**: зависит от разрешения камеры
4. **Сетевая пропускная способность**: влияет на качество и FPS стрима

## Обработка ошибок

### Типичные ошибки:
- `Камера с ID 'XXX' не найдена` - камера не существует или недоступна
- `Не удалось захватить изображение с камеры` - проблема с камерой
- `Ошибка стрима: ...` - другие ошибки стрима

### Рекомендации:
1. Всегда проверяйте список доступных камер перед запуском стрима
2. Обрабатывайте ошибки в клиентском коде
3. Используйте переподключение при разрыве соединения
4. Мониторьте производительность при высоких FPS

## Безопасность

- WebSocket соединения не требуют аутентификации
- Рекомендуется использовать HTTPS/WSS в продакшене
- Ограничьте доступ к API в корпоративных сетях 