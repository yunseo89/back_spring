package com.umc.movie;

import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.repository.*;
import com.umc.movie.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/** 실제 HTTP → 인증 인터셉터 → 서비스 프록시 → DB까지 호출합니다. 테스트 자체는 트랜잭션이 아닙니다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Week8IntegrationTest {
    @LocalServerPort int port;
    @Autowired MemberRepository members;
    @Autowired RatingRepository ratings;
    @MockitoSpyBean FavoriteRepository favorites;
    @Autowired JwtUtil jwt;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired Environment environment;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();
    Member a, b;
    String tokenA, tokenB;

    @BeforeEach
    void prepare() {
        // TEST_DB_URL 오입력 시 개인 실습 DB를 비우지 않도록 강제합니다.
        String url = environment.getRequiredProperty("spring.datasource.url");
        assertThat(url).matches("jdbc:h2:mem:week8_test.*|jdbc:mysql://[^/]+/umc_movie_week8_test(?:[?].*)?");
        favorites.deleteAll();
        ratings.deleteAll();
        members.deleteAll();
        a = members.saveAndFlush(Member.builder().email("a-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("TestOnly8!")).nickname("테스트가").build());
        b = members.saveAndFlush(Member.builder().email("b-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("TestOnly8!")).nickname("테스트나").build());
        tokenA = jwt.createAccessToken(a.getMemberId(), a.getEmail());
        tokenB = jwt.createAccessToken(b.getMemberId(), b.getEmail());
    }

    record Reply(int status, JsonNode body) {}
    Reply request(String method, String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), json.readTree(response.body()));
    }
    Reply favorite(long movie, String token) throws Exception {
        return request("POST", "/favorites", "{\"movieId\":" + movie + "}", token);
    }
    Reply record(long movie, boolean add) throws Exception {
        return request("POST", "/movie-records", "{\"movieId\":" + movie
                + ",\"score\":5,\"comment\":\"다시 보고 싶어요\",\"addToFavorites\":" + add + "}", tokenA);
    }
    void expect(Reply r, int status, String code) {
        assertThat(r.status()).isEqualTo(status);
        assertThat(r.body().path("code").asText()).isEqualTo(code);
        assertThat(r.body().path("isSuccess").asBoolean()).isEqualTo(status < 400);
    }

    @Test void loginTokenCanAccessNewProtectedApis() throws Exception {
        var login = request("POST", "/auth/login", "{\"email\":\"" + a.getEmail()
                + "\",\"password\":\"TestOnly8!\"}", null);
        expect(login, 200, "COMMON200");
        String token = login.body().path("result").path("accessToken").asText();
        expect(favorite(550, token), 201, "COMMON201");
    }

    @Test void favoriteLifecycleAndDuplicate() throws Exception {
        var empty = request("GET", "/favorites", null, tokenA);
        expect(empty, 200, "COMMON200");
        assertThat(empty.body().path("result").size()).isZero();
        assertThat(request("GET", "/favorites/550", null, tokenA).body().path("result").path("isFavorite").asBoolean()).isFalse();
        var created = favorite(550, tokenA);
        expect(created, 201, "COMMON201");
        assertThat(created.body().path("result").path("createdAt").isTextual()).isTrue();
        expect(favorite(550, tokenA), 409, "FAVORITE409_1");
        assertThat(request("GET", "/favorites/550", null, tokenA).body().path("result").path("isFavorite").asBoolean()).isTrue();
        var deleted = request("DELETE", "/favorites/550", null, tokenA);
        expect(deleted, 200, "COMMON200");
        assertThat(deleted.body().path("result").isNull()).isTrue();
        expect(request("DELETE", "/favorites/550", null, tokenA), 404, "FAVORITE404_1");
        assertThat(request("GET", "/favorites/550", null, tokenA).body().path("result").path("isFavorite").asBoolean()).isFalse();
    }

    @Test void memberIsolationAndLatestFirst() throws Exception {
        favorite(550, tokenA);
        favorite(238, tokenA);
        var list = request("GET", "/favorites", null, tokenA).body().path("result");
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).path("movieId").asLong()).isEqualTo(238);
        assertThat(list.get(1).path("movieId").asLong()).isEqualTo(550);
        assertThat(request("GET", "/favorites", null, tokenB).body().path("result").size()).isZero();
        assertThat(request("GET", "/favorites/550", null, tokenB).body().path("result").path("isFavorite").asBoolean()).isFalse();
        expect(request("DELETE", "/favorites/550", null, tokenB), 404, "FAVORITE404_1");
        expect(favorite(550, tokenB), 201, "COMMON201");
        assertThat(favorites.existsByMember_MemberIdAndMovieId(a.getMemberId(), 550L)).isTrue();
    }

    @Test void requestMemberIdCannotImpersonateAnotherUser() throws Exception {
        var r = request("POST", "/favorites?memberId=" + b.getMemberId(),
                "{\"movieId\":550,\"memberId\":" + b.getMemberId() + "}", tokenA);
        // 알 수 없는 필드를 무시하더라도, 저장 주인은 오직 검증된 토큰의 회원이어야 합니다.
        expect(r, 201, "COMMON201");
        assertThat(favorites.existsByMember_MemberIdAndMovieId(a.getMemberId(), 550L)).isTrue();
        assertThat(favorites.existsByMember_MemberIdAndMovieId(b.getMemberId(), 550L)).isFalse();
    }

    @Test void allFiveEndpointsRequireAuthentication() throws Exception {
        expect(favorite(550, null), 401, "AUTH401_2");
        expect(request("GET", "/favorites", null, null), 401, "AUTH401_2");
        expect(request("GET", "/favorites/550", null, null), 401, "AUTH401_2");
        expect(request("DELETE", "/favorites/550", null, null), 401, "AUTH401_2");
        expect(request("POST", "/movie-records", "{}", null), 401, "AUTH401_2");
        expect(favorite(550, "invalid-token"), 401, "AUTH401_3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"movieId\":0}", "{\"movieId\":-1}", "{\"movieId\":null}", "{\"movieId\":\"bad\"}", "{"})
    void invalidFavoriteInput(String body) throws Exception {
        expect(request("POST", "/favorites", body, tokenA), 400, "COMMON4001");
        assertThat(favorites.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void invalidMoviePath(String movie) throws Exception {
        expect(request("GET", "/favorites/" + movie, null, tokenA), 400, "COMMON4001");
        expect(request("DELETE", "/favorites/" + movie, null, tokenA), 400, "COMMON4001");
    }

    @Test void recordTrueCommitsBothAndDuplicateChangesNothing() throws Exception {
        var r = record(238, true);
        expect(r, 201, "COMMON201");
        assertThat(r.body().path("result").path("rating").path("movieId").asLong()).isEqualTo(238);
        assertThat(r.body().path("result").path("favorite").path("movieId").asLong()).isEqualTo(238);
        assertThat(r.body().path("result").path("rating").path("createdAt").isTextual()).isTrue();
        assertThat(ratings.count()).isEqualTo(1);
        assertThat(favorites.count()).isEqualTo(1);
        expect(record(238, true), 409, "RATING409_1");
        assertThat(ratings.count()).isEqualTo(1);
        assertThat(favorites.count()).isEqualTo(1);
    }

    @Test void recordFalseSavesOnlyRatingAndPreservesExistingFavorite() throws Exception {
        favorite(680, tokenA);
        var r = record(680, false);
        expect(r, 201, "COMMON201");
        assertThat(r.body().path("result").path("favorite").isNull()).isTrue();
        assertThat(ratings.count()).isEqualTo(1);
        assertThat(favorites.count()).isEqualTo(1);
        var onlyRating = record(13, false);
        expect(onlyRating, 201, "COMMON201");
        assertThat(favorites.existsByMember_MemberIdAndMovieId(a.getMemberId(), 13L)).isFalse();
    }

    @Test void favoritePrecheckRejectsBeforeAnyNewRating() throws Exception {
        favorite(238, tokenA);
        expect(record(238, true), 409, "FAVORITE409_1");
        assertThat(ratings.count()).isZero();
        assertThat(favorites.count()).isEqualTo(1);
    }

    @Test void databaseUniqueFailureRollsBackAlreadyInsertedRating() throws Exception {
        favorites.saveAndFlush(Favorite.builder().member(a).movieId(238L).build());
        // 사전 검사와 INSERT 사이에 다른 요청이 먼저 등록한 경쟁 상황을 재현합니다.
        // 검사 결과만 제어하고, 두 INSERT와 UNIQUE 위반/rollback은 실제 DB에서 수행합니다.
        doReturn(false).when(favorites).existsByMember_MemberIdAndMovieId(a.getMemberId(), 238L);
        expect(record(238, true), 409, "FAVORITE409_1");
        assertThat(ratings.count()).isZero(); // 첫 번째 INSERT도 DB에 남지 않아야 합니다.
        assertThat(favorites.count()).isEqualTo(1); // 미리 존재하던 즐겨찾기만 남습니다.
    }

    @Test void databaseUniqueFailureOnFavoriteAloneUsesDomainCode() throws Exception {
        favorites.saveAndFlush(Favorite.builder().member(a).movieId(550L).build());
        doReturn(false).when(favorites).existsByMember_MemberIdAndMovieId(a.getMemberId(), 550L);
        expect(favorite(550, tokenA), 409, "FAVORITE409_1");
        assertThat(favorites.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"movieId\":238,\"score\":0,\"addToFavorites\":true}",
        "{\"movieId\":238,\"score\":6,\"addToFavorites\":true}",
        "{\"movieId\":238,\"addToFavorites\":true}",
        "{\"movieId\":238,\"score\":5}",
        "{\"movieId\":238,\"score\":5,\"addToFavorites\":null}",
        "{\"movieId\":0,\"score\":5,\"addToFavorites\":true}"
    })
    void invalidRecordNeverWrites(String body) throws Exception {
        expect(request("POST", "/movie-records", body, tokenA), 400, "COMMON4001");
        assertThat(ratings.count()).isZero();
        assertThat(favorites.count()).isZero();
    }

    @Test void tooLongCommentNeverWrites() throws Exception {
        String body = "{\"movieId\":238,\"score\":5,\"addToFavorites\":true,\"comment\":\"" + "가".repeat(501) + "\"}";
        expect(request("POST", "/movie-records", body, tokenA), 400, "COMMON4001");
        assertThat(ratings.count()).isZero();
        assertThat(favorites.count()).isZero();
    }
}
