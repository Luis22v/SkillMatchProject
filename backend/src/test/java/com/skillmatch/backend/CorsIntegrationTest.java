package com.skillmatch.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La política CORS vive solo en SecurityConfig#corsConfigurationSource: ningún controller declara @CrossOrigin.
 * Estos tests fijan que un origen ajeno queda bloqueado y uno permitido recibe las cabeceras CORS.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void preflight_foreignOrigin_isRejected() {
        ResponseEntity<String> response = preflight("https://evil.example");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void preflight_localFrontendOrigin_isAllowedWithCredentials() {
        ResponseEntity<String> response = preflight("http://localhost:5500");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5500");
        assertThat(response.getHeaders().getAccessControlAllowCredentials()).isTrue();
    }

    @Test
    void get_foreignOrigin_isRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://evil.example");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/jobs/recent"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> preflight(String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(origin);
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type");
        return restTemplate.exchange(url("/api/jobs"), HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
