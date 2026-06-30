package org.egovframe.boot.ai.safeguard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * 금칙어·프롬프트 인젝션 탐지 시 LLM 호출 없이 즉시 차단 응답을 반환하는 advisor.
 * PII 마스킹보다 앞(order 작음)에서 동작한다.
 */
public class EgovAiSafeGuardAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovAiSafeGuardAdvisor.class);

    private final EgovAiSafeGuardChecker checker;
    private final String blockMessage;
    private final int order;

    public EgovAiSafeGuardAdvisor(EgovAiSafeGuardChecker checker, String blockMessage, int order) {
        this.checker = checker;
        this.blockMessage = blockMessage;
        this.order = order;
    }

    private String resolveUserText(ChatClientRequest request) {
        try {
            return request.prompt().getUserMessage().getText();
        } catch (Exception e) {
            return null;
        }
    }

    private ChatClientResponse blockResponse() {
        ChatResponse cr = new ChatResponse(List.of(new Generation(new AssistantMessage(blockMessage))));
        return ChatClientResponse.builder().chatResponse(cr).build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Optional<String> violation = checker.check(resolveUserText(request));
        if (violation.isPresent()) {
            log.warn("[SafeGuard] 요청 차단 — 사유: {}", violation.get());
            return blockResponse();
        }
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Optional<String> violation = checker.check(resolveUserText(request));
        if (violation.isPresent()) {
            log.warn("[SafeGuard] 스트리밍 요청 차단 — 사유: {}", violation.get());
            return Flux.just(blockResponse());
        }
        return chain.nextStream(request);
    }

    @Override public String getName() { return "EgovAiSafeGuardAdvisor"; }
    @Override public int getOrder() { return order; }
}
