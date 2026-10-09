# 05. Karate 실전 사용법

04 문서에서 feature 파일 안의 문법을 익혔다면, 이번에는 **실제 프로젝트에서 테스트를 잘 굴리기 위한 사용법**을 다룹니다.

> 이 문서의 핵심 예제와 실습 과제 정답은 이 프로젝트의 Karate 1.4.1 환경에서 실제로 실행해 검증했습니다.

**이 문서에서 배우는 것**

1. HTTP 요청을 구성하는 모든 키워드
2. 응답 관련 내장 변수
3. 공통 로직 재사용 (`call`, `callonce`)
4. 데이터 주도 테스트 심화 (`Scenario Outline`)
5. 재시도와 훅
6. 환경별 설정과 실행 방법
7. 디버깅과 리포트
8. 실습 과제

---

## 1. HTTP 요청 구성 키워드

### 1-1. 한눈에 보기

| 키워드 | 역할 | 예시 |
|--------|------|------|
| `url` | 기본 주소 | `* url baseUrl` |
| `path` | 경로 (여러 조각을 `/` 로 이어 붙임) | `* path '/api/users', userId` |
| `param` | 쿼리 파라미터 하나 | `* param page = 0` |
| `params` | 쿼리 파라미터 여러 개 | `* params { page: 0, size: 10 }` |
| `header` | 헤더 하나 | `* header Authorization = 'Bearer ' + token` |
| `headers` | 헤더 여러 개 | `* headers { 'X-Trace-Id': 'abc', 'X-Lang': 'ko' }` |
| `cookie` | 쿠키 | `* cookie session = 'xyz'` |
| `form field` | form 전송 필드 | `* form field username = 'admin'` |
| `request` | 요청 본문 | `* request { name: 'Alice' }` |
| `method` | **전송** | `* method POST` |

> JSON 키에 `-` 같은 특수문자가 들어가면 `'X-Trace-Id'` 처럼 따옴표로 감싸야 합니다.

### 1-2. `path` 이어 붙이기

`path` 에 쉼표로 여러 값을 주면 `/` 로 자동 연결되고, 특수문자는 URL 인코딩됩니다.

```gherkin
* path '/api/users', userId               # → /api/users/42
* path 'api', 'users', userId             # → /api/users/42 (같은 결과)
* path '/api/posts/by-author', authorId   # → /api/posts/by-author/7
```

문자열을 직접 더하는 `path '/api/users/' + userId` 보다 쉼표 방식이 안전합니다.

### 1-3. 쿼리 파라미터

```gherkin
Given path '/api/users'
And param page = 0
And param size = 10
When method GET
# → GET /api/users?page=0&size=10
```

### 1-4. 모든 요청에 공통 헤더 넣기

매번 `header` 를 쓰지 않도록 `configure headers` 를 사용합니다.
이 프로젝트의 `karate-config.js` 가 이미 이렇게 하고 있습니다.

```javascript
karate.configure('headers', { 'Content-Type': 'application/json', 'Accept': 'application/json' });
```

feature 파일 안에서도 같은 설정이 가능합니다.

```gherkin
Background:
  * url baseUrl
  * configure headers = { Authorization: 'Bearer my-token' }
```

---

## 2. 응답 관련 내장 변수

`method` 줄이 실행되면 다음 변수들이 자동으로 채워집니다.

| 변수 | 내용 |
|------|------|
| `response` | 응답 본문 (JSON 이면 자동으로 JSON 객체) |
| `responseStatus` | 상태 코드 숫자 (예: `200`) |
| `responseHeaders` | 응답 헤더 전체 |
| `responseTime` | 응답 시간 (ms) |
| `responseCookies` | 응답 쿠키 |

```gherkin
Given path '/api/users', userId
When method GET
Then status 200
And match header Content-Type contains 'application/json'
And assert responseTime < 5000               # 5초 안에 응답했는지
* def code = responseStatus                  # 숫자로 꺼내 쓰기
```

**204 No Content** 처럼 본문이 없는 응답은 `response` 가 빈 문자열입니다.

```gherkin
Given path '/api/users', userId
When method DELETE
Then status 204
And match response == ''
```

---

## 3. 공통 로직 재사용: `call` 과 `callonce`

여러 feature 에서 "테스트용 사용자 만들기" 같은 준비 작업을 반복하고 있다면 별도 feature 로 분리하세요.

### 3-1. 헬퍼 feature 만들기

`src/test/resources/features/common/create-user.feature`:

```gherkin
@ignore
Feature: 테스트용 사용자 생성 헬퍼

  Scenario:
    * def email = 'user-' + java.util.UUID.randomUUID() + '@example.com'
    Given url baseUrl
    And path '/api/users'
    And request { name: '#(name)', email: '#(email)' }
    When method POST
    Then status 201
    * def userId = response.data.id
```

- **`@ignore` 태그**: 이 파일이 단독으로 실행되지 않게 막습니다. 헬퍼에는 꼭 붙이세요.
- **`name` 변수**: 호출하는 쪽에서 넘겨준 값입니다 (아래 참고).
- **`url baseUrl`**: 호출된 feature 는 별도 문맥이라 `url` 을 다시 지정합니다.

### 3-2. 호출하기

```gherkin
Scenario: 포스트 작성
  # 인자를 JSON 으로 넘기면 헬퍼 안에서 name 변수로 쓸 수 있음
  * def author = call read('classpath:features/common/create-user.feature') { name: 'Writer' }

  # 헬퍼 안에서 def 한 변수는 결과 객체의 필드로 꺼냄
  Given path '/api/posts'
  And request { title: 'Hello', content: 'World', authorId: '#(author.userId)' }
  When method POST
  Then status 201
```

헬퍼 안에서 넘겨받은 인자 전체가 필요하면 `__arg` 변수를 쓰면 됩니다.

### 3-3. 배열로 호출하면 반복 실행

```gherkin
* def users = call read('classpath:features/common/create-user.feature') [{ name: 'A' }, { name: 'B' }]
* def ids = get users[*].userId                 # 두 번 실행된 결과의 userId 목록
```

### 3-4. `callonce`: feature 당 한 번만

`Background` 는 Scenario 마다 실행되므로, 비싼 준비 작업(로그인 토큰 발급 등)은 `callonce` 로 한 번만 수행합니다.

```gherkin
Background:
  * url baseUrl
  * def admin = callonce read('classpath:features/common/create-user.feature') { name: 'Admin' }
  # 이 feature 의 모든 Scenario 가 같은 admin.userId 를 공유
```

| | `call` | `callonce` |
|---|---|---|
| 실행 횟수 | 호출할 때마다 | feature 파일당 1회 (결과 캐시) |
| 주 사용처 | 시나리오마다 새 데이터가 필요할 때 | 토큰 발급 등 공유해도 되는 준비 작업 |

---

## 4. 데이터 주도 테스트 심화: `Scenario Outline`

03 문서에서 기본 사용법을 봤습니다. 여기서는 실전에서 유용한 기능을 더 살펴봅니다.

### 4-1. 컬럼을 변수로 바로 사용

`<name>` 처럼 꺾쇠로 치환하는 방법 외에, **컬럼 이름이 그대로 변수**가 됩니다.

```gherkin
Scenario Outline: <name> 생성
  Given path '/api/users'
  And request { name: '#(name)', email: '#(email)' }
  When method POST
  Then status 201

  Examples:
    | name  | email             |
    | Alice | alice@example.com |
    | Bob   | bob@example.com   |
```

### 4-2. `!` 로 타입 지정하기

Examples 의 값은 기본적으로 **문자열**입니다.
컬럼 이름 뒤에 `!` 를 붙이면 값을 JavaScript 로 해석해 숫자, 불리언, null, JSON 을 쓸 수 있습니다.

```gherkin
Examples:
  | name  | age! | active! | email!    |
  | Alice | 30   | true    | 'a@b.com' |
  | Bob   | 25   | false   | null      |
```

`!` 컬럼에서 문자열을 쓰려면 따옴표가 필요하다는 점에 주의하세요.

### 4-3. 동적 Examples

Examples 를 JSON 배열이나 파일로 대체할 수 있습니다. 데이터가 많을 때 유용합니다.

```gherkin
Scenario Outline: 동적 데이터 <id>
  * match id == '#number'

  Examples:
    | [{ id: 1 }, { id: 2 }, { id: 3 }] |
```

```gherkin
Examples:
  | read('classpath:testdata/users.json') |
```

---

## 5. 재시도와 훅

### 5-1. `retry until`: 조건이 맞을 때까지 재요청

비동기로 처리되는 API(예: 작업 상태가 `DONE` 이 될 때까지 대기)를 테스트할 때 씁니다.

```gherkin
* configure retry = { count: 5, interval: 1000 }   # 최대 5회, 1초 간격 (기본값: 3회, 3초)

Given path '/api/users', userId
And retry until responseStatus == 200 && response.data.name == 'Alice'
When method GET
```

`retry until` 은 **`method` 보다 앞에** 써야 합니다.
횟수를 다 써도 조건이 맞지 않으면 테스트가 실패합니다.

### 5-2. `afterScenario`: Scenario 가 끝날 때마다 실행

```gherkin
Background:
  * url baseUrl
  * configure afterScenario = function(){ karate.log('끝난 시나리오:', karate.scenario.name) }
```

로그 남기기, 정리 작업 등에 쓸 수 있습니다.
비슷하게 feature 가 끝날 때 실행되는 `afterFeature` 도 있습니다.

---

## 6. 환경별 설정과 실행 방법

### 6-1. `karate.env` 로 환경 나누기

로컬, 개발 서버, 스테이징 서버 등 환경마다 주소가 다를 때 사용합니다.

`karate-config.js`:

```javascript
function fn() {
    var env = karate.env || 'local';            // 지정 안 하면 'local'
    var port = karate.properties['karate.port'] || '8080';

    var config = { baseUrl: 'http://localhost:' + port };

    if (env == 'dev') {
        config.baseUrl = 'https://dev-api.example.com';
    } else if (env == 'stage') {
        config.baseUrl = 'https://stage-api.example.com';
    }
    return config;
}
```

환경별 파일을 따로 둘 수도 있습니다. `karate.env` 가 `dev` 이면 `karate-config.js` 다음에 `karate-config-dev.js` 가 추가로 로드되어 값을 덮어씁니다.

### 6-2. Gradle 에서 시스템 프로퍼티 전달하기

Gradle 은 테스트를 별도 JVM 에서 실행하기 때문에, `-Dkarate.env=dev` 를 그냥 붙이면 테스트 JVM 까지 전달되지 않습니다.
필요하다면 `build.gradle.kts` 의 `tasks.withType<Test>` 블록에 아래를 추가합니다.

```kotlin
tasks.withType<Test> {
    // ... 기존 설정 ...
    System.getProperty("karate.env")?.let { systemProperty("karate.env", it) }
}
```

```bash
./gradlew test -Dkarate.env=dev
```

### 6-3. 원하는 테스트만 실행하기

```bash
# 전체 테스트
./gradlew test

# 특정 러너 클래스만
./gradlew test --tests 'com.example.karatebdd.user.UserApiTest'

# 패키지 단위
./gradlew test --tests 'com.example.karatebdd.post.*'
```

IntelliJ 에서는 러너 클래스(`UserApiTest`)의 메서드 옆 실행 버튼을 누르면 됩니다.

### 6-4. 태그로 묶어 실행하기

feature 나 Scenario 에 태그를 붙이고, 러너에서 골라 실행합니다.

```gherkin
@smoke
Scenario: 사용자 단건 조회
```

```java
@Karate.Test
Karate smokeTests() {
    return Karate.run("classpath:features").tags("@smoke");
}
```

| 태그 표현 | 의미 |
|-----------|------|
| `.tags("@smoke")` | `@smoke` 가 붙은 것만 |
| `.tags("~@slow")` | `@slow` 가 붙은 것 제외 |
| `.tags("@smoke", "~@slow")` | `@smoke` 이면서 `@slow` 는 아닌 것 |
| `@ignore` | 별도 지정 없이도 항상 제외 (헬퍼 feature 용) |

---

## 7. 디버깅과 리포트

### 7-1. 값 출력하기

```gherkin
* print response                          # 응답 전체를 보기 좋게 출력
* print 'userId =', userId                # 라벨과 함께 출력
* karate.log('status:', responseStatus)   # JS 안에서 출력할 때
```

### 7-2. 요청/응답 로그를 보기 좋게

```gherkin
* configure logPrettyRequest = true
* configure logPrettyResponse = true
```

### 7-3. 리포트 보기

`./gradlew test` 실행 후 아래 파일을 브라우저로 열어보세요.

```
build/karate-reports/karate-summary.html     # Karate 리포트 (요청/응답 상세)
build/reports/tests/test/index.html          # JUnit 리포트
```

Karate 리포트에서 실패한 Scenario 를 클릭하면 **어떤 스텝에서, 어떤 요청과 응답으로** 실패했는지 모두 볼 수 있습니다.
테스트가 실패하면 로그보다 리포트를 먼저 열어보는 습관을 들이면 디버깅이 훨씬 빨라집니다.

### 7-4. 디버깅 순서 추천

1. 리포트에서 실패한 스텝 찾기
2. 실제 응답 확인 (`* print response`)
3. `match ==` 실패라면 → 응답에 예상 못한 필드가 있는지 확인 (`contains` 로 바꿔볼 것)
4. 상태 코드가 다르다면 → 요청 본문/경로가 맞는지 `logPrettyRequest` 로 확인

---

## 8. 실습 과제

배운 내용을 이 프로젝트의 API 로 직접 연습해 봅시다.
`src/test/resources/features/` 에 새 feature 파일을 만들고 러너를 연결해서 실행해 보세요.
(러너 만드는 법은 [03. 테스트 작성 가이드](03-writing-tests.md) 참고)

과제 1, 3 은 3-1 의 `create-user.feature` 헬퍼를 먼저 만들어 두어야 합니다.

### 과제 1. 스키마로 목록 전체 검증하기

> 사용자를 한 명 만든 뒤, 사용자 목록 API 의 **모든 항목**이 `id`(숫자), `name`(문자열), `email`(문자열), `createdAt`(문자열) 모양인지 검증하세요. 목록이 비어 있지 않은지도 확인하세요.

<details>
<summary>정답 보기</summary>

```gherkin
Background:
  * url baseUrl
  * def userSchema = { id: '#number', name: '#string', email: '#string', createdAt: '#string' }

Scenario: 사용자 목록 스키마 검증
  * def created = call read('classpath:features/common/create-user.feature') { name: 'Schema' }

  Given path '/api/users'
  When method GET
  Then status 200
  And match response.data == '#[_ > 0]'
  And match each response.data == userSchema

  # 정리
  Given path '/api/users', created.userId
  When method DELETE
  Then status 204
```

</details>

### 과제 2. 유효성 검사를 Scenario Outline 하나로

> 사용자 생성 시 아래 세 경우 모두 `400` 과 `{ success: false, error: 문자열 }` 응답이 오는지 **Scenario Outline 하나로** 검증하세요.
> 1. 이름이 빈 문자열
> 2. 이메일 형식이 잘못됨
> 3. 이메일 필드 자체가 없음
>
> 힌트: `!` 컬럼과 `##()` 를 함께 쓰면 "필드 자체가 없음"을 표현할 수 있습니다.

<details>
<summary>정답 보기</summary>

```gherkin
Scenario Outline: 사용자 생성 유효성 검사 - <case>
  Given path '/api/users'
  And request { name: '#(name)', email: '##(email)' }
  When method POST
  Then status 400
  And match response == { success: false, data: '##null', error: '#string' }

  Examples:
    | case             | name!   | email!              |
    | 이름 공백        | ''      | 'valid@example.com' |
    | 이메일 형식 오류 | 'Valid' | 'not-an-email'      |
    | 이메일 누락      | 'Valid' | null                |
```

`email` 이 `null` 이면 `##(email)` 에 의해 요청 본문에서 `email` 키가 빠집니다.

</details>

### 과제 3. 헬퍼 feature 로 포스트 작성 검증

> 헬퍼로 작성자를 만들고, 그 작성자로 포스트를 생성하세요.
> 응답에 `title`, `authorId`, `authorName` 이 올바르게 들어있는지 **`contains` 한 줄로** 검증하고, 만든 데이터는 모두 정리하세요.

<details>
<summary>정답 보기</summary>

```gherkin
Scenario: 헬퍼로 만든 작성자로 포스트 생성
  * def author = call read('classpath:features/common/create-user.feature') { name: 'Writer' }

  Given path '/api/posts'
  And request { title: 'Hello', content: 'World', authorId: '#(author.userId)' }
  When method POST
  Then status 201
  And match response.data contains { title: 'Hello', authorId: '#(author.userId)', authorName: 'Writer' }
  * def postId = response.data.id

  # 정리: 포스트 먼저, 그다음 작성자 (FK 순서)
  Given path '/api/posts', postId
  When method DELETE
  Then status 204

  Given path '/api/users', author.userId
  When method DELETE
  Then status 204
```

</details>

---

## 더 공부하기

- [Karate 공식 문서 (GitHub)](https://github.com/karatelabs/karate) — 모든 키워드의 원본 레퍼런스
- [Karate 데모 예제](https://github.com/karatelabs/karate/tree/master/karate-demo) — 인증, 파일 업로드 등 다양한 실전 예제

---

| | |
|---|---|
| 이전: [← 04. Karate 문법 친절 가이드](04-karate-syntax-guide.md) | |
