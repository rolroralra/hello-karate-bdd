# 02. 프로젝트 구조 설명

## 전체 디렉토리 구조

```
hello-karate-bdd/
│
├── compose.yaml                              # Docker Compose 설정 (PostgreSQL 17)
├── build.gradle.kts                          # Gradle Kotlin DSL 빌드 스크립트
├── settings.gradle.kts                       # 프로젝트 이름 설정
├── gradle/wrapper/gradle-wrapper.properties  # Gradle 래퍼 설정 (8.13)
│
├── README.md                                 # 프로젝트 개요 및 빠른 시작
│
├── docs/                                     # 상세 문서
│   ├── 01-karate-overview.md                 # Karate 개요 및 문법 참조
│   ├── 02-project-structure.md              # 이 파일 (프로젝트 구조)
│   ├── 03-writing-tests.md                  # 테스트 작성 가이드
│   ├── 04-karate-syntax-guide.md            # Karate 문법 친절 가이드
│   └── 05-karate-practical-guide.md         # Karate 실전 사용법
│
├── http/                                     # IntelliJ IDEA HTTP 클라이언트 파일
│   ├── users.http                            # User API 수동 테스트
│   └── posts.http                            # Post API 수동 테스트
│
└── src/
    ├── main/
    │   ├── java/com/example/karatebdd/
    │   │   ├── KarateBddApplication.java     # Spring Boot 진입점
    │   │   │
    │   │   ├── common/                       # 공통 모듈
    │   │   │   ├── ApiResponse.java          # 통합 API 응답 래퍼 (Record)
    │   │   │   └── GlobalExceptionHandler.java  # 전역 예외 처리
    │   │   │
    │   │   ├── user/                         # User 도메인
    │   │   │   ├── User.java                 # JPA 엔티티
    │   │   │   ├── UserRepository.java       # Spring Data JPA 리포지토리
    │   │   │   ├── UserService.java          # 비즈니스 로직
    │   │   │   ├── UserController.java       # REST 컨트롤러
    │   │   │   ├── CreateUserRequest.java    # 생성 요청 DTO (Record)
    │   │   │   ├── UpdateUserRequest.java    # 수정 요청 DTO (Record)
    │   │   │   └── UserResponse.java         # 응답 DTO (Record)
    │   │   │
    │   │   └── post/                         # Post 도메인
    │   │       ├── Post.java                 # JPA 엔티티 (User FK)
    │   │       ├── PostRepository.java
    │   │       ├── PostService.java
    │   │       ├── PostController.java
    │   │       ├── CreatePostRequest.java    # Record
    │   │       ├── UpdatePostRequest.java    # Record
    │   │       └── PostResponse.java         # Record
    │   │
    │   └── resources/
    │       └── application.yml               # 메인 설정 (PostgreSQL, JPA)
    │
    └── test/
        ├── java/com/example/karatebdd/
        │   ├── user/
        │   │   └── UserApiTest.java           # Karate JUnit5 러너 (User)
        │   └── post/
        │       └── PostApiTest.java           # Karate JUnit5 러너 (Post)
        │
        └── resources/
            ├── karate-config.js               # Karate 전역 설정 (baseUrl 등)
            ├── application-test.yml           # 테스트 프로파일 (H2, Docker Compose 비활성)
            └── features/
                ├── user/
                │   └── user.feature           # User API BDD 시나리오
                └── post/
                    └── post.feature           # Post API BDD 시나리오
```

---

## 주요 파일 상세 설명

### build.gradle.kts

```kotlin
dependencies {
    // 메인 의존성
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // 개발 시에만 활성화 — compose.yaml 의 PostgreSQL 자동 시작
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    runtimeOnly("org.postgresql:postgresql")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testImplementation("io.karatelabs:karate-junit5:1.4.1")
}
```

### compose.yaml

Spring Boot Docker Compose Support 가 자동으로 감지하는 파일입니다.
`./gradlew bootRun` 또는 IDE 에서 실행 시 Docker 가 자동으로 PostgreSQL 을 시작합니다.

```yaml
services:
  postgres:
    image: 'postgres:17'
    environment:
      - 'POSTGRES_USER=karate'
      - 'POSTGRES_PASSWORD=karate'
      - 'POSTGRES_DB=karate_bdd'
    ports:
      - '5432:5432'
```

### application-test.yml

테스트 프로파일에서 PostgreSQL 대신 H2 를 사용하고 Docker Compose 를 비활성화합니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  docker:
    compose:
      enabled: false
```

---

## API 응답 형식

모든 API 응답은 `ApiResponse<T>` 로 래핑됩니다.

**성공 응답:**
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

**에러 응답:**
```json
{
  "success": false,
  "data": null,
  "error": "에러 메시지"
}
```

**HTTP 상태 코드 정책:**

| 상황 | 상태 코드 |
|------|----------|
| 성공 조회/수정 | 200 OK |
| 성공 생성 | 201 Created |
| 성공 삭제 | 204 No Content |
| 유효성 검사 실패 | 400 Bad Request |
| 리소스 없음 | 404 Not Found |
| 비즈니스 규칙 위반 (중복 등) | 400 Bad Request |

---

## 도메인 모델

### User

```
users 테이블
- id (PK, IDENTITY)
- name (NOT NULL)
- email (NOT NULL, UNIQUE)
- created_at (NOT NULL)
```

### Post

```
posts 테이블
- id (PK, IDENTITY)
- title (NOT NULL)
- content (TEXT)
- author_id (FK → users.id, NOT NULL)
- created_at (NOT NULL)
```

---

| | |
|---|---|
| 이전: [← 01. Karate 개요](01-karate-overview.md) | 다음: [03. 테스트 작성 가이드 →](03-writing-tests.md) |
