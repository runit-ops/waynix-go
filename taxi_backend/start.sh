#!/bin/bash
cd "$(dirname "$0")"
[ -d "venv" ] && source venv/bin/activate
[ -f ".env" ] && export $(grep -v '^#' .env | xargs)
python manage.py runserver 0.0.0.0:8000 --noreload &
python manage.py run_bot &
wait
