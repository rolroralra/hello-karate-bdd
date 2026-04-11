# 01. Karate 개요

## Karate 란?

**Karate** 는 REST/GraphQL API 테스트, Mock 서버, 성능 테스트, UI 자동화를 하나의 도구로 통합한 오픈소스 BDD 테스트 프레임워크입니다.
Peter Thomas 가 2017년 처음 개발했고, 현재는 [Karate Labs](https://www.karatelabs.io/) 에서 관리합니다.

참고: [Baeldung - Karate REST API Testing](https://www.baeldung.com/karate-rest-api-testing)

---

## 왜 Karate 인가?

| 기존 방식 (RestAssured + JUnit) | Karate |
|---|---|
| Java 코드로 HTTP 요청/검증 작성 | Gherkin 문법(.feature 파일)으로 작성 |
| Given/When/Then 을 코드로 구현해야 함 | Given/When/Then 이 내장 DSL로 제공 |
| JSON 매칭이 번거로움 | `match` 키워드로 직관적 JSON 검증 |
| 테스트 데이터 관리가 복잡 | 변수, 재사용 가능한 feature call 지원 |
| 비개발자가 읽기 어려움 | 비개발자도 읽고 이해할 수 있는 시나리오 |

---

## 핵심 개념

### 1. Feature 파일 (`.feature`)

Gherkin 문법으로 작성된 BDD 시나리오 파일입니다.

```gherkin
Feature: 기능 설명

  Background:
    # 각 Scenario 실행 전 공통으로 수행할 스텝

  Scenario: 시나리오 설명
    Given  # 전제 조건 (사전 상태 설정)
    When   # 행동 (HTTP 요청 등)
    Then   # 결과 검증
    And    # 추가 조건/검증
    But    # 예외 케이스
    *      # 어느 위치에서나 사용 가능한 범용 스텝
```

### 2. URL & Path 설정

```gherkin
Given url 'http://localhost:8080'
And path '/api/users'

# 또는 karate-config.js 의 baseUrl 변수 활용
Given url baseUrl
And path '/api/users'

# path 세그먼트 자동 결합
Given path '/api/users', userId    # → /api/users/42
```

### 3. HTTP 메서드

```gherkin
When method GET
When method POST
When method PUT
When method DELETE
When method PATCH
```

### 4. Request Body

```gherkin
And request { name: 'Alice', email: 'alice@example.com' }

# 변수 사용
* def payload = { name: 'Alice', email: 'alice@example.com' }
And request payload
```

### 5. 상태 코드 검증

```gherkin
Then status 200
Then status 201
Then status 404
```

### 6. Response 검증 (`match`)

```gherkin
# 정확한 값 비교
And match response.data.name == 'Alice'

# 타입 검사 (퍼지 매칭)
And match response.data.id == '#number'       # 숫자
And match response.data.email == '#string'    # 문자열
And match response.data.createdAt == '#notnull'  # null 아님
And match response.data == '#notnull'

# 배열 검사
And match response.data == '#array'
And match response.data == '#[3]'         # 원소 3개

# 배열에 특정 원소 포함 여부
And match response.data[*].id contains 42

# 와일드카드
And match response.data == { id: '#number', name: '#string', email: '#string', createdAt: '#notnull' }
```

### 7. 변수 (def)

```gherkin
# 응답에서 변수 추출
* def userId = response.data.id

# 이후 스텝에서 재사용
Given path '/api/users', userId
```

### 8. Background

각 Scenario 실행 전 공통으로 수행됩니다.
테스트 픽스처(사전 데이터 생성 등)에 활용합니다.

```gherkin
Background:
  * url baseUrl
  * def token = 'Bearer my-token'
  Given path '/api/users'
  And request { name: 'Author', email: 'author@test.com' }
  When method POST
  Then status 201
  * def authorId = response.data.id
```

---

## 매칭 마커 (Fuzzy Matching)

| 마커 | 의미 |
|------|------|
| `#string` | 문자열 |
| `#number` | 숫자 (정수/실수) |
| `#boolean` | 불리언 |
| `#array` | 배열 |
| `#object` | 객체 |
| `#null` | null |
| `#notnull` | null 이 아닌 값 |
| `#present` | 키가 존재함 |
| `#notpresent` | 키가 없음 |
| `#[n]` | 원소가 n 개인 배열 |
| `#regex <pattern>` | 정규식 매칭 |
| `#? <js expression>` | JS 표현식 평가 |

---

## karate-config.js

테스트 전역 설정을 담당합니다. 테스트 클래스패스 루트(`src/test/resources/karate-config.js`)에 위치해야 합니다.

```javascript
function fn() {
    var port = karate.properties['karate.port'] || '8080';
    var config = {
        baseUrl: 'http://localhost:' + port
    };

    // 기본 헤더 설정
    karate.configure('headers', { 'Content-Type': 'application/json' });

    // 타임아웃 설정 (ms)
    karate.configure('readTimeout', 10000);

    return config;
}
```

---

## JUnit 5 통합

Karate 는 JUnit 5 확장을 통해 표준 테스트 러너와 통합됩니다.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        // karate-config.js 에서 읽을 포트 설정
        System.setProperty("karate.port", String.valueOf(port));
    }

    @Karate.Test
    Karate testUserApi() {
        return Karate.run("classpath:features/user/user.feature");
    }
}
```

---

| | |
|---|---|
| 이전: [← README](../README.md) | 다음: [02. 프로젝트 구조 설명 →](02-project-structure.md) |
