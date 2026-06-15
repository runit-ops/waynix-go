from django.core.management.base import BaseCommand

class Command(BaseCommand):
    help = "Запуск Telegram-бота"
    def handle(self, *args, **options):
        from telegram_bot.bot import run_bot
        run_bot()
