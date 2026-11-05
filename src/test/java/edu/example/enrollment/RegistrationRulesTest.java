package edu.example.enrollment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationRulesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(BindingConfiguration.class)
            .withPropertyValues(
                    "registration.rules.max-credit-hours-per-term=15",
                    "registration.rules.waitlist-size=4",
                    "registration.rules.registration-window-grace-period=PT6H",
                    "registration.rules.registrar-contact-address=registrar@example.edu");

    @Test
    void bindsRecord() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RegistrationRules.class).waitlistSize()).isEqualTo(4);
        });
    }

    @Test
    void invalidValuesNameTheProperty() {
        for (String property : new String[] {
                "waitlist-size=-1", "max-credit-hours-per-term=0",
                "registration-window-grace-period=-PT1H", "registrar-contact-address=invalid" }) {
            runner.withPropertyValues("registration.rules." + property).run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining(
                        property.substring(0, property.indexOf('=')));
            });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RegistrationRules.class)
    static class BindingConfiguration { }
}
