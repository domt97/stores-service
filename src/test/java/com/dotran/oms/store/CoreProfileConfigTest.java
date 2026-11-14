package com.dotran.oms.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * application.yaml includes the profile {@code core}, so oms-core's {@code application-core.yaml} supplies the settings
 * every OMS service shares. This test loads the configuration files the way Spring Boot does (no beans, no database) and
 * fails if the profile is no longer included or oms-core stops shipping the file: the AWS client settings below are
 * defined only there.
 */
class CoreProfileConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    @DisplayName("the core profile is included and supplies the shared settings")
    void coreProfileSuppliesSharedSettings() {
        runner.run(context -> {
            Environment env = context.getEnvironment();
            assertThat(env.getActiveProfiles()).contains("core");
            // only in oms-core's application-core.yaml
            assertThat(env.getProperty("springdoc.api-docs.path")).isEqualTo("/api-docs");
            assertThat(env.getProperty("aws.region")).isNotBlank();
            assertThat(env.getProperty("aws.endpoint")).isNotBlank();
            assertThat(env.getProperty("aws.access-key")).isNotBlank();
            assertThat(env.getProperty("aws.secret-key")).isNotBlank();
        });
    }

    @Test
    @DisplayName("the service's own settings are still read from application.yaml")
    void serviceSettingsAreKept() {
        runner.run(context -> {
            Environment env = context.getEnvironment();
            assertThat(env.getProperty("spring.application.name")).isEqualTo("stores-service");
            assertThat(env.getProperty("aws.s3.store-product-bucket")).isNotBlank();
        });
    }
}
