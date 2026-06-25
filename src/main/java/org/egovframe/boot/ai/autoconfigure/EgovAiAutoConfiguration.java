package org.egovframe.boot.ai.autoconfigure;

import org.egovframe.boot.ai.fallback.EgovLlmExceptionMapper;
import org.egovframe.boot.ai.fallback.EgovLlmFallbackAdvisor;
import org.egovframe.boot.ai.fallback.EgovLlmFallbackProperties;
import org.egovframe.boot.ai.pii.EgovPiiMasker;
import org.egovframe.boot.ai.pii.EgovPiiMaskingAdvisor;
import org.egovframe.boot.ai.pii.EgovPiiMaskingProperties;
import org.egovframe.boot.ai.usage.EgovTokenUsageAdvisor;
import org.egovframe.boot.ai.usage.EgovTokenUsageFormatter;
import org.egovframe.boot.ai.usage.EgovTokenUsageProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.util.List;

@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@EnableConfigurationProperties({
        EgovPiiMaskingProperties.class,
        EgovLlmFallbackProperties.class,
        EgovTokenUsageProperties.class })
public class EgovAiAutoConfiguration {

    // advisor 순서: PII(선실행) < fallback < usage(후실행)
    static final int ORDER_PII = 100;
    static final int ORDER_FALLBACK = 200;
    static final int ORDER_USAGE = 300;

    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.pii-masking.enabled", matchIfMissing = true)
    public EgovPiiMaskingAdvisor egovPiiMaskingAdvisor(EgovPiiMaskingProperties props) {
        return new EgovPiiMaskingAdvisor(new EgovPiiMasker(props.resolveTypes()), ORDER_PII);
    }

    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.fallback.enabled", matchIfMissing = true)
    public EgovLlmFallbackAdvisor egovLlmFallbackAdvisor(EgovLlmFallbackProperties props) {
        return new EgovLlmFallbackAdvisor(new EgovLlmExceptionMapper(),
                props.isReturnFallback(), props.getFallbackMessage(), ORDER_FALLBACK);
    }

    @Bean
    @ConditionalOnProperty(name = "egovframe.ai.token-usage.enabled", matchIfMissing = true)
    public EgovTokenUsageAdvisor egovTokenUsageAdvisor(EgovTokenUsageProperties props) {
        return new EgovTokenUsageAdvisor(new EgovTokenUsageFormatter(), ORDER_USAGE);
    }

    @Bean
    public EgovAiChatClientCustomizer egovAiChatClientCustomizer(List<Advisor> advisors) {
        return new EgovAiChatClientCustomizer(advisors);
    }
}
