package org.egovframe.boot.ai.autoconfigure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientCustomizer;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import java.util.List;

/**
 * 활성화된 eGov advisor를 자동구성된 {@code ChatClient.Builder}의 기본 advisor로 주입한다.
 * Spring AI의 {@link ChatClientCustomizer} 확장점을 이용하므로, 애플리케이션이
 * {@code ChatClient.Builder}를 주입받아 {@code build()}만 호출해도 {@link EgovAiAutoConfiguration}이
 * 등록한 advisor 체인(TRACE→SAFEGUARD→PII→FALLBACK→AUDIT→USAGE)이 자동으로 적용된다.
 * 개별 advisor의 순서는 각 advisor의 {@code getOrder()} 값으로 정렬되며 이 클래스는
 * 순서를 재조정하지 않고 주입받은 목록을 그대로 등록만 한다.
 */
public class EgovAiChatClientCustomizer implements ChatClientCustomizer {

    private final List<Advisor> advisors;

    /** @param advisors 활성화된 advisor 빈 전체(비활성화된 기능의 advisor는 목록에 포함되지 않는다) */
    public EgovAiChatClientCustomizer(List<Advisor> advisors) {
        this.advisors = advisors;
    }

    /**
     * builder에 advisor 목록을 기본값으로 등록한다. 활성화된 advisor가 하나도 없으면
     * (모든 기능이 비활성화된 극단적인 경우) builder를 건드리지 않는다.
     */
    @Override
    public void customize(ChatClient.Builder builder) {
        if (!advisors.isEmpty()) {
            builder.defaultAdvisors(advisors);
        }
    }
}
