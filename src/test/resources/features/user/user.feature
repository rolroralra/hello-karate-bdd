Feature: User API
  # Karate BDD 실습: User CRUD API 테스트
  # - Gherkin 문법(Given/When/Then)으로 REST API 시나리오를 표현합니다.
  # - 각 Scenario 는 독립적으로 실행됩니다.

  Background:
    # Background 는 각 Scenario 실행 전에 공통으로 수행됩니다.
    * url baseUrl

  # ──────────────────────────────────────────────
  # 1. 전체 CRUD 흐름 (Create → Read → Update → Delete)
  # ──────────────────────────────────────────────
  Scenario: User CRUD 전체 흐름
    # 1-1. 사용자 생성 (POST)
    Given path '/api/users'
    And request { name: 'Alice', email: 'alice@example.com' }
    When method POST
    Then status 201
    And match response.success == true
    And match response.data.id == '#number'
    And match response.data.name == 'Alice'
    And match response.data.email == 'alice@example.com'
    And match response.data.createdAt == '#notnull'
    * def userId = response.data.id

    # 1-2. 단건 조회 (GET by ID)
    Given path '/api/users', userId
    When method GET
    Then status 200
    And match response.success == true
    And match response.data.id == userId
    And match response.data.name == 'Alice'

    # 1-3. 수정 (PUT)
    Given path '/api/users', userId
    And request { name: 'Alice Updated', email: 'alice.updated@example.com' }
    When method PUT
    Then status 200
    And match response.success == true
    And match response.data.name == 'Alice Updated'
    And match response.data.email == 'alice.updated@example.com'

    # 1-4. 삭제 (DELETE)
    Given path '/api/users', userId
    When method DELETE
    Then status 204

    # 1-5. 삭제 후 조회 → 404
    Given path '/api/users', userId
    When method GET
    Then status 404
    And match response.success == false
    And match response.error == '#notnull'

  # ──────────────────────────────────────────────
  # 2. 전체 목록 조회
  # ──────────────────────────────────────────────
  Scenario: 사용자 목록 조회
    # 테스트 데이터 생성
    Given path '/api/users'
    And request { name: 'Bob', email: 'bob@example.com' }
    When method POST
    Then status 201
    * def bobId = response.data.id

    # 목록 조회
    Given path '/api/users'
    When method GET
    Then status 200
    And match response.success == true
    And match response.data == '#array'
    And match response.data[*].id contains bobId

    # 정리
    Given path '/api/users', bobId
    When method DELETE
    Then status 204

  # ──────────────────────────────────────────────
  # 3. 유효성 검사 — 빈 이름으로 생성 시 400
  # ──────────────────────────────────────────────
  Scenario: 이름 없이 사용자 생성 시 400 반환
    Given path '/api/users'
    And request { name: '', email: 'noname@example.com' }
    When method POST
    Then status 400
    And match response.success == false
    And match response.error == '#notnull'

  # ──────────────────────────────────────────────
  # 4. 유효성 검사 — 잘못된 이메일 형식
  # ──────────────────────────────────────────────
  Scenario: 잘못된 이메일로 사용자 생성 시 400 반환
    Given path '/api/users'
    And request { name: 'Charlie', email: 'not-an-email' }
    When method POST
    Then status 400
    And match response.success == false

  # ──────────────────────────────────────────────
  # 5. 중복 이메일
  # ──────────────────────────────────────────────
  Scenario: 이메일 중복 시 400 반환
    # 첫 번째 사용자 생성
    Given path '/api/users'
    And request { name: 'Dave', email: 'dave@example.com' }
    When method POST
    Then status 201
    * def daveId = response.data.id

    # 동일 이메일로 두 번째 생성 시도
    Given path '/api/users'
    And request { name: 'Dave Clone', email: 'dave@example.com' }
    When method POST
    Then status 400
    And match response.success == false

    # 정리
    Given path '/api/users', daveId
    When method DELETE
    Then status 204

  # ──────────────────────────────────────────────
  # 6. 존재하지 않는 사용자 조회 → 404
  # ──────────────────────────────────────────────
  Scenario: 존재하지 않는 사용자 조회 시 404 반환
    Given path '/api/users/999999'
    When method GET
    Then status 404
    And match response.success == false
    And match response.error contains '999999'
