# Hello Karate BDD

Spring Boot 4 + Karate BDD 를 활용한 REST API 테스트 실습 프로젝트입니다.

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 4.0.3 |
| Gradle | 8.13 (Kotlin DSL) |
| Karate | 1.4.1 |
| PostgreSQL | 17 (Docker) |
| H2 | 테스트용 인메모리 DB |

## 빠른 시작

### 사전 요구 사항

- JDK 21
- Docker Desktop (PostgreSQL 컨테이너 실행용)
- IntelliJ IDEA (권장)

### Gradle Wrapper 초기화

프로젝트를 처음 받았을 때 한 번만 실행합니다.

```bash
gradle wrapper
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

Spring Boot Docker Compose Support 가 자동으로 `compose.yaml` 를 읽어 PostgreSQL 컨테이너를 시작합니다.
서버 기동 후 `http://localhost:8080` 에서 API 를 사용할 수 있습니다.

### 테스트 실행

```bash
./gradlew test
```

테스트는 H2 인메모리 DB 를 사용하므로 Docker 가 없어도 실행 가능합니다.

### 테스트 결과 확인

```
build/reports/tests/test/index.html   - JUnit HTML 리포트
build/reports/karate/               - Karate HTML 리포트 (feature 별 상세)
```

## API 엔드포인트

### Users

| Method | Path | 설명 |
|--------|------|------|
| GET | /api/users | 사용자 목록 |
| POST | /api/users | 사용자 생성 |
| GET | /api/users/{id} | 사용자 단건 조회 |
| PUT | /api/users/{id} | 사용자 수정 |
| DELETE | /api/users/{id} | 사용자 삭제 |

### Posts

| Method | Path | 설명 |
|--------|------|------|
| GET | /api/posts | 포스트 목록 |
| POST | /api/posts | 포스트 생성 |
| GET | /api/posts/{id} | 포스트 단건 조회 |
| GET | /api/posts/by-author/{authorId} | 작성자별 포스트 목록 |
| PUT | /api/posts/{id} | 포스트 수정 |
| DELETE | /api/posts/{id} | 포스트 삭제 |

## 프로젝트 구조

```
hello-karate-bdd/
├── compose.yaml                          # Docker Compose (PostgreSQL)
├── build.gradle.kts                      # Gradle 빌드 설정
├── http/                                 # IntelliJ IDEA HTTP 테스트 파일
│   ├── users.http
│   └── posts.http
├── docs/                                 # 상세 문서
│   ├── 01-karate-overview.md
│   ├── 02-project-structure.md
│   └── 03-writing-tests.md
└── src/
    ├── main/
    │   ├── java/com/example/karatebdd/
    │   │   ├── KarateBddApplication.java
    │   │   ├── common/                   # 공통 응답 형식, 예외 처리
    │   │   ├── user/                     # User 도메인
    │   │   └── post/                     # Post 도메인
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── java/com/example/karatebdd/
        │   ├── user/UserApiTest.java      # Karate JUnit5 러너
        │   └── post/PostApiTest.java
        └── resources/
            ├── karate-config.js           # Karate 전역 설정
            ├── application-test.yml       # 테스트 프로파일 (H2)
            └── features/
                ├── user/user.feature      # User BDD 시나리오
                └── post/post.feature      # Post BDD 시나리오
```

## 문서

자세한 내용은 `docs/` 디렉토리를 참고하세요.

- [01. Karate 개요](docs/01-karate-overview.md)
- [02. 프로젝트 구조 설명](docs/02-project-structure.md)
- [03. 테스트 작성 가이드](docs/03-writing-tests.md)
---

| | |
|---|---|
| | 다음: [01. Karate 개요 →](docs/01-karate-overview.md) |
