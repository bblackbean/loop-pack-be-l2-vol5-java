package com.loopers.interfaces.api;

import com.loopers.domain.example.ExampleModel;
import com.loopers.infrastructure.example.ExampleJpaRepository;
import com.loopers.interfaces.api.example.ExampleV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 기존 /api/v1/examples API 가 네 가지 입력을 어떻게 구분해 응답하는지 관찰한 기록.
 * 각 입력마다 status / meta.result / errorCode / data 유무를 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractClassificationTest {

    private static final ParameterizedTypeReference<ApiResponse<ExampleV1Dto.ExampleResponse>> RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {};

    private final TestRestTemplate testRestTemplate;
    private final ExampleJpaRepository exampleJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public ContractClassificationTest(
        TestRestTemplate testRestTemplate,
        ExampleJpaRepository exampleJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.exampleJpaRepository = exampleJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private ResponseEntity<ApiResponse<ExampleV1Dto.ExampleResponse>> get(String url) {
        return testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), RESPONSE_TYPE);
    }

    @DisplayName("1. 존재하는 숫자 ID: 200 OK / SUCCESS / errorCode 없음 / data 있음")
    @Test
    void existingNumericId() {
        // arrange
        ExampleModel saved = exampleJpaRepository.save(new ExampleModel("예시 제목", "예시 설명"));

        // act
        var response = get("/api/v1/examples/" + saved.getId());

        // assert
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody().meta().result())
                .isEqualTo(ApiResponse.Metadata.Result.SUCCESS),
            () -> assertThat(response.getBody().meta().errorCode()).isNull(),
            () -> assertThat(response.getBody().data()).isNotNull(),
            () -> assertThat(response.getBody().data().id()).isEqualTo(saved.getId())
        );
    }

    @DisplayName("2. 숫자가 아닌 ID: 400 BAD_REQUEST / FAIL / Bad Request / data 없음")
    @Test
    void nonNumericId() {
        var response = get("/api/v1/examples/abc");

        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
            () -> assertThat(response.getBody().meta().result())
                .isEqualTo(ApiResponse.Metadata.Result.FAIL),
            () -> assertThat(response.getBody().meta().errorCode()).isEqualTo("Bad Request"),
            () -> assertThat(response.getBody().data()).isNull()
        );
    }

    @DisplayName("3. 존재하지 않는 숫자 ID: 404 NOT_FOUND / FAIL / Not Found / data 없음")
    @Test
    void notExistingNumericId() {
        var response = get("/api/v1/examples/-1");

        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
            () -> assertThat(response.getBody().meta().result())
                .isEqualTo(ApiResponse.Metadata.Result.FAIL),
            () -> assertThat(response.getBody().meta().errorCode()).isEqualTo("Not Found"),
            () -> assertThat(response.getBody().data()).isNull()
        );
    }

    @DisplayName("4. 매핑되지 않은 URL: 404 NOT_FOUND / FAIL / Not Found / data 없음")
    @Test
    void notMappedUrl() {
        var response = get("/api/v1/not-mapped-at-all");

        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
            () -> assertThat(response.getBody().meta().result())
                .isEqualTo(ApiResponse.Metadata.Result.FAIL),
            () -> assertThat(response.getBody().meta().errorCode()).isEqualTo("Not Found"),
            () -> assertThat(response.getBody().data()).isNull()
        );
    }
}
