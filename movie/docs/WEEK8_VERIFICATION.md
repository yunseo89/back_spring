# 8주차 구현 검증 결과

검증일: 2026-11-20 (한국 시간). 기준: week-7 / 406ac51. 아래는 클라우드의 분리된 테스트 DB에서 수행한 결과이며 사용자의 로컬 PC 촬영이 아닙니다.

| 검증 | 환경 / 결과 |
| --- | --- |
| Java 컴파일 / 실행 JAR | JDK 21, Spring Boot 4.1.2, Gradle 9.8.2. test bootJar 성공 |
| 기본 자동 테스트 | H2 메모리 DB, 테스트 27개 성공 / 실패 0 |
| MySQL 자동 테스트 | MySQL Community Server 8.4.13, 테스트 27개 성공 / 실패 0 |
| 실행 JAR의 실제 HTTP 호출 | MySQL + 기본 application.yml, Postman 컬렉션을 Newman 6.2.3로 실행. 요청 30개 / 응답 검증 102개 성공 / 실패 0 |
| 제공 SQL | 기존 member/rating만 있는 별도 MySQL DB에서 week8-favorite.sql로 favorite 생성 성공 |
| 보존 범위 | 루트 README, study/, .idea/ 변경 없음 |

## 확인한 동작

- 로그인으로 발급받은 토큰으로 신규 API 접근.
- 신규 5개 API의 토큰 없는 호출 401 AUTH401_2, 잘못된 토큰 401 AUTH401_3.
- 즐겨찾기 등록 201, 목록/여부 200, 해제 200/result null, 해제할 대상 없음 404.
- 중복 등록 시 사전 검사와 DB UNIQUE 충돌 양쪽 모두 409 FAVORITE409_1.
- 회원별 목록 분리, 다른 회원의 즐겨찾기 해제 불가, 다른 회원의 같은 영화 등록 가능.
- 회원 ID를 Body/쿼리에 임의로 넣어도 저장 주인은 검증된 토큰의 회원.
- 최신 favoriteId 내림차순 목록.
- 등록 전/등록 후/삭제 후 isFavorite false/true/false.
- 양수 movieId, 별점 1~5, 필수 addToFavorites, 코멘트 500자 제한.
- addToFavorites true: 평점+즐겨찾기 함께 저장, false: 평점만 저장하고 기존 즐겨찾기 유지.
- DB 생성 시각이 등록 응답에 문자열로 포함됨.
- 영화 기록 중복 RATING409_1, 기존 즐겨찾기 중복 FAVORITE409_1 시 행 추가 없음.
- rating INSERT 이후 favorite INSERT의 실제 DB UNIQUE 오류 발생 시 rating도 rollback.

롤백 테스트는 사전 중복 조회 결과만 제어해 경쟁 상황을 재현했습니다. 두 INSERT, 제약 위반, 트랜잭션은 실제 Spring 프록시와 DB에서 수행합니다. 실제 동시 HTTP 부하 테스트를 수행한 결과는 아닙니다.

`week8-response-examples.json`에는 토큰/비밀번호 없이 검증용 실제 응답 예시를 저장했습니다. 개인 제출물은 `WEEK8_GUIDE.md`의 순서대로 직접 촬영하세요. Swagger 선택미션은 포함하지 않았습니다.
