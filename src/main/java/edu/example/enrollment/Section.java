package edu.example.enrollment;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "sections")
public class Section {

    @Id
    private Long id;
    private String termCode;
    private String title;
    private int creditHours;
    private int capacity;
    private Instant registrationOpensAt;
    private Instant registrationClosesAt;

    @Version
    private long version;

    protected Section() {
    }

    public Long getId() {
        return id;
    }

    public String getTermCode() {
        return termCode;
    }

    public String getTitle() {
        return title;
    }

    public int getCreditHours() {
        return creditHours;
    }

    public int getCapacity() {
        return capacity;
    }

    public Instant getRegistrationOpensAt() {
        return registrationOpensAt;
    }

    public Instant getRegistrationClosesAt() {
        return registrationClosesAt;
    }

}
