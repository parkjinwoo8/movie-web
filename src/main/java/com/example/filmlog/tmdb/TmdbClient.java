package com.example.filmlog.tmdb;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/* TMDB API 가져오기 */
@Component
public class TmdbClient {

    private final RestClient client;
    private final String apiKey;

    public TmdbClient(RestClient.Builder builder,
                      @Value("${tmdb.api-key}") String apiKey,
                      @Value("${tmdb.base-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    /* TMDB 응답 JSON을 문자열로 */
    public String get(String path, Map<String, String> params) {
        try {
            return client.get().uri(b -> {
                Map<String, Object> vars = new HashMap<>();
                vars.put("api_key", apiKey);
                b.path(path).queryParam("api_key", "{api_key}").queryParam("language", "ko-KR");
                params.forEach((k, v) -> {
                    b.queryParam(k, "{" + k + "}");   
                    vars.put(k, v);
                });
                return b.build(vars);
            }).retrieve().body(String.class);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영화를 찾을 수 없습니다.");
            }
            String msg = code == 401 ? "TMDB API 키가 올바르지 않습니다." : "TMDB 요청에 실패했습니다. (" + code + ")";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TMDB에 연결할 수 없습니다.");
        }
    }
}
