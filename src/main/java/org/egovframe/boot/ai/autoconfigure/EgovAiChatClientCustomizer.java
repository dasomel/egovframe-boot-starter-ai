package org.egovframe.boot.ai.autoconfigure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientCustomizer;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import java.util.List;

/** 활성화된 eGov advisor를 자동구성된 ChatClient.Builder의 기본 advisor로 주입한다. */
public class EgovAiChatClientCustomizer implements ChatClientCustomizer {

    private final List<Advisor> advisors;

    public EgovAiChatClientCustomizer(List<Advisor> advisors) {
        this.advisors = advisors;
    }

    @Override
    public void customize(ChatClient.Builder builder) {
        if (!advisors.isEmpty()) {
            builder.defaultAdvisors(advisors);
        }
    }
}
