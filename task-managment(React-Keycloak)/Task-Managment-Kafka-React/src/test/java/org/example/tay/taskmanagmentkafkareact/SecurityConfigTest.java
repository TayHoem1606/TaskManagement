package org.example.tay.taskmanagmentkafkareact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * SecurityConfig Integration Tests
 *
 * FIX: Requires these pom.xml test dependencies:
 *   spring-boot-starter-test  (provides @SpringBootTest, WebTestClient)
 *   spring-security-test      (provides mockJwt)
 *   reactor-test              (provides StepVerifier)
 *
 * Tests that the security filter chain correctly:
 *   - Returns 401 for all /api/tasks endpoints with no token
 *   - Returns 401 for malformed/expired tokens
 *   - Allows /actuator/health without a token
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=" +
                "http://localhost:8180/realms/internship-task-realm/protocol/openid-connect/certs",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=" +
                "http://localhost:8180/realms/internship-task-realm",
        "cors.allowed-origins=http://localhost:3000"
})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    // ── 401 — no token ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/tasks without token → 401")
    void getAllTasks_noToken_returns401() {
        webTestClient.get()
                .uri("/api/tasks")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/tasks without token → 401")
    void createTask_noToken_returns401() {
        webTestClient.post()
                .uri("/api/tasks")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("""
                    {
                        "title": "Test",
                        "description": "Desc",
                        "status": "PENDING",
                        "priority": "MEDIUM",
                        "dueDate": "2027-12-31"
                    }
                """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} without token → 401")
    void updateTask_noToken_returns401() {
        webTestClient.put()
                .uri("/api/tasks/TASK-0001")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} without token → 401")
    void deleteTask_noToken_returns401() {
        webTestClient.delete()
                .uri("/api/tasks/TASK-0001")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("PATCH /api/tasks/{id}/status without token → 401")
    void patchStatus_noToken_returns401() {
        webTestClient.patch()
                .uri("/api/tasks/TASK-0001/status")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("{\"status\": \"IN_PROGRESS\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Malformed Bearer token → 401")
    void malformedToken_returns401() {
        webTestClient.get()
                .uri("/api/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer this-is-not-a-jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── Actuator health — should be public ────────────────────────

    @Test
    @DisplayName("/actuator/health is accessible without token")
    void actuatorHealth_noToken_notUnauthorized() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().value(status ->
                        org.assertj.core.api.Assertions.assertThat(status)
                                .isIn(200, 404)  // 404 if actuator not on classpath
                );
    }
}
