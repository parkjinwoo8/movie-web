package com.example.filmlog.geocode;

/** 주소 검색 결과 한 건. lat=위도(y), lng=경도(x). */
public record Place(String label, double lat, double lng, String type) {}
