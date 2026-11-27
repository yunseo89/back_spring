# 9주차 필수미션 · Swagger와 평점 테스트

IntelliJ에서 JDK 21로 `movie` 프로젝트를 열고 Gradle을 동기화한다. 기존 MySQL 설정과 환경 변수로 `MovieApplication`을 실행한 뒤 [Swagger UI](http://localhost:8080/swagger-ui.html)를 연다. JSON 명세는 `/v3/api-docs`에서 확인할 수 있다.

1. 공개 API인 `POST /members`로 가입하거나 기존 실습 계정을 사용한다.
2. `POST /auth/login` 응답에서 `result.accessToken`을 복사한다.
3. **Authorize**의 bearerAuth 입력란에 토큰 값만 붙여 넣는다.
4. **즐겨찾기 → POST /favorites → Try it out**에서 아직 등록하지 않은 TMDB 영화 ID(예: `{"movieId":550}`)를 보낸다. 첫 응답은 201 COMMON201, 동일 요청을 다시 보내면 409 FAVORITE409_1이다. 캡처에는 응답 영역만 담는다.

## 워크북 제출 자료

- [Swagger 전체 목록](01-swagger.png) · [즐겨찾기 등록 명세](02-favorite.png)
- [Swagger에서 실제 등록한 응답](03-favorite-201.png) · [같은 영화를 다시 등록한 응답](03-favorite-409.png)
- [평점 단위 테스트](../../src/test/java/com/umc/movie/service/RatingServiceTest.java) · [평점 HTTP 통합 테스트](../../src/test/java/com/umc/movie/RatingApiIntegrationTest.java)
- [전체 테스트 결과](04-tests.png)

캡처는 테스트 프로필(H2)로 실행한 Spring 서버와 Swagger UI에서 얻은 결과다. Windows에서 직접 검증할 때는 `gradlew.bat`가 있는 `movie` 폴더에서 다음을 실행한다.

```powershell
.\gradlew.bat test --rerun-tasks
Start-Process .\build\reports\tests\test\index.html
```

단위 테스트는 가짜 Repository를 사용해 중복 평점 등록(409 RATING409_1)과 타인의 평점 수정(403 RATING403_1)을 확인한다. 통합 테스트는 HTTP로 평점 등록을 호출해 토큰이 없으면 401 AUTH401_2, 로그인으로 발급한 토큰이 있으면 201 COMMON201과 해당 회원의 DB 저장을 확인한다.
