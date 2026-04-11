Feature: Post API
  # Karate BDD 실습: Post CRUD API 테스트
  # Post 는 User(Author)가 먼저 존재해야 생성 가능합니다.
  # Background 에서 테스트용 사용자를 미리 생성합니다.

  Background:
    * url baseUrl

    # UUID 로 고유 이메일 생성 → 각 Scenario 마다 독립적인 사용자 생성
    * def randomEmail = 'author-' + java.util.UUID.randomUUID() + '@example.com'

    Given path '/api/users'
    And request { name: 'PostAuthor', email: '#(randomEmail)' }
    When method POST
    Then status 201
    # authorId 는 숫자 타입 그대로 보존
    * def authorId = response.data.id

  # ──────────────────────────────────────────────
  # 1. 전체 CRUD 흐름
  # ──────────────────────────────────────────────
  Scenario: Post CRUD 전체 흐름
    # 1-1. 포스트 생성 (POST)
    # authorId 는 따옴표 없이 사용해야 JSON number 로 전송됨
    Given path '/api/posts'
    And request { title: 'Hello Karate', content: 'Karate is awesome!', authorId: #(authorId) }
    When method POST
    Then status 201
    And match response.success == true
    And match response.data.id == '#number'
    And match response.data.title == 'Hello Karate'
    And match response.data.content == 'Karate is awesome!'
    And match response.data.authorId == authorId
    And match response.data.authorName == 'PostAuthor'
    * def postId = response.data.id

    # 1-2. 단건 조회 (GET by ID)
    Given path '/api/posts', postId
    When method GET
    Then status 200
    And match response.data.id == postId
    And match response.data.title == 'Hello Karate'

    # 1-3. 수정 (PUT)
    Given path '/api/posts', postId
    And request { title: 'Updated Title', content: 'Updated content.' }
    When method PUT
    Then status 200
    And match response.data.title == 'Updated Title'
    And match response.data.content == 'Updated content.'

    # 1-4. 작성자별 조회
    Given path '/api/posts/by-author', authorId
    When method GET
    Then status 200
    And match response.data == '#array'
    And match response.data[0].authorId == authorId

    # 1-5. 삭제 (DELETE)
    Given path '/api/posts', postId
    When method DELETE
    Then status 204

    # 1-6. 삭제 후 조회 → 404
    Given path '/api/posts', postId
    When method GET
    Then status 404
    And match response.success == false

    # 사용자 정리
    Given path '/api/users', authorId
    When method DELETE
    Then status 204

  # ──────────────────────────────────────────────
  # 2. 전체 목록 조회
  # ──────────────────────────────────────────────
  Scenario: 포스트 목록 조회
    Given path '/api/posts'
    And request { title: 'List Test Post', content: 'content', authorId: #(authorId) }
    When method POST
    Then status 201
    * def postId = response.data.id

    Given path '/api/posts'
    When method GET
    Then status 200
    And match response.success == true
    And match response.data == '#array'

    # 정리
    Given path '/api/posts', postId
    When method DELETE
    Then status 204

    Given path '/api/users', authorId
    When method DELETE
    Then status 204

  # ──────────────────────────────────────────────
  # 3. 유효성 검사 — 제목 없이 생성
  # ──────────────────────────────────────────────
  Scenario: 제목 없이 포스트 생성 시 400 반환
    Given path '/api/posts'
    And request { title: '', content: 'some content', authorId: #(authorId) }
    When method POST
    Then status 400
    And match response.success == false

    # 정리
    Given path '/api/users', authorId
    When method DELETE
    Then status 204

  # ──────────────────────────────────────────────
  # 4. 존재하지 않는 작성자로 생성
  # ──────────────────────────────────────────────
  Scenario: 존재하지 않는 사용자로 포스트 생성 시 404 반환
    Given path '/api/posts'
    And request { title: 'Ghost Post', content: 'content', authorId: 999999 }
    When method POST
    Then status 404
    And match response.success == false

    # 정리
    Given path '/api/users', authorId
    When method DELETE
    Then status 204

  # ──────────────────────────────────────────────
  # 5. 존재하지 않는 포스트 조회 → 404
  # ──────────────────────────────────────────────
  Scenario: 존재하지 않는 포스트 조회 시 404 반환
    Given path '/api/posts/999999'
    When method GET
    Then status 404
    And match response.success == false

    # 정리
    Given path '/api/users', authorId
    When method DELETE
    Then status 204
