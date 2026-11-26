package com.example.filmlog.theater;


public record Theater(String name, String brand, String address, String phone, int distance,
                      double lat, double lng, String bookingUrl, String mapUrl, String routeUrl) {}
