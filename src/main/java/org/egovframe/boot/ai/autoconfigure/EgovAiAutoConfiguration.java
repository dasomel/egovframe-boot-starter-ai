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
import org.egovframe.boot.ai.safeguard.EgovAiSafeGuardChecker;
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
 *
 * <p>각 advisor 빈은 {@code egovframe.ai.<기능>.enabled}(기본 true)로 개별 비활성화할 수 있으며,
 * {@link ConditionalOnProperty}로 제어한다. 마지막으로 {@link EgovAiChatClientCustomizer}가
 * 이 시점까지 등록된 모든 {@link Advisor} 빈을 모아 {@code ChatClient.Builder}의 기본
 * advisor로 주입한다. 자세한 순서 결정 근거는 프로젝트 README "아키텍처" 절 참고.</p>
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
    // 낮은 값이 바깥쪽(먼저 실행)이다. 간격을 넓게 둔 이유는 향후 advisor 추가 시
    // 기존 상수를 재조정하지 않고 사잇값을 끼워 넣을 수 있도록 하기 위함이다.
    static final int ORDER_TRACE     = 50;
    static final int ORDER_SAFEGUARD = 75;
    static final int ORDER_PII       = 100;
    static final int ORDER_FALLBACK  = 200;
    static final int ORDER_AUDIT     = 250;
    static final int ORDER_USAGE     = 300;

    /** trace 로그 문자열 포맷터. {@link #egovAiTraceAdvisor}에서만 사용하는 보조 빈. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.trace.enabled", matchIfMissing = true)
    public EgovAiTraceLogFormatter egovAiTraceLogFormatter() {
        return new EgovAiTraceLogFormatter();
    }

    /** advisor 체인 최외곽(order=50)에 위치할 호출 추적 로깅 advisor를 등록한다. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.trace.enabled", matchIfMissing = true)
    public EgovAiTraceAdvisor egovAiTraceAdvisor(EgovAiTraceLogFormatter formatter,
                                                  EgovAiTraceProperties props) {
        return new EgovAiTraceAdvisor(formatter, props, ORDER_TRACE);
    }

    /** order=100에 위치할 PII 마스킹 advisor를 등록한다. 활성 유형·체크디지트 검증 여부는 설정에서 읽는다. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.pii-masking.enabled", matchIfMissing = true)
    public EgovPiiMaskingAdvisor egovPiiMaskingAdvisor(EgovPiiMaskingProperties props) {
        return new EgovPiiMaskingAdvisor(new EgovPiiMasker(props.resolveTypes(), props.isVerifyCheckdigit()), ORDER_PII);
    }

    /** order=200에 위치할 예외 분류·폴백 advisor를 등록한다. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.fallback.enabled", matchIfMissing = true)
    public EgovLlmFallbackAdvisor egovLlmFallbackAdvisor(EgovLlmFallbackProperties props) {
        return new EgovLlmFallbackAdvisor(new EgovLlmExceptionMapper(),
                props.isReturnFallback(), props.getFallbackMessage(), ORDER_FALLBACK);
    }

    /** advisor 체인 최내곽(order=300)에 위치할 토큰 사용량 로깅 advisor를 등록한다. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.token-usage.enabled", matchIfMissing = true)
    public EgovTokenUsageAdvisor egovTokenUsageAdvisor(EgovTokenUsageProperties props) {
        return new EgovTokenUsageAdvisor(new EgovTokenUsageFormatter(), ORDER_USAGE);
    }

    /**
     * 프롬프트 템플릿 매니저를 등록한다. advisor 체인에는 참여하지 않는 독립 컴포넌트로,
     * 애플리케이션 코드가 직접 주입받아 사용한다.
     */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.prompt-template.enabled", matchIfMissing = true)
    public EgovPromptTemplateManager egovPromptTemplateManager(ResourceLoader resourceLoader,
                                                               EgovPromptTemplateProperties props) {
        return new EgovPromptTemplateManager(resourceLoader, props);
    }

    /** order=75에 위치할 보안 가드레일 advisor를 등록한다. 금칙어·인젝션 패턴 탐지기를 함께 구성한다. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.safeguard.enabled", matchIfMissing = true)
    public EgovAiSafeGuardAdvisor egovAiSafeGuardAdvisor(EgovAiSafeGuardProperties props) {
        EgovAiSafeGuardChecker checker =
            new EgovAiSafeGuardChecker(props.getBlockedWords(), props.isDetectInjection());
        return new EgovAiSafeGuardAdvisor(checker, props.getBlockMessage(), ORDER_SAFEGUARD);
    }

    /** order=250에 위치할 감사 로그 advisor를 등록한다. 이벤트 발행에 Spring의 기본 퍼블리셔를 사용한다. */
    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.audit.enabled", matchIfMissing = true)
    public EgovAiAuditLogAdvisor egovAiAuditLogAdvisor(ApplicationEventPublisher publisher,
                                                        EgovAiAuditProperties props) {
        return new EgovAiAuditLogAdvisor(publisher, props, ORDER_AUDIT);
    }

    /**
     * 활성화된 advisor 빈을 모두 주입받아 {@link ChatClient.Builder}의 기본 advisor로 배선하는
     * 커스터마이저를 등록한다. 항상 등록되며(조건 없음), 실제 배선 여부는
     * {@link EgovAiChatClientCustomizer#customize}가 advisor 목록이 비어 있는지로 판단한다.
     */
    @Bean
    public EgovAiChatClientCustomizer egovAiChatClientCustomizer(List<Advisor> advisors) {
        return new EgovAiChatClientCustomizer(advisors);
    }
}
