package edu.example.enrollment;

import java.time.Duration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "registration.rules")
public record RegistrationRules(
        @Min(1) @Max(30) int maxCreditHoursPerTerm,
        @Min(0) @Max(1000) int waitlistSize,
        @NotNull @DurationMin(seconds = 0) @DurationMax(days = 30)
        Duration registrationWindowGracePeriod,
        @NotBlank @Email String registrarContactAddress) {
}
