package com.example.weatherapp.responseItem

data class DayItem(
    val current: Current,
    val forecast: Forecast,
    val location: Location
)