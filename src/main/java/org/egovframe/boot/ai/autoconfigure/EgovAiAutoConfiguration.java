package org.egovframe.boot.ai.autoconfigure;

import org.egovframe.boot.ai.audit.EgovAiAuditLogAdvisor;
import org.egovframe.boot.ai.audit.EgovAiAuditProperties;
import org.egovframe.boot.ai.fallback.EgovLlmExceptionMapper;
import org.egovframe.boot.ai.fallback.EgovLlmFallbackAdvisor;
import org.egovframe.boot.ai.fallback.EgovLlmFallbackProperties;
import org.egovframe.boot.ai.pii.EgovPiiMasker;
import org.egovframe.boot.ai.pii.EgovPiiMaskingAdvisor;
import org.egovframe.boot.ai.pii.EgovPiiMaskingProperties;
import org.egovframe.boot.ai.prompt.EgovPromptTemplateManager;
import org.egovframe.boot.ai.prompt.EgovPromptTemplateProperties;
import org.egovframe.boot.ai.safeguard.EgovAiSafeGuardAdvisor;
import org.egovframe.boot.ai.safeguard.EgovAiSafeGuardProperties;
import org.egovframe.boot.ai.trace.EgovAiTraceAdvisor;
import org.egovframe.boot.ai.trace.EgovAiTraceLogFormatter;
import org.egovframe.boot.ai.trace.EgovAiTraceProperties;
import org.egovframe.boot.ai.usage.EgovTokenUsageAdvisor;
import org.egovframe.boot.ai.usage.EgovTokenUsageFormatter;
import org.egovframe.boot.ai.usage.EgovTokenUsageProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

/**
 * Spring Boot 자동구성 진입점. 소비 애플리케이션이 이 스타터를 의존성에 추가하기만 하면
 * 별도 {@code @Bean} 선언 없이 아래 advisor 체인 전체가 {@code ChatClient}에 배선된다
 * ({@code spring.factories}/{@code AutoConfiguration.imports}에 등록된 표준 Spring Boot 방식).
 *
 * <pre>
 * ChatClient.call()
 *     TRACE(50, 최외곽) → SAFEGUARD(75) → PII(100) → FALLBACK(200) → AUDIT(250) → USAGE(300, 최내곽)
 *     → ChatModel
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@EnableConfigurationProperties({
        EgovPiiMaskingProperties.class,
        EgovLlmFallbackProperties.class,
        EgovTokenUsageProperties.class,
        EgovPromptTemplateProperties.class,
        EgovAiTraceProperties.class,
        EgovAiSafeGuardProperties.class,
        EgovAiAuditProperties.class })
public class EgovAiAutoConfiguration {

    // advisor 순서: TRACE(최외곽) < SAFEGUARD < PII < FALLBACK < AUDIT < USAGE(최내곽)
    static final int ORDER_TRACE     = 50;
    static final int ORDER_SAFEGUARD = 75;
    static final int ORDER_PII       = 100;
    static final int ORDER_FALLBACK  = 200;
    static final int ORDER_AUDIT     = 250;
    static final int ORDER_USAGE     = 300;

    /** trace 로그 문자열 포맷터. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.trace.enabled", matchIfMissing = true)
    public EgovAiTraceLogFormatter egovAiTraceLogFormatter() {
        return new EgovAiTraceLogFormatter();
    }

    /** order=50 최외곽 trace 로깅 advisor */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.trace.enabled", matchIfMissing = true)
    public EgovAiTraceAdvisor egovAiTraceAdvisor(EgovAiTraceLogFormatter formatter,
                                                  EgovAiTraceProperties props) {
        return new EgovAiTraceAdvisor(formatter, props, ORDER_TRACE);
    }

    /** order=75에 위치할 보안 가드레일 advisor를 등록한다. 금칙어 사전 검사 및 숏서킷 차단을 수행한다. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.safeguard.enabled", matchIfMissing = true)
    public EgovAiSafeGuardAdvisor egovAiSafeGuardAdvisor(EgovAiSafeGuardProperties props) {
        return new EgovAiSafeGuardAdvisor(props.getKeywords(), props.getBlockMessage(), ORDER_SAFEGUARD);
    }

    /** order=100 PII 마스킹 advisor */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.pii-masking.enabled", matchIfMissing = true)
    public EgovPiiMaskingAdvisor egovPiiMaskingAdvisor(EgovPiiMaskingProperties props) {
        return new EgovPiiMaskingAdvisor(new EgovPiiMasker(props.resolveTypes(), props.isVerifyCheckdigit()), ORDER_PII);
    }

    /** order=200 예외 분류·폴백 advisor */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.fallback.enabled", matchIfMissing = true)
    public EgovLlmFallbackAdvisor egovLlmFallbackAdvisor(EgovLlmFallbackProperties props) {
        return new EgovLlmFallbackAdvisor(new EgovLlmExceptionMapper(),
                props.isReturnFallback(), props.getFallbackMessage(), ORDER_FALLBACK);
    }

    /** order=250 감사 로그 및 이벤트 발행 advisor */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.audit.enabled", matchIfMissing = true)
    public EgovAiAuditLogAdvisor egovAiAuditLogAdvisor(ApplicationEventPublisher publisher,
                                                        EgovAiAuditProperties props) {
        return new EgovAiAuditLogAdvisor(publisher, props.isLogToConsole(), ORDER_AUDIT);
    }

    /** order=300 토큰 사용량 로깅 advisor */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.token-usage.enabled", matchIfMissing = true)
    public EgovTokenUsageAdvisor egovTokenUsageAdvisor(EgovTokenUsageProperties props) {
        return new EgovTokenUsageAdvisor(new EgovTokenUsageFormatter(), ORDER_USAGE);
    }

    /** 프롬프트 템플릿 매니저 */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.prompt-template.enabled", matchIfMissing = true)
    public EgovPromptTemplateManager egovPromptTemplateManager(ResourceLoader resourceLoader,
                                                               EgovPromptTemplateProperties props) {
        return new EgovPromptTemplateManager(resourceLoader, props);
    }

    /** ChatClient.Builder 커스터마이저 */
    @Bean
    public EgovAiChatClientCustomizer egovAiChatClientCustomizer(List<Advisor> advisors) {
        return new EgovAiChatClientCustomizer(advisors);
    }
}
