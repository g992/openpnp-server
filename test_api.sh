#!/bin/bash

# Тест API OpenPnP
echo "🚀 Тестирование OpenPnP API..."

API_URL="http://localhost:8080"

echo ""
echo "📍 1. Тест ping endpoint:"
response=$(curl -s "$API_URL/api/ping")
echo "$response" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✅ Success: {data['success']}\")
print(f\"📅 Version: {data['data']['version']}\")
print(f\"⏱️  Uptime: {data['data']['uptime_ms']} ms\")
print(f\"🔧 Machine: {'enabled' if data['data']['machine_enabled'] else 'disabled'}\")
"

echo ""
echo "📍 2. Тест health endpoint:"
response=$(curl -s "$API_URL/api/health")
echo "$response" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✅ Success: {data['success']}\")
print(f\"💬 Message: {data['message']}\")
"

echo ""
echo "📍 3. Тест API info endpoint:"
response=$(curl -s "$API_URL/api")
echo "$response" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f\"📛 Name: {data['name']}\")
print(f\"📝 Description: {data['description']}\")
print(f\"📅 Version: {data['version']}\")
print(f\"🔗 Endpoints: {', '.join(data['endpoints'].keys())}\")
"

echo ""
echo "📍 4. Тест корневой страницы:"
response=$(curl -s -w "Status: %{http_code}" "$API_URL/")
if [[ "$response" == *"Status: 200" ]]; then
    echo "✅ Корневая страница доступна"
else
    echo "❌ Ошибка при загрузке корневой страницы"
fi

echo ""
echo "📍 5. Тест несуществующего endpoint:"
status=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/api/nonexistent")
if [[ "$status" == "404" ]]; then
    echo "✅ 404 ошибка корректно обрабатывается"
else
    echo "❌ Неожиданный статус код: $status"
fi

echo ""
echo "🎉 Тестирование завершено!"
echo "🌐 API доступен по адресу: $API_URL"
echo "📖 Документация: $API_URL (откройте в браузере)" 