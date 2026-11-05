package edu.example.enrollment;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SectionRepository extends JpaRepository<Section, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Section s where s.id = :id")
    Optional<Section> findForEnrollment(Long id);
}
