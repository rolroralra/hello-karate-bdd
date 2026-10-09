# 04. Karate 문법 친절 가이드

앞의 문서들에서 Karate 의 전체 그림과 테스트 작성 흐름을 살펴봤습니다.
이 문서에서는 **feature 파일 안에서 실제로 쓰는 문법**을 하나씩 천천히 짚어봅니다.

> 이 문서의 모든 예제는 이 프로젝트의 Karate 1.4.1 환경에서 실제로 실행해 검증했습니다.

**이 문서에서 배우는 것**

1. feature 파일을 한 줄씩 읽는 법
2. 변수와 데이터 타입
3. JSON 만들고 고치기
4. 응답에서 원하는 값 꺼내기
5. `match` 완전 정복
6. JavaScript / Java 함께 쓰기
7. 초보자가 자주 하는 실수

---

## 1. feature 파일 한 줄씩 읽기

`user.feature` 의 일부를 한 줄씩 해석해 보겠습니다.

```gherkin
Feature: User API                              # (1) 이 파일이 테스트하는 기능 이름

  Background:                                  # (2) 모든 Scenario 직전에 매번 실행
    * url baseUrl                              # (3) 요청 기본 주소 (karate-config.js 에서 옴)

  Scenario: User CRUD 전체 흐름                # (4) 테스트 케이스 하나
    Given path '/api/users'                    # (5) baseUrl 뒤에 붙을 경로
    And request { name: 'Alice', email: 'alice@example.com' }   # (6) 요청 본문
    When method POST                           # (7) 이 줄에서 실제로 HTTP 요청이 나감!
    Then status 201                            # (8) 상태 코드 검증
    And match response.data.name == 'Alice'    # (9) 응답 본문 검증
    * def userId = response.data.id            # (10) 응답 값을 변수에 저장
```

핵심은 **`method` 줄에서 요청이 전송된다**는 점입니다.
그 위의 `url`, `path`, `request`, `header` 등은 요청을 "준비"하는 줄이고,
그 아래의 `status`, `match` 는 받은 응답을 "검사"하는 줄입니다.

```
준비 (url / path / param / header / request)
   ↓
전송 (method GET / POST / PUT / DELETE)
   ↓
검사 (status / match / assert)
```

> 요청을 한 번 보내고 나면 `path`, `request`, `param` 같은 준비 값은 초기화됩니다.
> 그래서 같은 Scenario 안에서 다음 요청을 보낼 때 `path` 를 다시 적어야 합니다.
> (`url` 과 `configure headers` 로 지정한 값은 유지됩니다.)

---

## 2. Given / When / Then 은 "읽기 좋게 하는 장식"

Karate 에서 `Given`, `When`, `Then`, `And`, `But`, `*` 는 **모두 똑같이 동작**합니다.
아래 두 블록은 완전히 같은 테스트입니다.

```gherkin
# 사람이 읽기 좋은 버전
Given path '/api/users'
When method GET
Then status 200

# 기계적으로 같은 버전
* path '/api/users'
* method GET
* status 200
```

관례적으로 다음처럼 구분해서 씁니다.

| 키워드 | 주로 쓰는 곳 |
|--------|-------------|
| `Given` | 요청 준비 (`path`, `request`) |
| `When` | 요청 전송 (`method`) |
| `Then` | 첫 번째 검증 (`status`) |
| `And` | 앞 줄과 같은 단계의 추가 내용 |
| `*` | 변수 정의, 출력 등 BDD 문장으로 읽기 어색한 줄 |

---

## 3. 변수와 데이터 타입

### 3-1. `def` 로 변수 만들기

```gherkin
* def name = 'Alice'                     # 문자열 (작은따옴표, 큰따옴표 모두 가능)
* def age = 30                           # 숫자
* def active = true                      # 불리언
* def user = { name: 'Alice', age: 30 }  # JSON 객체 (키에 따옴표 생략 가능)
* def tags = ['a', 'b', 'c']             # JSON 배열
* def nothing = null                     # null
```

`def` 오른쪽에는 **JavaScript 표현식**이 올 수 있습니다.

```gherkin
* def total = 10 + 20                    # 30
* def greeting = 'Hello, ' + name        # 'Hello, Alice'
* def upper = name.toUpperCase()         # 'ALICE'
```

### 3-2. 변수의 범위

- `Background` 에서 만든 변수 → 모든 Scenario 에서 사용 가능
- `Scenario` 에서 만든 변수 → 그 Scenario 안에서만 사용 가능
- 각 Scenario 는 **서로 변수를 공유하지 않습니다** (Scenario A 에서 만든 `userId` 를 Scenario B 에서 쓸 수 없음)

### 3-3. 타입 변환

API 응답은 문자열로 올 때도 있어서 타입 변환이 자주 필요합니다.

```gherkin
# 문자열 → 숫자
* def str = '42'
* def num = ~~str                        # 42 (정수 변환 JS 관용구)
* def num2 = parseInt(str)               # 이것도 가능

# 숫자 → 문자열
* def numStr = 42 + ''                   # '42'

# 문자열 → JSON
* json parsed = '{ "a": 1 }'
* match parsed.a == 1

# JSON → 문자열
* string back = parsed
* match back contains '"a"'
```

### 3-4. 여러 줄 텍스트: `text`

GraphQL 쿼리나 긴 문자열은 `"""` 블록으로 씁니다.
`text` 는 내용을 해석하지 않고 **문자열 그대로** 저장합니다.

```gherkin
* text query =
  """
  {
    users { id name }
  }
  """
```

### 3-5. 표로 JSON 배열 만들기: `table`

```gherkin
* table users
  | name    | age |
  | 'Alice' | 30  |
  | 'Bob'   | 25  |

# 결과: [{ name: 'Alice', age: 30 }, { name: 'Bob', age: 25 }]
```

> `table` 의 칸은 JavaScript 표현식으로 해석되므로 **문자열은 반드시 따옴표**로 감싸야 합니다.
> 따옴표를 빼먹으면 `Alice` 라는 변수를 찾다가 에러가 납니다.

---

## 4. JSON 만들고 고치기

### 4-1. 임베디드 표현식 `#( )`

JSON 안에 변수를 넣고 싶을 때 `'#(변수명)'` 을 씁니다.

```gherkin
* def name = 'Alice'
* def age = 30
* def user = { name: '#(name)', age: '#(age)' }

* match user == { name: 'Alice', age: 30 }
* match user.age == '#number'            # 따옴표 안에 썼어도 숫자 타입 그대로!
```

**자주 하는 오해:** `'#(age)'` 처럼 따옴표 안에 써도 결과가 문자열 `'30'` 이 되지 않습니다.
Karate 는 원래 변수의 타입(숫자, 객체, 배열 등)을 그대로 보존합니다.

문자열 이어 붙이기처럼 계산이 필요하면 괄호 안에 표현식을 쓰면 됩니다.

```gherkin
* def user = { greeting: '#("Hello, " + name)' }
```

### 4-2. 값이 없으면 키 자체를 빼기: `##( )`

샵을 두 개 쓰면 **값이 null 일 때 키 자체가 제거**됩니다.
선택 필드가 있는 요청을 만들 때 유용합니다.

```gherkin
* def nickname = null
* def user = { name: 'Alice', nickname: '##(nickname)' }

* match user == { name: 'Alice' }        # nickname 키가 아예 없음
```

### 4-3. 값 바꾸기 / 추가하기: `set`

```gherkin
* def user = { name: 'Alice' }
* set user.email = 'alice@example.com'   # 새 키 추가
* set user.name = 'Alice Kim'            # 기존 값 변경

* match user == { name: 'Alice Kim', email: 'alice@example.com' }
```

### 4-4. 키 제거하기: `remove`

```gherkin
* def user = { name: 'Alice', age: 30 }
* remove user.age

* match user == { name: 'Alice' }
```

유효성 검사 테스트에서 "필수 필드가 빠진 요청"을 만들 때 아주 편리합니다.

### 4-5. 원본을 지키는 복사: `copy`

`def` 로 다른 변수에 대입하면 **같은 객체를 가리킵니다.** 한쪽을 고치면 다른 쪽도 바뀝니다.
원본을 그대로 두고 변형본을 만들고 싶으면 `copy` 를 쓰세요.

```gherkin
* def base = { name: 'Alice', email: 'alice@example.com' }

* copy invalid = base
* set invalid.email = 'not-an-email'

* match base.email == 'alice@example.com'   # 원본은 그대로
```

### 4-6. 파일 읽기: `read`

```gherkin
* def payload = read('classpath:testdata/create-user.json')   # 클래스패스 기준
* def payload2 = read('create-user.json')                      # 현재 feature 파일 기준 상대 경로
```

`read` 는 확장자를 보고 알아서 변환합니다. `.json` 은 JSON 으로, `.txt` 는 문자열로, `.feature` 는 호출 가능한 feature 로, `.js` 는 함수로 읽습니다.

---

## 5. 응답에서 원하는 값 꺼내기

이 프로젝트의 목록 응답은 다음과 같다고 해봅시다.

```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "Alice" },
    { "id": 2, "name": "Bob" }
  ],
  "error": null
}
```

```gherkin
# 점(.) 과 인덱스로 접근
* def first = response.data[0]           # { id: 1, name: 'Alice' }
* def firstName = response.data[0].name  # 'Alice'
* def count = response.data.length       # 2

# [*] 로 모든 원소의 특정 필드 모으기 → get 키워드 사용
* def ids = get response.data[*].id      # [1, 2]

# 모은 결과 중 첫 번째만
* def firstId = get[0] response.data[*].id   # 1

# 조건으로 찾기 (JsonPath 필터)
* def bobs = karate.jsonPath(response, "$.data[?(@.name=='Bob')]")
* match bobs[0].id == 2
```

> `[*]` 같은 와일드카드는 `def` 에서 그냥 쓸 수 없고 `get` 키워드를 붙여야 합니다.
> 반면 `match` 왼쪽에서는 `match response.data[*].id contains 2` 처럼 바로 쓸 수 있습니다.

---

## 6. `match` 완전 정복

`match` 는 Karate 의 가장 강력한 기능입니다. 연산자별로 정리해 보겠습니다.

### 6-1. 연산자 한눈에 보기

| 연산자 | 의미 | 예시 |
|--------|------|------|
| `==` | 완전히 같음 | `match user == { name: 'Alice' }` |
| `!=` | 다름 | `match user.name != 'Bob'` |
| `contains` | 일부를 포함 | `match user contains { name: 'Alice' }` |
| `!contains` | 포함하지 않음 | `match list !contains 7` |
| `contains only` | 같은 원소, 순서 무관 | `match [1,2,3] contains only [3,2,1]` |
| `contains any` | 하나라도 포함 | `match [1,2,3] contains any [9,1]` |
| `contains deep` | 중첩 객체까지 부분 포함 | `match x contains deep { a: { b: 1 } }` |
| `match each` | 배열의 모든 원소 검사 | `match each list == '#number'` |

### 6-2. `==` 는 "정확히 같다" 입니다

초보자가 가장 많이 실수하는 부분입니다.

```gherkin
* def user = { id: 1, name: 'Alice', email: 'a@b.com' }

* match user == { name: 'Alice' }                  # 실패! id, email 이 더 있음
* match user contains { name: 'Alice' }            # 성공
* match user == { id: '#number', name: 'Alice', email: '#string' }   # 성공
```

- **응답 전체 모양을 고정하고 싶다** → `==` + 마커(`#number` 등)
- **관심 있는 필드만 확인하고 싶다** → `contains`

### 6-3. 배열 검증

```gherkin
* def list = [1, 2, 3]

* match list == [1, 2, 3]                # 순서까지 같아야 함
* match list contains 2                  # 원소 하나 포함
* match list contains [3, 1]             # 여러 원소 포함 (순서 무관)
* match list contains only [3, 2, 1]     # 정확히 이 원소들만 (순서 무관)
* match list contains any [9, 1]         # 하나라도 있으면 성공
* match list !contains 7                 # 없어야 성공

# 개수 / 타입 검증
* match list == '#[3]'                   # 원소가 정확히 3개
* match list == '#[_ > 0]'               # 원소가 1개 이상 (_ 는 배열 길이)
* match list == '#[] #number'            # 모든 원소가 숫자인 배열
* match list == '#[3] #number'           # 숫자 3개짜리 배열
```

### 6-4. `match each` 로 모든 원소 검사

```gherkin
* def users = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }]

* match each users == { id: '#number', name: '#string' }
* match each users contains { id: '#number' }
```

목록 API 응답의 모든 항목이 같은 모양인지 검증할 때 씁니다.

### 6-5. 마커 심화

01 문서의 기본 마커 표에 더해, 실무에서 자주 쓰는 마커들입니다.

| 마커 | 의미 |
|------|------|
| `##string` | 문자열이거나, null 이거나, 키가 없어도 됨 (선택 필드) |
| `##null` | null 이거나 키가 없음 |
| `#ignore` | 이 필드는 검사하지 않음 |
| `#uuid` | UUID 형식 문자열 |
| `#regex [a-z]+` | 정규식과 일치하는 문자열 |
| `#? _ > 0` | JS 조건식이 참 (`_` 는 현재 값) |
| `#? _ > $.min` | 같은 객체의 다른 필드와 비교 (`$` 는 현재 객체) |

```gherkin
# 선택 필드
* match response == { success: true, data: '#object', error: '##null' }

# 범위 검사
* match response.data.age == '#? _ >= 0 && _ <= 150'

# 필드 간 비교
* def range = { min: 1, max: 5 }
* match range == { min: '#number', max: '#? _ > $.min' }
```

`#` 하나는 "반드시 있어야 함", `##` 두 개는 "없어도 됨"이라고 기억하면 쉽습니다.

### 6-6. 스키마를 변수로 재사용하기

같은 모양을 여러 번 검증한다면 변수로 빼두세요.

```gherkin
Background:
  * url baseUrl
  * def userSchema = { id: '#number', name: '#string', email: '#string', createdAt: '#string' }

Scenario: 단건 조회
  Given path '/api/users', 1
  When method GET
  Then status 200
  And match response.data == userSchema

Scenario: 목록 조회
  Given path '/api/users'
  When method GET
  Then status 200
  And match each response.data == userSchema
```

### 6-7. 응답 헤더 검증

```gherkin
And match header Content-Type contains 'application/json'
```

헤더 이름은 대소문자를 구분하지 않습니다.

### 6-8. `match` vs `assert`

```gherkin
* assert age > 18                         # 참/거짓만 판단
* match age == '#? _ > 18'                # 같은 의미지만 실패 메시지가 더 자세함
```

`assert` 는 실패 시 "조건식이 참이 아니다" 정도만 알려줍니다.
가능하면 **`match` 를 우선 사용**하세요. 실제 값과 기대 값을 함께 보여줘서 디버깅이 쉽습니다.

---

## 7. JavaScript 함께 쓰기

### 7-1. 함수 정의와 호출

```gherkin
* def double = function(x){ return x * 2 }

* def r1 = double(3)                      # 6 (일반 호출)
* def r2 = call double 3                  # 6 (call 키워드로 호출)
```

### 7-2. 자주 쓰는 `karate` 내장 함수

| 함수 | 설명 | 예시 |
|------|------|------|
| `karate.log(...)` | 로그 출력 | `karate.log('id =', id)` |
| `karate.filter(list, fn)` | 조건에 맞는 원소만 | `karate.filter([1,2,3,4], function(x){ return x % 2 == 0 })` → `[2,4]` |
| `karate.map(list, fn)` | 원소 변환 | `karate.map(users, function(x){ return x.name })` |
| `karate.repeat(n, fn)` | n 번 반복해 배열 생성 | `karate.repeat(3, function(i){ return i * 10 })` → `[0,10,20]` |
| `karate.sizeOf(x)` | 배열/객체 크기 | `karate.sizeOf({ a: 1, b: 2 })` → `2` |
| `karate.keysOf(x)` | 객체 키 목록 | `karate.keysOf({ a: 1 })` → `['a']` |
| `karate.jsonPath(x, path)` | JsonPath 조회 | `karate.jsonPath(response, '$.data[0].id')` |
| `karate.fail(msg)` | 테스트를 즉시 실패시킴 | `karate.fail('예상치 못한 상태')` |

### 7-3. 조건 분기

Karate 는 "테스트는 단순해야 한다"는 철학이라 `if` 블록 문법이 없습니다.
꼭 필요하면 JS 한 줄로 표현합니다.

```gherkin
* def code = responseStatus
* if (code == 404) karate.log('사용자가 없습니다')
* def label = code == 200 ? 'OK' : 'FAIL'
```

---

## 8. Java 함께 쓰기

JVM 위에서 돌기 때문에 Java 클래스를 바로 쓸 수 있습니다.

```gherkin
# 방법 1: 패키지 전체 경로로 바로 사용 (이 프로젝트 post.feature 방식)
* def email = 'user-' + java.util.UUID.randomUUID() + '@example.com'

# 방법 2: Java.type 으로 클래스를 변수에 담아 재사용
* def UUID = Java.type('java.util.UUID')
* def id = UUID.randomUUID() + ''         # + '' 로 문자열 변환
* match id == '#uuid'

# 현재 시각
* def now = java.time.LocalDateTime.now() + ''
```

> Java 객체는 JSON 이 아니므로 `match` 전에 `+ ''` 로 문자열로 바꿔주는 습관을 들이세요.

---

## 9. 초보자가 자주 하는 실수

| 증상 | 원인 | 해결 |
|------|------|------|
| 응답이 맞는 것 같은데 `match ==` 가 실패 | `==` 는 전체 일치 비교라 응답에 필드가 더 있으면 실패 | 일부만 확인하려면 `contains` 사용 |
| 두 번째 요청이 엉뚱한 경로로 감 | `method` 후 `path` 가 초기화됨 | 요청마다 `path` 를 다시 작성 |
| `table` 에서 "xxx is not defined" | 문자열에 따옴표 누락 | `'Alice'` 처럼 따옴표 추가 |
| Scenario B 에서 변수가 undefined | Scenario 간 변수 공유 안 됨 | `Background` 로 옮기거나 `call` 사용 |
| `def x = response.data[*].id` 에러 | 와일드카드는 `get` 필요 | `def x = get response.data[*].id` |
| 원본 JSON 이 함께 바뀜 | `def` 는 같은 객체를 참조 | `copy` 로 깊은 복사 |
| 실패 원인을 모르겠음 | 실제 응답을 확인하지 않음 | `* print response` 로 응답 전체 출력 (05 문서 참고) |

---

| | |
|---|---|
| 이전: [← 03. 테스트 작성 가이드](03-writing-tests.md) | 다음: [05. Karate 실전 사용법 →](05-karate-practical-guide.md) |
