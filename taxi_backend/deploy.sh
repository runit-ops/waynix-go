#!/bin/bash
# Запуск WaynixGO в фоне (nohup)
cd "$(dirname "$0")"

# Убиваем старые процессы
pkill -f "manage.py runserver" 2>/dev/null
pkill -f "manage.py run_bot" 2>/dev/null
sleep 1

# Активируем venv
source venv/bin/activate

# Загружаем env
export $(grep -v '^#' .env | xargs)

echo "🚀 Запуск WaynixGO..."

# Django бэкенд на порту 5556
nohup python manage.py runserver 0.0.0.0:5556 --noreload > backend.log 2>&1 &
echo "✅ Бэкенд запущен (PID: $!)"

# Telegram бот
nohup python manage.py run_bot > bot.log 2>&1 &
echo "✅ Бот запущен (PID: $!)"

echo ""
echo "📋 Логи:"
echo "   tail -f backend.log"
echo "   tail -f bot.log"
echo ""
echo "🛑 Остановить:"
echo "   pkill -f 'manage.py runserver'"
echo "   pkill -f 'manage.py run_bot'"
