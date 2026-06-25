package org.egovframe.boot.ai.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.egovframe.boot.ai.pii.EgovPiiMaskingAdvisor;
import org.egovframe.boot.ai.fallback.EgovLlmFallbackAdvisor;
import org.egovframe.boot.ai.usage.EgovTokenUsageAdvisor;
import static org.assertj.core.api.Assertions.assertThat;

class EgovAiAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(EgovAiAutoConfiguration.class));

    @Test void registersAllAdvisorsByDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(EgovPiiMaskingAdvisor.class)
            .hasSingleBean(EgovLlmFallbackAdvisor.class)
            .hasSingleBean(EgovTokenUsageAdvisor.class)
            .hasSingleBean(EgovAiChatClientCustomizer.class));
    }

    @Test void disablesPiiWhenPropertyFalse() {
        runner.withPropertyValues("egovframe.ai.pii-masking.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(EgovPiiMaskingAdvisor.class)
                .hasSingleBean(EgovLlmFallbackAdvisor.class));
    }
}
