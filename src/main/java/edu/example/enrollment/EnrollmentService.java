package edu.example.enrollment;

import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnrollmentService {

    private final RegistrationRules rules;
    private final SectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final Clock clock;
    private final StudentRepository students;

    public EnrollmentService(RegistrationRules rules, SectionRepository sections,
            EnrollmentRepository enrollments, Clock clock, StudentRepository students) {
        this.rules = rules;
        this.sections = sections;
        this.enrollments = enrollments;
        this.clock = clock;
        this.students = students;
    }

    @Transactional
    public Enrollment enroll(Long studentId, Long sectionId) {
        // Lock the student first: concurrent requests cannot exceed their term credit limit.
        if (studentMissing(studentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student was not found.");
        }
        Section section = sections.findForEnrollment(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section was not found."));
        Instant now = clock.instant();
        if (now.isBefore(section.getRegistrationOpensAt())
                || now.isAfter(section.getRegistrationClosesAt().plus(rules.registrationWindowGracePeriod()))) {
            reject("The registration window is closed.");
        }
        if (enrollments.existsByStudentIdAndSectionId(studentId, sectionId)) {
            reject("The student already has an enrollment or waitlist reservation.");
        }
        if (enrollments.reservedCredits(studentId, section.getTermCode()) + section.getCreditHours()
                > rules.maxCreditHoursPerTerm()) {
            reject("The term credit limit has been reached.");
        }
        Enrollment.Status status = Enrollment.Status.ENROLLED;
        if (enrollments.countBySectionIdAndStatus(sectionId, status) >= section.getCapacity()) {
            if (enrollments.countBySectionIdAndStatus(sectionId, Enrollment.Status.WAITLISTED)
                    >= rules.waitlistSize()) {
                reject("The section and its waitlist are full.");
            }
            status = Enrollment.Status.WAITLISTED;
        }
        return enrollments.save(new Enrollment(studentId, section, status));
    }

    private boolean studentMissing(Long studentId) {
        return students.findForEnrollment(studentId).isEmpty();
    }

    private void reject(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                message + " Contact " + rules.registrarContactAddress());
    }
}
