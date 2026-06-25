package org.egovframe.boot.ai.pii;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/** 프롬프트 user 메시지의 개인정보를 마스킹한 뒤 다음 advisor로 전달한다. */
public class EgovPiiMaskingAdvisor implements CallAdvisor, StreamAdvisor {

    private final EgovPiiMasker masker;
    private final int order;

    public EgovPiiMaskingAdvisor(EgovPiiMasker masker, int order) {
        this.masker = masker;
        this.order = order;
    }

    private ChatClientRequest maskRequest(ChatClientRequest request) {
        Prompt masked = request.prompt().augmentUserMessage(
            um -> um.mutate().text(masker.mask(um.getText())).build());
        return request.mutate().prompt(masked).build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(maskRequest(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(maskRequest(request));
    }

    @Override public String getName() { return "EgovPiiMaskingAdvisor"; }
    @Override public int getOrder() { return order; }
}
