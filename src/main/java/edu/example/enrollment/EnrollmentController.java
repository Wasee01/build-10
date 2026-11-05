package edu.example.enrollment;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService service;
    private final SectionRepository sections;
    private final StudentRepository students;

    public EnrollmentController(EnrollmentService service, SectionRepository sections, StudentRepository students) {
        this.service = service;
        this.sections = sections;
        this.students = students;
    }

    @GetMapping("/students")
    public List<Student> students() {
        return students.findAll();
    }

    @GetMapping("/sections")
    public List<Section> sections() {
        return sections.findAll();
    }

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentReceipt enroll(@Valid @RequestBody EnrollmentRequest request) {
        Enrollment result = service.enroll(request.studentId(), request.sectionId());
        return new EnrollmentReceipt(result.getId(), result.getStatus());
    }

    public record EnrollmentRequest(@NotNull @Positive Long studentId, @NotNull @Positive Long sectionId) { }

    public record EnrollmentReceipt(Long id, Enrollment.Status status) { }
}
