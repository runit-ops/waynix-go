"""
rides/views.py — Все API-эндпоинты
"""
from rest_framework import generics, filters, status
from rest_framework.decorators import api_view
from rest_framework.response import Response
from django.utils import timezone
from django.db.models import Q
import logging

logger = logging.getLogger(__name__)

from .models import Driver, RideOffer, Booking
from .serializers import (
    DriverSerializer, DriverCreateSerializer,
    RideOfferSerializer, RideOfferCreateSerializer,
    BookingSerializer,
)


# ═══════════════════════════════════════════════════════════
#  СЛУЖЕБНЫЙ
# ═══════════════════════════════════════════════════════════

@api_view(['GET'])
def api_root(request):
    """
    GET /api/
    Возвращает список всех доступных эндпоинтов.
    """
    return Response({
        'status': 'ok',
        'message': 'Добро пожаловать в Taxi API 🚕',
        'endpoints': {
            'Проверка связи (health)': '/api/health/',
            'Объявления (список + поиск)': '/api/rides/',
            'Создать объявление': '/api/rides/create/',
            'Детали объявления': '/api/rides/<id>/',
            'Изменить статус объявления': '/api/rides/<id>/status/',
            'Зарегистрировать таксиста': '/api/drivers/register/',
            'Список таксистов': '/api/drivers/',
            'Профиль таксиста': '/api/drivers/<id>/',
            'Создать бронирование': '/api/bookings/create/',
            'Мои бронирования (по телефону)': '/api/bookings/?phone=998XXXXXXXXX',
        }
    })


@api_view(['GET'])
def health_check(request):
    """
    GET /api/health/
    Простой эндпоинт для проверки доступности сервера.
    """
    return Response({
        'status': 'ok',
        'timestamp': timezone.now(),
        'message': 'Сервер WaynixGO активен'
    })


# ═══════════════════════════════════════════════════════════
#  ТАКСИСТЫ
# ═══════════════════════════════════════════════════════════

class DriverRegisterView(generics.CreateAPIView):
    """
    POST /api/drivers/register/
    Регистрация или получение существующего таксиста по номеру телефона.

    Тело запроса (JSON):
    {
        "name": "Алибек",
        "phone": "+998901234567",
        "car_model": "Cobalt",
        "car_color": "Белый",
        "car_plate": "01 A 123 BA"
    }
    """
    serializer_class = DriverCreateSerializer

    def create(self, request, *args, **kwargs):
        phone = request.data.get('phone')
        if not phone:
            return Response({'phone': 'Обязательное поле'}, status=status.HTTP_400_BAD_REQUEST)

        # Пробуем найти существующего
        driver = Driver.objects.filter(phone=phone).first()
        if driver:
            logger.info(f"Существующий таксист найден: {phone}")
            return Response(
                {
                    'message': 'Таксист уже зарегистрирован',
                    'driver': DriverSerializer(driver).data,
                },
                status=status.HTTP_200_OK
            )

        # Если нет — создаём нового
        logger.info(f"Регистрация нового таксиста: {phone}")
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        driver = serializer.save()
        return Response(
            {
                'message': 'Таксист успешно зарегистрирован!',
                'driver': DriverSerializer(driver).data,
            },
            status=status.HTTP_201_CREATED
        )


class DriverListView(generics.ListAPIView):
    """
    GET /api/drivers/
    Список всех активных таксистов, отсортированных по рейтингу.
    """
    queryset = Driver.objects.filter(is_active=True)
    serializer_class = DriverSerializer


class DriverDetailView(generics.RetrieveAPIView):
    """
    GET /api/drivers/<id>/
    Профиль конкретного таксиста + его активные объявления.
    """
    queryset = Driver.objects.filter(is_active=True)
    serializer_class = DriverSerializer

    def retrieve(self, request, *args, **kwargs):
        driver = self.get_object()
        driver_data = DriverSerializer(driver).data
        # Добавляем его текущие активные объявления
        active_offers = RideOffer.objects.filter(
            driver=driver,
            status='active',
            departure_time__gt=timezone.now()
        )
        driver_data['active_offers'] = RideOfferSerializer(active_offers, many=True).data
        return Response(driver_data)


# ═══════════════════════════════════════════════════════════
#  ОБЪЯВЛЕНИЯ О ПОЕЗДКАХ
# ═══════════════════════════════════════════════════════════

class RideOfferListView(generics.ListAPIView):
    """
    GET /api/rides/
    Список активных объявлений.
    """
    serializer_class = RideOfferSerializer
    filter_backends = [filters.SearchFilter, filters.OrderingFilter]
    search_fields = ['from_city', 'to_district', 'route_description', 'driver__name']
    ordering_fields = ['departure_time', 'price_per_seat', 'created_at']
    ordering = ['departure_time']

    def get_queryset(self):
        driver_id = self.request.query_params.get('driver_id')
        
        # Если это водитель запрашивает свои — показываем ВСЁ (даже неактивные)
        if driver_id:
            return RideOffer.objects.filter(driver_id=driver_id).order_by('-created_at')

        # Иначе — обычный поиск для пассажиров (только активные и с местами)
        qs = RideOffer.objects.filter(
            status='active',
            departure_time__gt=timezone.now()
        ).select_related('driver')

        to_param = self.request.query_params.get('to')
        from_param = self.request.query_params.get('from')

        if to_param:
            qs = qs.filter(to_district__icontains=to_param)
        if from_param:
            qs = qs.filter(from_city__icontains=from_param)

        # Фильтрация: Показываем только те, где реально есть подтвержденные места
        from django.db.models import Sum, F, fields
        from django.db.models.functions import Coalesce

        qs = qs.annotate(
            confirmed_seats_count=Coalesce(
                Sum('bookings__seats_requested', filter=Q(bookings__status__in=['confirmed', 'checked_in'])),
                0,
                output_field=fields.IntegerField()
            )
        ).filter(confirmed_seats_count__lt=F('total_seats'))

        return qs


@api_view(['PATCH'])
def booking_status_update(request, pk):
    """
    PATCH /api/bookings/<id>/status/
    Водитель меняет статус бронирования (подтверждает, отмечает неявку и т.д.)
    """
    try:
        booking = Booking.objects.get(pk=pk)
    except Booking.DoesNotExist:
        return Response({'error': 'Бронирование не найдено'}, status=status.HTTP_404_NOT_FOUND)

    driver_id = request.data.get('driver_id')
    new_status = request.data.get('status')
    
    if not driver_id or not new_status:
        return Response({'error': 'driver_id и status обязательны'}, status=status.HTTP_400_BAD_REQUEST)

    if str(booking.offer.driver.id) != str(driver_id):
        return Response({'error': 'Это не ваша поездка'}, status=status.HTTP_403_FORBIDDEN)

    allowed_statuses = [choice[0] for choice in Booking.STATUS_CHOICES]
    if new_status not in allowed_statuses:
        return Response({'error': 'Недопустимый статус'}, status=status.HTTP_400_BAD_REQUEST)

    if new_status == 'confirmed':
        available = booking.offer.seats_available
        if booking.seats_requested > available:
            return Response({'error': f'Недостаточно мест. Доступно: {available}'}, status=status.HTTP_400_BAD_REQUEST)

    booking.status = new_status
    booking.save()
    
    return Response({
        'message': f'Статус брони изменен на {new_status}',
        'booking': {
            'id': booking.id,
            'status': booking.status,
            'seats_available_now': booking.offer.seats_available
        }
    })


class RideOfferCreateView(generics.CreateAPIView):
    """
    POST /api/rides/create/
    Таксист публикует новое объявление.

    Тело запроса (JSON):
    {
        "driver_id": 1,
        "from_city": "Ташкент",
        "to_district": "Сергели (массив Куйлюк)",
        "route_description": "Заеду в Юнусабад по пути",
        "departure_time": "2025-06-15T18:00:00",
        "price_per_seat": 15000,
        "seats_available": 3
    }
    """
    serializer_class = RideOfferCreateSerializer

    def create(self, request, *args, **kwargs):
        logger.info(f"Создание объявления таксистом ID={request.data.get('driver_id')}")
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        offer = serializer.save()
        return Response(
            {
                'message': 'Объявление успешно опубликовано!',
                'offer': RideOfferSerializer(offer).data,
            },
            status=status.HTTP_201_CREATED
        )


class RideOfferDetailView(generics.RetrieveAPIView):
    """
    GET /api/rides/<id>/
    Детальная карточка объявления.
    """
    queryset = RideOffer.objects.all().select_related('driver')
    serializer_class = RideOfferSerializer


@api_view(['PATCH'])
def ride_offer_status(request, pk):
    """
    PATCH /api/rides/<id>/status/
    Таксист меняет статус своего объявления.

    Тело запроса:
    {
        "driver_id": 1,
        "status": "cancelled"   // или "done"
    }
    """
    try:
        offer = RideOffer.objects.get(pk=pk)
    except RideOffer.DoesNotExist:
        return Response({'error': 'Объявление не найдено.'}, status=status.HTTP_404_NOT_FOUND)

    driver_id = request.data.get('driver_id')
    new_status = request.data.get('status')

    if not driver_id or not new_status:
        return Response({'error': 'driver_id и status обязательны.'}, status=status.HTTP_400_BAD_REQUEST)

    if str(offer.driver.id) != str(driver_id):
        return Response({'error': 'Нельзя менять чужое объявление.'}, status=status.HTTP_403_FORBIDDEN)

    allowed = ['active', 'cancelled', 'done', 'booked']
    if new_status not in allowed:
        return Response({'error': f'Допустимые статусы: {allowed}'}, status=status.HTTP_400_BAD_REQUEST)

    if new_status == 'done' and offer.status != 'done':
        offer.driver.total_rides += 1
        offer.driver.save()

    offer.status = new_status
    offer.save()
    return Response({
        'message': f'Статус изменён на «{new_status}».',
        'offer': RideOfferSerializer(offer).data,
    })


# ═══════════════════════════════════════════════════════════
#  БРОНИРОВАНИЯ
# ═══════════════════════════════════════════════════════════

class BookingCreateView(generics.CreateAPIView):
    """
    POST /api/bookings/create/
    Пассажир оставляет заявку на поездку.

    Тело запроса (JSON):
    {
        "offer_id": 5,
        "passenger_name": "Санжар",
        "passenger_phone": "+998901112233",
        "seats_requested": 1,
        "note": "Буду у метро Мирзо Улугбек"
    }
    """
    serializer_class = BookingSerializer

    def create(self, request, *args, **kwargs):
        logger.info(f"Запрос бронирования: offer={request.data.get('offer_id')}, phone={request.data.get('passenger_phone')}")
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        booking = serializer.save()
        return Response(
            {
                'message': 'Заявка отправлена! Свяжитесь с таксистом для подтверждения.',
                'booking': BookingSerializer(booking).data,
                'driver_phone': booking.offer.driver.phone,
            },
            status=status.HTTP_201_CREATED
        )


class BookingListView(generics.ListAPIView):
    """
    GET /api/bookings/?phone=+998901234567
    Список бронирований конкретного пассажира (по номеру телефона).
    """
    serializer_class = BookingSerializer

    def get_queryset(self):
        phone = self.request.query_params.get('phone', '')
        if not phone:
            return Booking.objects.none()
        return Booking.objects.filter(
            passenger_phone__icontains=phone
        ).select_related('offer', 'offer__driver')
