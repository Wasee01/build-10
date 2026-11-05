package edu.example.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);

    long countBySectionIdAndStatus(Long sectionId, Enrollment.Status status);

    @Query("""
            select coalesce(sum(e.section.creditHours), 0) from Enrollment e
            where e.studentId = :studentId and e.section.termCode = :termCode
            """)
    long reservedCredits(Long studentId, String termCode);
}
