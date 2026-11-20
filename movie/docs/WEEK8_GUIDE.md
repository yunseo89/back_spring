# 백엔드 8주차: 실행과 Postman 촬영 순서

기준: `yunseo89/back_spring`의 `week-7` 커밋 `406ac51`. 수정 범위는 `movie/`입니다. 루트 README와 `study/`는 수정하지 않습니다. 필수미션과 실습 체크리스트를 구현하며, 선택미션인 Swagger는 포함하지 않습니다.

## 1. 어떤 코드가 추가됐나요?

| 코드 | 역할 |
| --- | --- |
| `entity/Favorite.java` | 로그인한 회원과 TMDB 영화 ID의 관계. `(member_id, movie_id)` UNIQUE |
| `entity/BaseTimeEntity.java` | 기존 DB의 생성/수정 시각 정책을 공통으로 매핑. Member/Rating/Favorite가 상속 |
| `repository/FavoriteRepository.java` | 회원별 최신순 목록, 회원+영화로 여부/삭제 대상 조회 |
| `dto/CreateFavoriteRequest.java` | 양수 movieId만 받음. memberId를 받는 필드 없음 |
| `dto/CreateMovieRecordRequest.java` | 양수 movieId, 정수 별점 1~5, 코멘트 500자, 필수 Boolean 검증 |
| `service/FavoriteService.java` | 즐겨찾기 등록/조회/해제와 중복 검사 |
| `service/MovieRecordService.java` | `@Transactional` 안에서 평점과 선택한 즐겨찾기를 함께 저장 |
| `controller/FavoriteController.java` | 즐겨찾기 API 4개. 회원은 `@RequestAttribute`로만 식별 |
| `controller/MovieRecordController.java` | 영화 기록 API 1개. 회원은 `@RequestAttribute`로만 식별 |
| `controller/GlobalExceptionHandler.java` | DB UNIQUE 충돌도 FAVORITE409_1 / RATING409_1로 응답. 잘못된 JSON/경로 값은 400 |
| `resources/application.yml` | 기존 spring.jwt 들여쓰기를 jwt로 바로잡아 JwtUtil의 설정 경로와 일치 |

`AuthInterceptor`와 보호 경로 설정은 그대로입니다. 새 URL도 기본적으로 인증이 필요합니다. `movieId`는 TMDB ID이며 우리 DB의 movie 테이블에 대한 외래 키가 아닙니다.

시간 컬럼은 기존 MySQL의 `DEFAULT CURRENT_TIMESTAMP`, `ON UPDATE CURRENT_TIMESTAMP`를 계속 사용합니다. `@Generated`는 저장 후 DB에서 생성한 시간을 읽어 응답의 createdAt가 비지 않도록 합니다. 별도 JPA Auditing 설정은 필요하지 않습니다.

## 2. 로컬 코드를 준비하세요

이 변경이 GitHub `week-8`에 업로드된 **이후에만** 다음 명령을 사용합니다. 현재 PC에서 변경 중인 코드가 있다면 먼저 `git status`로 확인하고 필요한 것을 커밋하거나 stash 하세요.

```powershell
git fetch origin
git switch --track origin/week-8
```

이미 로컬 week-8 브랜치가 있으면 `git switch week-8` 후 `git pull --ff-only`를 사용합니다. 위 명령은 IntelliJ 프로젝트의 Git 저장소 안에서 실행합니다.

## 3. MySQL 테이블을 확인하세요

1. MySQL Workbench에서 기존 로컬 연결을 여세요.
2. `movie/docs/sql/week8-favorite.sql`을 여세요.
3. 현재 사용하는 DB가 `umc_movie`인지 확인하고 SQL을 실행하세요.
4. `SHOW CREATE TABLE favorite` 결과에 다음이 있어야 합니다.
   - BIGINT favorite_id, member_id, movie_id
   - created_at의 기본값, updated_at의 기본값과 ON UPDATE
   - `(member_id, movie_id)` 복합 UNIQUE: `uk_favorite_member_movie`
   - member.member_id를 참조하는 외래 키
5. 이미 5주차 테이블이 있다면 `CREATE TABLE IF NOT EXISTS`는 그 구조를 고치지 않습니다. UNIQUE가 없거나 이름이 다를 때만 SQL 아래의 주석 예시를 참고해 보완하세요. 기존 행을 자동으로 삭제하지 않습니다.
6. rating의 복합 UNIQUE도 `uk_rating_member_movie`인지 확인하세요.

`movie_id` 하나만 UNIQUE로 만들면 다른 회원도 같은 영화를 등록할 수 없게 됩니다. 두 컬럼을 한 쌍으로 묶습니다. `member.member_id`가 UNSIGNED인 기존 DB라면 외래 키 컬럼의 자료형도 동일하게 맞춰야 합니다.

`ddl-auto: validate`를 유지합니다. 테이블이 없거나 컬럼 구조가 다르면 서버 시작 시 오류가 나므로, 해당 테이블부터 확인하세요. 개인 DB를 지우거나 create/create-drop으로 바꾸지 마세요.

## 4. IntelliJ에서 서버를 실행하세요

1. `movie` Gradle 프로젝트를 열고 변경된 Gradle 설정을 동기화합니다.
2. Project SDK와 Gradle JVM을 **JDK 21**로 설정합니다.
3. 실행 구성은 `com.umc.movie.MovieApplication`입니다.
4. 환경 변수에 기존의 `DB_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION`을 설정합니다. 비밀값을 코드나 Git에 적지 않습니다. 만료 시간은 실습용으로 `3600`초를 쓸 수 있습니다.
5. 실행 후 `Started MovieApplication`과 포트 8080 로그를 확인합니다.

기존 7주차 로컬 환경에 JWT 설정을 따로 수정해 두었다면 새 application.yml에서 `jwt:`가 `spring:`과 같은 들여쓰기인지 확인하세요. 서버를 두 번 실행해 포트 충돌이 나지 않도록 합니다.

## 5. Postman을 준비하세요

1. Postman → **Import** → `movie/postman/week8.postman_collection.json`을 선택합니다.
2. 가져온 컬렉션을 열고 **Variables** 탭의 `baseUrl`이 `http://localhost:8080`인지 확인합니다. `/v8`은 붙이지 않습니다.
3. 첫 두 요청이 서로 다른 새 실습 계정 A/B를 만듭니다. 이메일·닉네임·비밀번호는 비어 있으면 실습용 값으로 자동 생성됩니다. 기존 계정에는 영향을 주지 않습니다.
4. 03/04 로그인 요청은 응답의 `result.accessToken`을 컬렉션 변수에 자동 저장합니다. 이후 요청에는 Bearer 토큰이 자동으로 붙습니다.
5. **01부터 30까지 번호 순서대로 Send**하세요. 응답의 **Test Results**도 확인합니다. Collection Runner로 전체를 한 번에 실행할 수도 있지만, 제출 캡처는 각 요청에서 찍는 편이 쉽습니다.
6. 01/02 가입이 끝난 같은 계정으로 다시 전체를 실행하면 가입·등록의 중복 오류가 정상적으로 발생합니다. 처음부터 재실행하려면 emailA/B, nicknameA/B, passwordA/B, accessTokenA/B 값을 비우고 새 실습 계정으로 시작하세요.

토큰·비밀번호가 저장된 실행 후 컬렉션이나 Postman 환경 파일을 Git에 올리지 마세요. 제출 화면은 URL·요청 JSON·응답 JSON·HTTP 상태가 보이게 촬영하고, Authorization 값과 로그인 요청의 비밀번호를 가립니다.

## 6. 즐겨찾기 실습: 무엇이 나와야 하나요?

새 실습 계정 A로 아래 순서를 따릅니다. 컬렉션의 변수 movieFavorite 기본값은 `550`입니다.

| 요청 번호 | Method / URL | Body | 기대 결과와 촬영할 부분 |
| --- | --- | --- | --- |
| 05 | GET /favorites | 없음 | 200, `result: []` |
| 06 | GET /favorites/550 | 없음 | 200, `result.isFavorite: false` (등록 전) |
| 07 | POST /favorites | `{"movieId":550}` | 201 COMMON201. favoriteId, movieId, createdAt |
| 08 | POST /favorites | 위와 동일 | 409 FAVORITE409_1. 중복 등록 증거 |
| 09 | GET /favorites/550 | 없음 | 200, isFavorite: true (등록 후) |
| 10 | GET /favorites | 없음 | 내 즐겨찾기 목록. movieId 550 한 건 |
| 11 | GET /favorites (B 토큰) | 없음 | 200, 빈 목록. A의 데이터는 보이지 않음 |
| 12 | DELETE /favorites/550 (B 토큰) | 없음 | 404 FAVORITE404_1. A의 즐겨찾기는 유지 |
| 13 | DELETE /favorites/550 (A 토큰) | 없음 | 200 COMMON200, result: null |
| 14 | GET /favorites/550 | 없음 | 200, isFavorite: false (해제 후) |
| 15 | DELETE /favorites/550 | 없음 | 404 FAVORITE404_1 |

Body를 직접 입력할 때는 **Body → raw → JSON**을 고르세요. movieId는 따옴표 없이 숫자로 적습니다. 신규 API의 URL/쿼리/Body에 memberId를 넣지 않습니다.

## 7. 영화 기록 실습: 두 데이터가 함께 저장되나요?

**16번** 요청입니다.

```http
POST http://localhost:8080/movie-records
Authorization: Bearer {{accessTokenA}}
Content-Type: application/json
```

```json
{
  "movieId": 238,
  "score": 5,
  "comment": "다시 보고 싶은 영화입니다.",
  "addToFavorites": true
}
```

201 COMMON201이며 `result.rating`과 `result.favorite`가 모두 객체이고 movieId가 238이어야 합니다. 두 객체가 함께 보이는 응답을 캡처하세요.

- **17번:** 같은 요청을 다시 보내면 409 RATING409_1. 행이 추가되지 않아야 합니다.
- **18번:** movieId 680, addToFavorites false로 저장하면 201이며 favorite는 null입니다. false는 기존 즐겨찾기를 해제한다는 뜻이 아니라, 이번 요청에서 즐겨찾기를 추가하지 않는다는 뜻입니다.
- **19번:** 680의 즐겨찾기 여부는 false입니다.
- **20→21번:** 550을 다시 즐겨찾기한 뒤 목록은 최신 등록한 550, 먼저 저장한 238 순서입니다. 정렬 증거로 캡처하세요.
- **22번:** 즐겨찾기만 있는 550에 영화 기록 true를 요청하면 409 FAVORITE409_1이며 550의 새 평점은 생기지 않습니다.

MySQL Workbench에서 아래 SELECT로 확인할 수도 있습니다. 이메일은 Postman Variables에 생성된 emailA 값으로 바꿉니다. 이 SQL의 memberId 조회는 **DB 증거 확인용**이며 신규 API 요청에 memberId를 보내는 것이 아닙니다.

```sql
USE umc_movie;
SET @week8_member_id = (SELECT member_id FROM member WHERE email = 'Postman의 emailA 값');
SELECT rating_id, member_id, movie_id, score FROM rating
WHERE member_id = @week8_member_id ORDER BY rating_id DESC;
SELECT favorite_id, member_id, movie_id, created_at FROM favorite
WHERE member_id = @week8_member_id ORDER BY favorite_id DESC;
```

01~30을 한 번 실행한 뒤 A에게 남는 데이터는 평점 238·680, 즐겨찾기 238·550입니다. 550 평점은 없어야 합니다.

**중요:** 17/22번은 사전 중복 검사에서 거절되는 흐름입니다. 이것만으로 “평점 INSERT 후 실제 rollback을 재현했다”고 적지 않습니다. 실제 중간 실패의 롤백은 아래 자동 테스트로 별도 검증합니다.

## 8. 실패 상황 확인

| 요청 | 기대 결과 |
| --- | --- |
| 23~27: 신규 API 5개를 No Auth로 호출 | 401 AUTH401_2 |
| 28: movieId 0 | 400 COMMON4001 |
| 29: score 6 | 400 COMMON4001 |
| 30: addToFavorites 누락 | 400 COMMON4001 |

08의 중복 409, 15의 없는 즐겨찾기 404, 06의 정상 false, 05의 정상 빈 목록도 함께 기록합니다. API마다 직접 토큰을 설정하려면 Authorization → Bearer Token에서 `{{accessTokenA}}`를 사용하세요. 401 증거를 찍을 때는 **No Auth**를 선택합니다.

## 9. 자동 테스트로 실제 rollback 확인

`movie/gradlew.bat`가 있는 폴더의 PowerShell에서 실행합니다.

```powershell
.\gradlew.bat test bootJar
```

기본 테스트는 별도 H2 메모리 DB로 실행됩니다. 개인 MySQL, DB_PASSWORD, 실제 JWT 비밀값 없이 동작합니다. 보고서는 `movie/build/reports/tests/test/index.html`입니다.

`Week8IntegrationTest.databaseUniqueFailureRollsBackAlreadyInsertedRating`는 다음을 검증합니다.

1. 기존 즐겨찾기 238 한 건을 준비합니다.
2. 동시 요청이 사전 검사와 INSERT 사이에 끼어든 상황을 재현하도록 **사전 조회 결과만** false로 제어합니다.
3. 실제 Spring 서비스 프록시의 트랜잭션에서 rating INSERT를 실행합니다.
4. favorite INSERT가 DB의 UNIQUE 제약으로 실패합니다.
5. HTTP 응답은 409 FAVORITE409_1이며 DB에는 새 rating이 0건, 기존 favorite만 1건이어야 합니다.

DB 저장/rollback을 Mockito로 대체하지 않습니다. 다만 두 클라이언트를 실제 동시에 실행하는 부하 테스트는 아닙니다. `saveAndFlush()`는 SQL을 DB로 보내는 것이며 commit은 아닙니다. 두 저장의 commit은 바깥 `@Transactional` 메서드가 정상 종료될 때 함께 이루어집니다. 자동증가 ID에 빈 번호가 생겨도 rollback 실패라는 뜻은 아닙니다.

MySQL로 같은 테스트를 하고 싶다면 **개인 실습 DB와 별개인** `umc_movie_week8_test`를 먼저 만들고, 아래를 설정해 실행합니다. 테스트는 전용 DB의 member/rating/favorite 행을 삭제하므로 일반 umc_movie를 지정하면 안 됩니다. 테스트 코드도 허용된 DB 이름을 확인한 뒤 정리합니다.

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://localhost:3306/umc_movie_week8_test'
$env:TEST_DB_USERNAME = 'root'
# 뜨는 입력 창에 테스트 DB의 root 비밀번호를 입력합니다. 명령 기록에 값이 남지 않습니다.
$week8Credential = Get-Credential -UserName 'root' -Message '테스트 DB 비밀번호'
$env:TEST_DB_PASSWORD = $week8Credential.GetNetworkCredential().Password
.\gradlew.bat test --rerun-tasks
Remove-Item Env:TEST_DB_URL, Env:TEST_DB_USERNAME, Env:TEST_DB_PASSWORD -ErrorAction SilentlyContinue
```

실습 DB 생성 권한이 있는 계정으로 `CREATE DATABASE umc_movie_week8_test;`를 한 번 실행하면 됩니다. 테스트용 테이블은 테스트 시작 때 생성됩니다.

## 10. 워크북에 넣을 문장과 캡처

- 실습 인증: 07 성공, 08 중복, 21 최신순 목록, 06/09 여부 변화, 13 해제, 16 두 객체 동시 응답.
- 회원 분리 추가 증거: 10의 A 목록과 11의 B 빈 목록, 12의 B 해제 실패.
- 트랜잭션 코드: `MovieRecordService.create()`의 `@Transactional`과 두 `saveAndFlush()`가 한 화면에 보이도록 캡처.
- 한 문장 설명: “평점과 즐겨찾기는 하나의 영화 기록이므로, 두 저장 중 하나가 실패하면 먼저 저장한 데이터도 취소하도록 하나의 트랜잭션으로 묶었다.”
- 필수미션 코드 링크: 업로드 후 `week-8`의 Favorite 엔티티/Repository/Service/Controller 및 MovieRecordService 링크를 넣습니다. 공개하지 않은 코드의 링크를 먼저 제출하지 않습니다.
- 테스트 증거: 결과 보고서와 `databaseUniqueFailureRollsBackAlreadyInsertedRating` 통과 화면. 본인 Postman 촬영과 자동 테스트 결과를 구분해 적습니다.

세부 학습정리와 기록 템플릿은 `WEEK8_WORKBOOK.md`를 사용하세요. 체크리스트의 “확인했다” 항목은 PC에서 직접 확인한 다음 체크합니다.
