package edu.example.enrollment;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration(proxyBeanMethods = false)
public class ProfileConfiguration {

    public ProfileConfiguration(Environment environment) {
        boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        if (dev == prod) {
            throw new IllegalStateException("Activate exactly one deployment profile: dev or prod.");
        }
    }
}
