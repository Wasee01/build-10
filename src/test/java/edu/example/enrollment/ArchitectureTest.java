package edu.example.enrollment;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {

    @Test
    void enrollmentServiceHasNoNumericBusinessLiterals() throws Exception {
        String source = Files.readString(Path.of("src/main/java/edu/example/enrollment/EnrollmentService.java"));
        assertThat(source).doesNotContainPattern("\\b[0-9]+[LlDdFf]?\\b");
        assertThat(source).contains("rules.maxCreditHoursPerTerm()", "rules.waitlistSize()",
                "rules.registrationWindowGracePeriod()", "rules.registrarContactAddress()");
        assertThat(RegistrationRules.class.isRecord()).isTrue();
    }
}
