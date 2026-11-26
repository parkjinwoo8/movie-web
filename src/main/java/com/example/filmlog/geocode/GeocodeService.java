package com.example.filmlog.geocode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class GeocodeService {

    private static final int SIZE = 5;

    private final RestClient kakao;
    private final ObjectMapper mapper;
    private final String key;

    public GeocodeService(RestClient.Builder builder, ObjectMapper mapper,
                          @Value("${kakao.rest-api-key:}") String key) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.kakao = builder.requestFactory(factory).baseUrl("https://dapi.kakao.com").build();
        this.mapper = mapper;
        this.key = key == null ? "" : key.trim();
    }

    public List<Place> search(String query) {
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "주소 검색에는 카카오 REST API 키(KAKAO_REST_API_KEY)가 필요합니다. 대신 '내 위치로 찾기'를 이용해 주세요.");
        }
        try {
            List<Place> places = address(query);
            return places.isEmpty() ? keyword(query) : places;
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            String msg = (code == 401 || code == 403)
                    ? "카카오 REST API 키가 올바르지 않거나 사용 설정이 꺼져 있습니다."
                    : "주소 검색에 실패했습니다. (" + code + ")";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "주소 검색에 실패했습니다.");
        }
    }

    private List<Place> address(String query) throws Exception {
        List<Place> out = new ArrayList<>();
        for (JsonNode d : mapper.readTree(get("/v2/local/search/address.json", query)).path("documents")) {
            out.add(new Place(d.path("address_name").asText(),
                    d.path("y").asDouble(), d.path("x").asDouble(), d.path("address_type").asText()));
        }
        return out;
    }

    private List<Place> keyword(String query) throws Exception {
        List<Place> out = new ArrayList<>();
        for (JsonNode d : mapper.readTree(get("/v2/local/search/keyword.json", query)).path("documents")) {
            String addr = d.path("address_name").asText();
            out.add(new Place(d.path("place_name").asText() + (addr.isBlank() ? "" : " (" + addr + ")"),
                    d.path("y").asDouble(), d.path("x").asDouble(), "PLACE"));
        }
        return out;
    }

    private String get(String path, String query) {
        return kakao.get().uri(u -> u.path(path)
                        .queryParam("query", "{q}").queryParam("size", SIZE)
                        .build(Map.of("q", query)))
                .header("Authorization", "KakaoAK " + key)
                .retrieve().body(String.class);
    }
}
