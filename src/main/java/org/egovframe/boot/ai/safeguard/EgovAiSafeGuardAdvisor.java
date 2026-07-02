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
import java.util.Map;

/**
 * 프롬프트 user 메시지에 비공개/기밀 금칙어가 포함되어 있는지 사전 검사하는 가드레일 어드바이저.
 * 금칙어가 감지될 경우 LLM 호출을 건너뛰고 차단 메시지를 즉시 반환(Short-circuit)한다.
 */
public class EgovAiSafeGuardAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovAiSafeGuardAdvisor.class);

    private final List<String> forbiddenKeywords;
    private final String blockMessage;
    private final int order;

    public EgovAiSafeGuardAdvisor(List<String> forbiddenKeywords, String blockMessage, int order) {
        this.forbiddenKeywords = forbiddenKeywords != null ? forbiddenKeywords : List.of();
        this.blockMessage = blockMessage != null ? blockMessage : "보안 정책에 따라 금지된 단어가 포함되어 있습니다.";
        this.order = order;
    }

    private String detectForbiddenKeyword(ChatClientRequest request) {
        if (forbiddenKeywords.isEmpty()) {
            return null;
        }
        String text = request.prompt().getUserMessage().getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        for (String keyword : forbiddenKeywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private ChatClientResponse createBlockedResponse() {
        AssistantMessage assistantMessage = new AssistantMessage(blockMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        return new ChatClientResponse(chatResponse, Map.of());
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String detected = detectForbiddenKeyword(request);
        if (detected != null) {
            log.warn("AI 프롬프트 금칙어 차단 발생 - 감지된 금칙어: {}", detected);
            return createBlockedResponse();
        }
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String detected = detectForbiddenKeyword(request);
        if (detected != null) {
            log.warn("AI 프롬프트 금칙어 차단 발생 (스트리밍) - 감지된 금칙어: {}", detected);
            return Flux.just(createBlockedResponse());
        }
        return chain.nextStream(request);
    }

    @Override
    public String getName() {
        return "EgovAiSafeGuardAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
