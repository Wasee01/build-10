package edu.example.enrollment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnrollmentRulesTest {

    private final SectionRepository sections = mock(SectionRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final Section section = mock(Section.class);
    private final Instant now = Instant.parse("2026-11-05T12:00:00Z");
    private final RegistrationRules rules = new RegistrationRules(7, 2, Duration.ofHours(3), "office@example.edu");
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(rules, sections, enrollments, Clock.fixed(now, ZoneOffset.UTC), students);
        when(students.findForEnrollment(1L)).thenReturn(Optional.of(mock(Student.class)));
        when(sections.findForEnrollment(1L)).thenReturn(Optional.of(section));
        when(section.getId()).thenReturn(1L);
        when(section.getTermCode()).thenReturn("TEST");
        when(section.getCreditHours()).thenReturn(3);
        when(section.getCapacity()).thenReturn(1);
        when(section.getRegistrationOpensAt()).thenReturn(now.minus(Duration.ofDays(2)));
        when(section.getRegistrationClosesAt()).thenReturn(now.minus(Duration.ofHours(2)));
        when(enrollments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creditLimitComesFromConfiguration() {
        when(enrollments.reservedCredits(1L, "TEST")).thenReturn(5L);
        assertThatThrownBy(() -> service.enroll(1L, 1L))
                .hasMessageContaining("credit limit").hasMessageContaining(rules.registrarContactAddress());
    }

    @Test
    void configuredGracePeriodAllowsRegistration() {
        org.assertj.core.api.Assertions.assertThat(service.enroll(1L, 1L).getStatus())
                .isEqualTo(Enrollment.Status.ENROLLED);
    }

    @Test
    void registrationAfterConfiguredGraceIsRejected() {
        when(section.getRegistrationClosesAt()).thenReturn(now.minus(Duration.ofHours(4)));
        assertThatThrownBy(() -> service.enroll(1L, 1L)).hasMessageContaining("window is closed");
    }

    @Test
    void configuredWaitlistCapacityIsEnforced() {
        when(enrollments.countBySectionIdAndStatus(1L, Enrollment.Status.ENROLLED)).thenReturn(1L);
        when(enrollments.countBySectionIdAndStatus(1L, Enrollment.Status.WAITLISTED)).thenReturn(2L);
        assertThatThrownBy(() -> service.enroll(1L, 1L)).hasMessageContaining("waitlist are full");
    }
}
