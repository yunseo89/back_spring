package com.umc.movie;

import com.umc.movie.repository.RatingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RatingApiIntegrationTest {
    private static final String RATING = """
            {"movieId":550,"score":5,"comment":"다시 보고 싶은 영화"}
            """;
    @LocalServerPort int port;
    @Autowired RatingRepository ratings;
    final HttpClient client = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("토큰 없이 POST /ratings를 호출하면 401 AUTH401_2이고 새 평점이 없다")
    void createWithoutToken() throws Exception {
        long before = ratings.count();
        var response = post("/ratings", RATING, null);
        assertThat(response.statusCode()).isEqualTo(401);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("code").asText()).isEqualTo("AUTH401_2");
        assertThat(body.path("isSuccess").asBoolean()).isFalse();
        assertThat(ratings.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("로그인 토큰으로 POST /ratings를 호출하면 201 COMMON201이고 토큰 주인의 평점이 저장된다")
    void createWithLoginToken() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "week9-" + suffix + "@example.com";
        String signup = """
                {"email":"%s","password":"MovieLog9!","nickname":"w9%s"}
                """.formatted(email, suffix);
        var member = post("/members", signup, null);
        assertThat(member.statusCode()).isEqualTo(201);
        long memberId = json.readTree(member.body()).at("/result/memberId").asLong();
        var login = post("/auth/login", """
                {"email":"%s","password":"MovieLog9!"}
                """.formatted(email), null);
        assertThat(login.statusCode()).isEqualTo(200);
        String token = json.readTree(login.body()).at("/result/accessToken").asText();

        var response = post("/ratings", RATING, token);
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("code").asText()).isEqualTo("COMMON201");
        assertThat(body.path("isSuccess").asBoolean()).isTrue();
        assertThat(body.at("/result/movieId").asLong()).isEqualTo(550);
        assertThat(body.at("/result/score").asInt()).isEqualTo(5);
        assertThat(ratings.existsByMember_MemberIdAndMovieId(memberId, 550L)).isTrue();
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
