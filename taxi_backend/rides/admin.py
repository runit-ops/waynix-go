"""
rides/admin.py — Настройка Django Admin
"""
from django.contrib import admin
from .models import Driver, RideOffer, Booking, TelegramAuthSession


@admin.register(Driver)
class DriverAdmin(admin.ModelAdmin):
    list_display = ['name', 'phone', 'card_number', 'car_model', 'rating', 'total_rides', 'is_active']
    list_filter = ['is_active']
    search_fields = ['name', 'phone', 'card_number']
    list_editable = ['is_active']


@admin.register(RideOffer)
class RideOfferAdmin(admin.ModelAdmin):
    list_display = ['driver', 'from_city', 'to_district', 'departure_time', 'price_per_seat', 'seats_available', 'status']
    list_filter = ['status', 'from_city']
    search_fields = ['from_city', 'to_district', 'driver__name']
    list_editable = ['status']
    ordering = ['departure_time']


@admin.register(Booking)
class BookingAdmin(admin.ModelAdmin):
    list_display = ['passenger_name', 'passenger_phone', 'offer', 'seats_requested', 'status', 'created_at']
    list_filter = ['status']
    search_fields = ['passenger_name', 'passenger_phone']

@admin.register(TelegramAuthSession)
class TelegramAuthSessionAdmin(admin.ModelAdmin):
    list_display = ['session_id', 'phone', 'code', 'status', 'created_at']
    list_filter = ['status']
