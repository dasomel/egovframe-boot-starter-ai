package org.egovframe.boot.ai.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/** nextCall 후 토큰 사용량을 표준 포맷으로 로깅한다. */
public class EgovTokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovTokenUsageAdvisor.class);

    private final EgovTokenUsageFormatter formatter;
    private final int order;

    public EgovTokenUsageAdvisor(EgovTokenUsageFormatter formatter, int order) {
        this.formatter = formatter;
        this.order = order;
    }

    private void logUsage(ChatClientResponse response) {
        ChatResponse cr = response == null ? null : response.chatResponse();
        if (cr == null || cr.getMetadata() == null) { return; }
        Usage u = cr.getMetadata().getUsage();
        if (u == null) { return; }
        log.info(formatter.format(u.getPromptTokens(), u.getCompletionTokens(), u.getTotalTokens()));
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        logUsage(response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(request).doOnNext(this::logUsage);
    }

    @Override public String getName() { return "EgovTokenUsageAdvisor"; }
    @Override public int getOrder() { return order; }
}
