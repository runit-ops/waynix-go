"""
rides/models.py — Модели базы данных
"""
import uuid
from django.db import models
from django.utils import timezone


class Driver(models.Model):
    """
    Таксист. Регистрируется один раз, потом создаёт объявления.
    """
    name = models.CharField(max_length=100, verbose_name='Имя')
    phone = models.CharField(max_length=20, unique=True, verbose_name='Телефон')
    card_number = models.CharField(
        max_length=20, blank=True, null=True,
        verbose_name='Номер карты (Humo/Uzcard)',
        help_text='Только цифры, например: 8600123412341234'
    )
    car_model = models.CharField(max_length=100, blank=True, verbose_name='Марка машины')
    car_color = models.CharField(max_length=50, blank=True, verbose_name='Цвет машины')
    car_plate = models.CharField(max_length=20, blank=True, verbose_name='Гос. номер')
    rating = models.DecimalField(
        max_digits=3, decimal_places=2,
        default=5.00, verbose_name='Рейтинг'
    )
    total_rides = models.PositiveIntegerField(default=0, verbose_name='Поездок всего')
    is_active = models.BooleanField(default=True, verbose_name='Активен')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name = 'Таксист'
        verbose_name_plural = 'Таксисты'
        ordering = ['-rating', '-total_rides']

    def __str__(self):
        return f'{self.name} ({self.phone})'


class RideOffer(models.Model):
    """
    Объявление таксиста — «Еду из A в B в такое-то время».
    """
    STATUS_CHOICES = [
        ('active', 'Активно'),
        ('done', 'Завершено'),
        ('cancelled', 'Отменено'),
    ]

    driver = models.ForeignKey(
        Driver, on_delete=models.CASCADE,
        related_name='offers', verbose_name='Таксист'
    )

    from_city = models.CharField(max_length=150, verbose_name='Откуда (город/район)')
    to_district = models.CharField(max_length=150, verbose_name='Куда (посёлок/район)')
    route_description = models.TextField(blank=True, verbose_name='Описание маршрута')
    departure_time = models.DateTimeField(verbose_name='Время отправления')
    
    # Общая вместимость (сколько всего мест в машине)
    total_seats = models.PositiveSmallIntegerField(default=4, verbose_name='Всего мест')
    price_per_seat = models.PositiveIntegerField(verbose_name='Цена за место (сум)')

    status = models.CharField(
        max_length=20, choices=STATUS_CHOICES,
        default='active', verbose_name='Статус'
    )

    created_at = models.DateTimeField(auto_now_add=True, verbose_name='Создано')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='Обновлено')

    class Meta:
        verbose_name = 'Объявление о поездке'
        verbose_name_plural = 'Объявления о поездках'
        ordering = ['departure_time']

    def __str__(self):
        return f'{self.from_city} → {self.to_district} | {self.departure_time:%d.%m %H:%M}'

    @property
    def confirmed_seats(self):
        """Считаем только подтвержденные и пришедшие бронирования"""
        return self.bookings.filter(status__in=['confirmed', 'checked_in']).aggregate(
            models.Sum('seats_requested')
        )['seats_requested__sum'] or 0

    @property
    def seats_available(self):
        """Оставшиеся свободные места"""
        return max(0, self.total_seats - self.confirmed_seats)

    @property
    def is_available(self):
        """Доступна ли поездка для ПОИСКА и новых заявок"""
        return self.status == 'active' and self.seats_available > 0 and self.departure_time > timezone.now()


class Booking(models.Model):
    """
    Заявка пассажира. Теперь с полным жизненным циклом.
    """
    STATUS_CHOICES = [
        ('pending', 'Ожидает подтверждения'),
        ('confirmed', 'Подтверждено (Место занято)'),
        ('rejected', 'Отклонено водителем'),
        ('cancelled', 'Отменено пассажиром'),
        ('checked_in', 'Пассажир сел в машину'),
        ('no_show', 'Пассажир не пришел (Место освободилось)'),
    ]

    offer = models.ForeignKey(
        RideOffer, on_delete=models.CASCADE,
        related_name='bookings', verbose_name='Объявление'
    )
    passenger_name = models.CharField(max_length=100, verbose_name='Имя пассажира')
    passenger_phone = models.CharField(max_length=20, verbose_name='Телефон пассажира')
    seats_requested = models.PositiveSmallIntegerField(
        default=1, verbose_name='Запрошено мест'
    )
    status = models.CharField(
        max_length=20, choices=STATUS_CHOICES,
        default='pending', verbose_name='Статус'
    )
    note = models.TextField(blank=True, verbose_name='Комментарий')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        verbose_name = 'Бронирование'
        verbose_name_plural = 'Бронирования'
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.passenger_name} → {self.offer}'


class TelegramAuthSession(models.Model):
    STATUS_CHOICES = [
        ('pending', 'Ожидает'),
        ('code_sent', 'Код отправлен'),
        ('verified', 'Проверен'),
        ('expired', 'Истёк'),
    ]
    session_id = models.UUIDField(default=uuid.uuid4, unique=True, editable=False)
    phone = models.CharField(max_length=20)
    telegram_user_id = models.BigIntegerField(null=True, blank=True)
    code = models.CharField(max_length=6)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()

    class Meta:
        ordering = ['-created_at']

    @property
    def is_valid(self):
        return self.status == 'code_sent' and timezone.now() <= self.expires_at
