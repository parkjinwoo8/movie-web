package com.example.filmlog.theater;

/** 화면에 보여줄 영화관 한 곳. bookingUrl은 체인(CGV·롯데시네마·메가박스)일 때만 채워집니다. */
public record Theater(String name, String brand, String address, String phone, int distance,
                      double lat, double lng, String bookingUrl, String mapUrl, String routeUrl) {}
