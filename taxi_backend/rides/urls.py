"""
rides/urls.py — Все маршруты приложения
"""
from django.urls import path
from . import views

urlpatterns = [
    # Корень API
    path('', views.api_root, name='api-root'),
    path('health/', views.health_check, name='health-check'),

    # Таксисты
    path('drivers/', views.DriverListView.as_view(), name='driver-list'),
    path('drivers/register/', views.DriverRegisterView.as_view(), name='driver-register'),
    path('drivers/<int:pk>/', views.DriverDetailView.as_view(), name='driver-detail'),

    # Объявления
    path('rides/', views.RideOfferListView.as_view(), name='ride-list'),
    path('rides/create/', views.RideOfferCreateView.as_view(), name='ride-create'),
    path('rides/<int:pk>/', views.RideOfferDetailView.as_view(), name='ride-detail'),
    path('rides/<int:pk>/status/', views.ride_offer_status, name='ride-status'),

    # Бронирования
    path('bookings/', views.BookingListView.as_view(), name='booking-list'),
    path('bookings/create/', views.BookingCreateView.as_view(), name='booking-create'),
    path('bookings/<int:pk>/status/', views.booking_status_update, name='booking-status'),
    path('auth/telegram/init/', views.telegram_auth_init),
    path('auth/telegram/verify/', views.telegram_auth_verify),
]