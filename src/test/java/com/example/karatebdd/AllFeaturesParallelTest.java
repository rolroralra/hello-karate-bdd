package com.example.karatebdd;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 전체 feature 통합 러너 (병렬 실행, CI 용)
 *
 * Runner.parallel() 로 모든 feature 를 병렬 실행하고, 리포트를 build/karate-reports 에 하나로 모읍니다.
 * (Gradle 프로젝트에서는 Karate 가 build/ 를 자동으로 리포트 위치로 사용합니다.)
 * @Karate.Test 가 아닌 일반 @Test 이므로 실패 여부는 Results 로 직접 검증합니다.
 *
 * 주의: 병렬 실행 시 Scenario 들이 같은 DB 를 동시에 사용하므로,
 * 다른 Scenario 의 데이터에 의존하는 검증(예: 전체 목록 개수)은 피해야 합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AllFeaturesParallelTest {

    private static final int THREAD_COUNT = 4;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        System.setProperty("karate.port", String.valueOf(port));
    }

    @Test
    void testAllFeaturesInParallel() {
        Results results = Runner.path("classpath:features")
                .tags("~@ignore")
                .parallel(THREAD_COUNT);

        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
