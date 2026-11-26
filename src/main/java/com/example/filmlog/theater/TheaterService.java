package com.example.filmlog.theater;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*좌표 주변 영화관을 찾기 
카카오 로컬 API 사용(링크 복사 -> 넣기)
 */
@Service
public class TheaterService {

    private static final long TTL_MS = 10 * 60 * 1000L;
    private static final int LIMIT = 15;
    private static final Map<String, String> BOOKING = Map.of(
            "CGV", "https://www.cgv.co.kr/ticket/",
            "롯데시네마", "https://www.lottecinema.co.kr/NLCHS/Ticketing",
            "메가박스", "https://www.megabox.co.kr/booking");

    private record Cached(List<Theater> list, long at) {}

    private final RestClient kakao;
    private final RestClient overpass;
    private final String kakaoKey;
    private final ObjectMapper mapper;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public TheaterService(RestClient.Builder builder, ObjectMapper mapper,
                          @Value("${kakao.rest-api-key:}") String kakaoKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        RestClient.Builder base = builder.requestFactory(factory);
        this.kakao = base.clone().baseUrl("https://dapi.kakao.com").build();
        this.overpass = base.clone().baseUrl("https://overpass-api.de").build();
        this.mapper = mapper;
        this.kakaoKey = kakaoKey == null ? "" : kakaoKey.trim();
    }

    public List<Theater> find(double lat, double lng, int radius) {
        // 좌표를 약 100m 단위로 맞춰 같은 동네 요청은 캐시를 재사용합니다.
        final double rlat = Math.round(lat * 1000) / 1000.0;
        final double rlng = Math.round(lng * 1000) / 1000.0;
        String key = String.format(Locale.ROOT, "%.3f,%.3f,%d", rlat, rlng, radius);
        Cached hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.at() < TTL_MS) {
            return hit.list();
        }
        List<Theater> list;
        try {
            list = kakaoKey.isEmpty() ? fromOverpass(rlat, rlng, radius) : fromKakao(rlat, rlng, radius);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "근처 영화관 정보를 가져오지 못했습니다.");
        }
        if (cache.size() > 500) {
            cache.clear();
        }
        cache.put(key, new Cached(list, System.currentTimeMillis()));
        return list;
    }

    private List<Theater> fromKakao(double lat, double lng, int radius) throws Exception {
        String body = kakao.get().uri(u -> u.path("/v2/local/search/keyword.json")
                        .queryParam("query", "{q}").queryParam("x", "{x}").queryParam("y", "{y}")
                        .queryParam("radius", radius).queryParam("sort", "distance").queryParam("size", LIMIT)
                        .build(Map.of("q", "영화관", "x", lng, "y", lat)))
                .header("Authorization", "KakaoAK " + kakaoKey)
                .retrieve().body(String.class);
        List<Theater> out = new ArrayList<>();
        for (JsonNode d : mapper.readTree(body).path("documents")) {
            String name = d.path("place_name").asText();
            if (!d.path("category_name").asText().contains("영화관") && detectBrand(name) == null) {
                continue;
            }
            String address = firstNonBlank(d.path("road_address_name").asText(), d.path("address_name").asText());
            out.add(build(name, name, address, d.path("phone").asText(),
                    d.path("distance").asInt(), d.path("y").asDouble(), d.path("x").asDouble()));
        }
        return out;
    }

    private List<Theater> fromOverpass(double lat, double lng, int radius) throws Exception {
        String query = String.format(Locale.ROOT,
                "[out:json][timeout:12];nwr[\"amenity\"=\"cinema\"](around:%d,%f,%f);out center tags 40;",
                radius, lat, lng);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);
        String body = overpass.post().uri("/api/interpreter")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(String.class);

        List<Theater> out = new ArrayList<>();
        for (JsonNode e : mapper.readTree(body).path("elements")) {
            JsonNode tags = e.path("tags");
            String name = firstNonBlank(tags.path("name:ko").asText(), tags.path("name").asText());
            JsonNode pos = e.has("lat") ? e : e.path("center");
            double la = pos.path("lat").asDouble();
            double lo = pos.path("lon").asDouble();
            if (name.isBlank() || (la == 0 && lo == 0)) {
                continue;
            }
            String address = firstNonBlank(tags.path("addr:full").asText(), String.join(" ",
                    nonBlank(tags.path("addr:city").asText(), tags.path("addr:district").asText(),
                            tags.path("addr:street").asText(), tags.path("addr:housenumber").asText())));
            String phone = firstNonBlank(tags.path("phone").asText(), tags.path("contact:phone").asText());
            String brandSource = name + " " + tags.path("brand").asText() + " " + tags.path("operator").asText();
            out.add(build(name, brandSource, address, phone, (int) Math.round(haversine(lat, lng, la, lo)), la, lo));
        }
        out.sort(Comparator.comparingInt(Theater::distance));
        return out.size() > LIMIT ? new ArrayList<>(out.subList(0, LIMIT)) : out;
    }

    private Theater build(String name, String brandSource, String address, String phone,
                          int distance, double lat, double lng) {
        String brand = detectBrand(brandSource);
        String pos = URLEncoder.encode(name.replace(",", " "), StandardCharsets.UTF_8).replace("+", "%20")
                + "," + lat + "," + lng;
        return new Theater(name, brand, address, phone, distance, lat, lng,
                brand == null ? null : BOOKING.get(brand),
                "https://map.kakao.com/link/map/" + pos,
                "https://map.kakao.com/link/to/" + pos);
    }

    private static String detectBrand(String text) {
        String s = text.toLowerCase(Locale.ROOT);
        if (s.contains("cgv")) return "CGV";
        if (s.contains("롯데시네마") || s.contains("lotte")) return "롯데시네마";
        if (s.contains("메가박스") || s.contains("megabox")) return "메가박스";
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : (b == null ? "" : b);
    }

    private static List<String> nonBlank(String... parts) {
        List<String> r = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) r.add(p);
        }
        return r;
    }

    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
