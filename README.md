# 필름로그 (Spring Boot)

TMDB 데이터를 서버가 대신 호출하는 영화 평점 사이트입니다.

## 실행
- 필요: JDK 17+, Maven 3.9+
- `mvn spring-boot:run` 후 http://localhost:8080 접속
- API 키는 `application.yml`의 기본값을 쓰며, 환경변수 `TMDB_API_KEY`로 덮어쓸 수 있습니다.
  - 예) `TMDB_API_KEY=내키 mvn spring-boot:run`
- 별점은 `./data/filmlog` (H2 파일 DB)에 저장됩니다.

## API
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | /api/movies?mode=popular&page=1 | 목록 (popular, top_rated, now_playing, upcoming) |
| GET | /api/movies/search?query=&page= | 제목 검색 |
| GET | /api/movies/{id} | 상세 + 출연진 |
| GET | /api/genres | 장르 목록 |
| GET | /api/ratings/mine | 내 별점 전체 |
| PUT | /api/ratings/{id} | 별점 등록/수정 (body: {"score":1~5}) |
| DELETE | /api/ratings/{id} | 별점 취소 |
| GET | /api/ratings/{id}/summary | 사이트 평균 별점·참여 인원 |

## 구조
- `tmdb/TmdbClient` TMDB 호출 (키는 서버에만 존재)
- `movie/MovieController` 목록·검색·상세 프록시
- `rating/*` 쿠키(uid) 기반 익명 별점 저장
- `static/index.html` 프런트엔드

## 내 주변 영화관
- 화면의 `내 주변 영화관` 버튼 → 브라우저 위치 권한 허용 → 반경 5km 이내 영화관을 가까운 순으로 표시합니다.
- 체인 극장(CGV·롯데시네마·메가박스)은 `예매` 링크를, 모든 극장은 카카오맵 `지도`·`길찾기` 링크를 제공합니다.
- `GET /api/theaters?lat=&lng=&radius=5000` (radius 500~20000m). 좌표는 저장하지 않고 10분간 결과만 캐시합니다.
- 검색 소스: 환경변수 `KAKAO_REST_API_KEY`가 있으면 카카오 로컬 API(권장, 정확도 높음), 없으면 OpenStreetMap Overpass(키 불필요, 일부 극장이 누락될 수 있음).
- 브라우저 위치 기능은 `localhost` 또는 HTTPS 환경에서만 동작합니다.

## 주소로 영화관 찾기
- 내 주변 영화관 창의 입력칸에 도로명·지번 주소 또는 장소명(예: `강남역`)을 넣으면 그 위치 기준으로 영화관을 찾습니다.
- `GET /api/geocode?query=` → 카카오 로컬 `v2/local/search/address.json`으로 좌표 변환, 결과가 없으면 `keyword.json`으로 장소명 검색 (최대 5건)
- **환경변수 `KAKAO_REST_API_KEY`가 필수**입니다. 없으면 주소 검색만 안내 메시지를 보여 주고, `내 위치로 찾기`는 그대로 동작합니다.
