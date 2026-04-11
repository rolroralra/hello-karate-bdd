package com.example.karatebdd.post;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Post API Karate BDD 테스트 러너
 *
 * User API 와 동일한 방식으로 랜덤 포트에서 실행됩니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PostApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        System.setProperty("karate.port", String.valueOf(port));
    }

    @Karate.Test
    Karate testPostApi() {
        return Karate.run("classpath:features/post/post.feature");
    }
}
