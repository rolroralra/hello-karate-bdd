# 03. 테스트 작성 가이드

## 전체 테스트 흐름

```
JUnit5 실행
  └─ @SpringBootTest (RANDOM_PORT)
      └─ Spring Context 기동 (H2 + test 프로파일)
          └─ @BeforeEach: System.setProperty("karate.port", ...)
              └─ @Karate.Test: Karate 엔진 시작
                  └─ karate-config.js 로드 (baseUrl 설정)
                      └─ .feature 파일 순서대로 Scenario 실행
                          └─ HTTP 요청 → 응답 검증
```

---

## 새 Feature 파일 작성하기

### 1단계: feature 파일 생성

`src/test/resources/features/<도메인>/` 디렉토리에 `.feature` 파일을 만듭니다.

```gherkin
Feature: Comment API
  # 이 Feature 가 테스트하는 기능을 간략히 설명

  Background:
    * url baseUrl
    # 공통 전제 조건 (픽스처 생성 등)

  Scenario: 댓글 생성
    Given path '/api/comments'
    And request { content: '좋은 글이에요!', postId: 1 }
    When method POST
    Then status 201
    And match response.success == true
    And match response.data.id == '#number'
```

### 2단계: JUnit5 테스트 러너 생성

`src/test/java/com/example/karatebdd/<도메인>/` 에 러너 클래스를 만듭니다.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CommentApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        System.setProperty("karate.port", String.valueOf(port));
    }

    @Karate.Test
    Karate testCommentApi() {
        return Karate.run("classpath:features/comment/comment.feature");
    }
}
```

---

## Karate 문법 레시피

### 변수 정의 및 재사용

```gherkin
Scenario: 변수 활용 예시
  # 직접 정의
  * def userName = 'Alice'
  * def userEmail = 'alice@example.com'

  Given path '/api/users'
  And request { name: '#(userName)', email: '#(userEmail)' }
  When method POST
  Then status 201

  # 응답에서 추출
  * def createdId = response.data.id

  # 다음 요청에서 재사용
  Given path '/api/users', createdId
  When method GET
  Then status 200
```

### JSON 파일로 요청 본문 분리

`src/test/resources/testdata/create-user.json`:
```json
{ "name": "Alice", "email": "alice@example.com" }
```

Feature 파일에서 사용:
```gherkin
* def createUserPayload = read('classpath:testdata/create-user.json')

Given path '/api/users'
And request createUserPayload
When method POST
Then status 201
```

### Scenario Outline (데이터 기반 테스트)

```gherkin
Scenario Outline: 다양한 이메일로 사용자 생성
  Given path '/api/users'
  And request { name: '<name>', email: '<email>' }
  When method POST
  Then status <status>

  Examples:
    | name    | email               | status |
    | Alice   | alice@example.com   | 201    |
    | Bob     | bob@example.com     | 201    |
    | Invalid | not-an-email        | 400    |
```

### 태그(Tag)로 선택적 실행

```gherkin
@smoke
Scenario: 가장 중요한 핵심 시나리오

@slow
Scenario: 시간이 오래 걸리는 시나리오
```

특정 태그만 실행:
```java
return Karate.run("classpath:features").tags("@smoke");
```

태그 제외:
```java
return Karate.run("classpath:features").tags("~@slow");
```

### 다른 Feature 재사용 (Call)

공통 인증 토큰을 얻는 feature:

`auth.feature`:
```gherkin
Feature: Auth helper

  Scenario: 로그인
    Given url baseUrl
    And path '/api/auth/login'
    And request { username: 'admin', password: 'secret' }
    When method POST
    Then status 200
    * def token = response.data.token
```

다른 feature 에서 호출:
```gherkin
  Scenario: 인증이 필요한 API 호출
    * def authResult = call read('classpath:features/auth/auth.feature')
    * def token = authResult.token

    Given path '/api/admin/users'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
```

### JavaScript 표현식 활용

```gherkin
Scenario: JS 표현식으로 유연한 검증
  Given path '/api/users'
  When method GET
  Then status 200

  # 배열 길이 검증
  * def count = response.data.length
  * assert count >= 0

  # JS 표현식 매칭
  And match response.data[*].email == '#[] #? _.includes("@")'
```

---

## 테스트 격리 전략

Karate 테스트는 HTTP 를 통해 실제 서버와 통신하므로, 각 Scenario 가 독립적으로 실행될 수 있도록 데이터를 직접 관리해야 합니다.

### 패턴 1: Scenario 내에서 생성 → 삭제 (권장)

```gherkin
Scenario: 격리된 CRUD 테스트
  # 1. 테스트 데이터 생성
  Given path '/api/users'
  And request { name: 'Test', email: 'test@example.com' }
  When method POST
  Then status 201
  * def userId = response.data.id

  # 2. 테스트 수행 ...

  # 3. 테스트 데이터 정리 (cleanup)
  Given path '/api/users', userId
  When method DELETE
  Then status 204
```

### 패턴 2: Background 에서 생성, 마지막 Scenario 에서 정리

테스트 데이터 공유가 필요할 때 사용하되, 실행 순서에 의존하지 않도록 주의합니다.

### 패턴 3: 고유한 테스트 데이터 사용

```gherkin
# UUID 를 활용해 이메일 중복 방지
* def randomEmail = 'test-' + java.util.UUID.randomUUID() + '@example.com'
Given path '/api/users'
And request { name: 'Test', email: '#(randomEmail)' }
When method POST
Then status 201
```

---

## 테스트 리포트

Karate 는 실행 후 자동으로 HTML 리포트를 생성합니다.

```
build/
├── reports/
│   └── tests/test/          # JUnit HTML 리포트
└── surefire-reports/        # XML 리포트 (CI 용)
```

Karate 전용 리포트 (더 상세):
```
target/karate-reports/       # Maven 기준
```

Gradle 에서는 Karate 리포트가 `build/` 하위에 생성됩니다.

---

## 자주 발생하는 오류와 해결

### `Connection refused`

테스트 실행 시 서버에 연결할 수 없는 경우:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` 가 선언되어 있는지 확인
- `System.setProperty("karate.port", ...)` 가 `@BeforeEach` 에 있는지 확인
- `application-test.yml` 에 `spring.docker.compose.enabled: false` 가 있는지 확인

### `NoSuchBeanDefinitionException`

테스트 컨텍스트에 필요한 Bean 이 없는 경우:
- `@SpringBootTest` 가 메인 애플리케이션 클래스 하위 패키지에 있는지 확인

### H2 방언 오류

`DDL` 실행 시 PostgreSQL 전용 문법 오류:
- `application-test.yml` 에 `hibernate.dialect: org.hibernate.dialect.H2Dialect` 를 명시
- `url` 에 `MODE=PostgreSQL` 옵션 추가

### `match` 실패 디버깅

```gherkin
# 응답 전체를 로그로 출력
* print response
* print responseStatus
* print responseHeaders
```

---

| | |
|---|---|
| 이전: [← 02. 프로젝트 구조 설명](02-project-structure.md) | |
