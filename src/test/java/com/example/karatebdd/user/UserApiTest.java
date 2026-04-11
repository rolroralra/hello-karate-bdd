package com.example.karatebdd.user;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * User API Karate BDD 테스트 러너
 *
 * @SpringBootTest(webEnvironment = RANDOM_PORT) 로 내장 서버를 랜덤 포트에 실행하고,
 * 해당 포트를 System Property "karate.port" 에 저장하여 karate-config.js 에서 읽습니다.
 *
 * 테스트 프로파일(application-test.yml)에 의해 H2 메모리 DB 가 사용됩니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        System.setProperty("karate.port", String.valueOf(port));
    }

    @Karate.Test
    Karate testUserApi() {
        return Karate.run("classpath:features/user/user.feature");
    }
}
