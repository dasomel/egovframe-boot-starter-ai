package org.egovframe.boot.ai.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.egovframe.boot.ai.audit.EgovAiAuditLogAdvisor;
import org.egovframe.boot.ai.pii.EgovPiiMaskingAdvisor;
import org.egovframe.boot.ai.fallback.EgovLlmFallbackAdvisor;
import org.egovframe.boot.ai.safeguard.EgovAiSafeGuardAdvisor;
import org.egovframe.boot.ai.usage.EgovTokenUsageAdvisor;
import org.egovframe.boot.ai.prompt.EgovPromptTemplateManager;
import org.egovframe.boot.ai.trace.EgovAiTraceAdvisor;
import org.egovframe.boot.ai.trace.EgovAiTraceLogFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class EgovAiAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(EgovAiAutoConfiguration.class));

    @Test void registersAllAdvisorsByDefault() {
        runner.run(ctx -> assertThat(ctx)
            .hasSingleBean(EgovPiiMaskingAdvisor.class)
            .hasSingleBean(EgovLlmFallbackAdvisor.class)
            .hasSingleBean(EgovTokenUsageAdvisor.class)
            .hasSingleBean(EgovAiTraceAdvisor.class)
            .hasSingleBean(EgovAiTraceLogFormatter.class)
            .hasSingleBean(EgovPromptTemplateManager.class)
            .hasSingleBean(EgovAiSafeGuardAdvisor.class)
            .hasSingleBean(EgovAiAuditLogAdvisor.class)
            .hasSingleBean(EgovAiChatClientCustomizer.class));
    }

    @Test void disablesPiiWhenPropertyFalse() {
        runner.withPropertyValues("egovframe.ai.pii-masking.enabled=false")
            .run(ctx -> assertThat(ctx)
                .doesNotHaveBean(EgovPiiMaskingAdvisor.class)
                .hasSingleBean(EgovLlmFallbackAdvisor.class));
    }

    @Test void disablesTraceAdvisorWhenPropertyFalse() {
        runner.withPropertyValues("egovframe.ai.trace.enabled=false")
            .run(ctx -> assertThat(ctx)
                .doesNotHaveBean(EgovAiTraceAdvisor.class)
                .doesNotHaveBean(EgovAiTraceLogFormatter.class)
                .hasSingleBean(EgovPiiMaskingAdvisor.class));
    }

    @Test void disablesPromptTemplateManagerWhenPropertyFalse() {
        runner.withPropertyValues("egovframe.ai.prompt-template.enabled=false")
            .run(ctx -> assertThat(ctx)
                .doesNotHaveBean(EgovPromptTemplateManager.class)
                .hasSingleBean(EgovAiTraceAdvisor.class));
    }

    @Test void disablesSafeGuardWhenPropertyFalse() {
        runner.withPropertyValues("egovframe.ai.safeguard.enabled=false")
            .run(ctx -> assertThat(ctx)
                .doesNotHaveBean(EgovAiSafeGuardAdvisor.class)
                .hasSingleBean(EgovPiiMaskingAdvisor.class));
    }

    @Test void disablesAuditLogWhenPropertyFalse() {
        runner.withPropertyValues("egovframe.ai.audit.enabled=false")
            .run(ctx -> assertThat(ctx)
                .doesNotHaveBean(EgovAiAuditLogAdvisor.class)
                .hasSingleBean(EgovLlmFallbackAdvisor.class));
    }

    @Test void traceAdvisorOrderIsLowerThanPii() {
        runner.run(ctx -> {
            EgovAiTraceAdvisor trace = ctx.getBean(EgovAiTraceAdvisor.class);
            EgovPiiMaskingAdvisor pii = ctx.getBean(EgovPiiMaskingAdvisor.class);
            assertThat(trace.getOrder()).isLessThan(pii.getOrder());
        });
    }

    @Test void safeGuardOrderIsBetweenTraceAndPii() {
        runner.run(ctx -> {
            EgovAiTraceAdvisor trace = ctx.getBean(EgovAiTraceAdvisor.class);
            EgovAiSafeGuardAdvisor sg = ctx.getBean(EgovAiSafeGuardAdvisor.class);
            EgovPiiMaskingAdvisor pii = ctx.getBean(EgovPiiMaskingAdvisor.class);
            assertThat(sg.getOrder()).isGreaterThan(trace.getOrder());
            assertThat(sg.getOrder()).isLessThan(pii.getOrder());
        });
    }

    @Test void auditLogOrderIsBetweenFallbackAndUsage() {
        runner.run(ctx -> {
            EgovLlmFallbackAdvisor fallback = ctx.getBean(EgovLlmFallbackAdvisor.class);
            EgovAiAuditLogAdvisor audit = ctx.getBean(EgovAiAuditLogAdvisor.class);
            EgovTokenUsageAdvisor usage = ctx.getBean(EgovTokenUsageAdvisor.class);
            assertThat(audit.getOrder()).isGreaterThan(fallback.getOrder());
            assertThat(audit.getOrder()).isLessThan(usage.getOrder());
        });
    }
}
