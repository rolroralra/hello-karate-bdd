package com.example.karatebdd;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * 전체 feature 통합 러너 (순차 실행)
 *
 * classpath:features 폴더 아래의 모든 .feature 파일을 한 번에 실행합니다.
 * 새 feature 를 추가해도 러너를 따로 만들 필요가 없고,
 * IntelliJ 테스트 트리에 feature / Scenario 단위로 결과가 표시됩니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AllFeaturesTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        System.setProperty("karate.port", String.valueOf(port));
    }

    @Karate.Test
    Karate testAllFeatures() {
        return Karate.run("classpath:features");
    }
}