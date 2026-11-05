package edu.example.enrollment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DevIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private StudentRepository students;

    @Autowired
    private EnrollmentService service;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Test
    void flywaySeedsAndHibernateValidates() {
        assertThat(students.count()).isEqualTo(3);
        assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("true");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    void enrollsThenWaitlistsAndRejectsDuplicate() {
        assertThat(service.enroll(1L, 1L).getStatus()).isEqualTo(Enrollment.Status.ENROLLED);
        assertThat(service.enroll(2L, 1L).getStatus()).isEqualTo(Enrollment.Status.WAITLISTED);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.enroll(1L, 1L))
                .hasMessageContaining("already has");
    }

    @Test
    void onlyApprovedActuatorEndpointsAreReachable() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String endpoint : new String[] {"health", "info", "metrics"}) {
                assertThat(get(client, endpoint).statusCode()).isEqualTo(200);
            }
            assertThat(get(client, "health").body()).contains("UP");
            for (String endpoint : new String[] {"env", "beans", "configprops", "loggers", "mappings", "heapdump"}) {
                assertThat(get(client, endpoint).statusCode()).isIn(403, 404);
            }
        }
    }

    private HttpResponse<String> get(HttpClient client, String endpoint) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/" + endpoint))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
