import os, sys, logging
from telegram import Update, ReplyKeyboardMarkup, KeyboardButton
from telegram.ext import ApplicationBuilder, CommandHandler, MessageHandler, ContextTypes, filters

if "django" not in sys.modules or not os.environ.get("DJANGO_SETTINGS_MODULE"):
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings")
    import django; django.setup()

from django.utils import timezone
from rides.models import TelegramAuthSession
logger = logging.getLogger(__name__)

async def cmd_start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    welcome = f"👋 Привет, {user.first_name}!\n\nЯ помогу войти в <b>WaynixGO</b>.\n\nНажми кнопку ниже, чтобы поделиться своим номером телефона 👇"
    keyboard = ReplyKeyboardMarkup([[KeyboardButton("📱 Поделиться номером", request_contact=True)]], resize_keyboard=True, one_time_keyboard=True)
    await update.message.reply_text(welcome, reply_markup=keyboard, parse_mode="HTML")

async def handle_contact(update: Update, context: ContextTypes.DEFAULT_TYPE):
    contact = update.message.contact
    if not contact or not contact.phone_number:
        await update.message.reply_text("❌ Не удалось получить номер. /start")
        return
    phone_digits = "".join(filter(str.isdigit, contact.phone_number))
    session = None
    for s in TelegramAuthSession.objects.filter(status="pending").order_by("-created_at")[:50]:
        sd = "".join(filter(str.isdigit, s.phone))
        if sd[-9:] == phone_digits[-9:]:
            session = s
            break
    if not session:
        await update.message.reply_text("❌ Активная сессия не найдена. Введите номер в приложении WaynixGO.", parse_mode="HTML")
        return
    if not session.is_valid:
        await update.message.reply_text("⏰ Сессия истекла. Начните заново.")
        return
    session.telegram_user_id = update.effective_user.id
    session.status = "code_sent"
    session.save()
    await update.message.reply_text(f"✅ Код: <code>{session.code}</code>\nВернитесь в приложение.", parse_mode="HTML", reply_markup={})

async def handle_unknown(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("Нажмите /start чтобы начать.")

def run_bot():
    from django.conf import settings
    token = getattr(settings, "TELEGRAM_BOT_TOKEN", "")
    if not token:
        raise ValueError("TELEGRAM_BOT_TOKEN не задан!")
    app = ApplicationBuilder().token(token).build()
    app.add_handler(CommandHandler("start", cmd_start))
    app.add_handler(MessageHandler(filters.CONTACT, handle_contact))
    app.add_handler(MessageHandler(filters.ALL & ~filters.COMMAND & ~filters.CONTACT, handle_unknown))
    app.run_polling(drop_pending_updates=True)

if __name__ == "__main__":
    run_bot()
