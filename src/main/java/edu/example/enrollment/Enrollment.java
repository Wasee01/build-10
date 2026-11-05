package edu.example.enrollment;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    public enum Status { ENROLLED, WAITLISTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id")
    private Section section;

    @Enumerated(EnumType.STRING)
    private Status status;

    protected Enrollment() {
    }

    public Enrollment(Long studentId, Section section, Status status) {
        this.studentId = studentId;
        this.section = section;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

}
