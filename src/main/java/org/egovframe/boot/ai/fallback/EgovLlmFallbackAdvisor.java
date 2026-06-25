package org.egovframe.boot.ai.fallback;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import java.util.List;

/** nextCall 예외를 표준 {@link EgovLlmException}으로 변환하고, 옵션에 따라 폴백 응답을 반환한다. */
public class EgovLlmFallbackAdvisor implements CallAdvisor, StreamAdvisor {

    private final EgovLlmExceptionMapper mapper;
    private final boolean returnFallback;
    private final String fallbackMessage;
    private final int order;

    public EgovLlmFallbackAdvisor(EgovLlmExceptionMapper mapper, boolean returnFallback,
                                  String fallbackMessage, int order) {
        this.mapper = mapper;
        this.returnFallback = returnFallback;
        this.fallbackMessage = fallbackMessage;
        this.order = order;
    }

    private ChatClientResponse fallbackResponse() {
        ChatResponse cr = new ChatResponse(List.of(new Generation(new AssistantMessage(fallbackMessage))));
        return ChatClientResponse.builder().chatResponse(cr).build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        try {
            return chain.nextCall(request);
        } catch (Throwable t) {
            EgovLlmException mapped = mapper.map(t);
            if (returnFallback) { return fallbackResponse(); }
            throw mapped;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(request).onErrorResume(t -> {
            EgovLlmException mapped = mapper.map(t);
            return returnFallback ? Flux.just(fallbackResponse()) : Flux.error(mapped);
        });
    }

    @Override public String getName() { return "EgovLlmFallbackAdvisor"; }
    @Override public int getOrder() { return order; }
}
