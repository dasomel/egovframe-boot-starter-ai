package org.egovframe.boot.ai.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

/**
 * 프롬프트 요청 및 LLM 응답을 추적하여 감사 레코드를 생성하고
 * {@link ApplicationEventPublisher}를 통해 {@link EgovAiAuditEvent}를 비동기 수집이 가능하도록 발행하는 어드바이저.
 */
public class EgovAiAuditLogAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovAiAuditLogAdvisor.class);

    private final ApplicationEventPublisher eventPublisher;
    private final boolean logToConsole;
    private final int order;

    public EgovAiAuditLogAdvisor(ApplicationEventPublisher eventPublisher, boolean logToConsole, int order) {
        this.eventPublisher = eventPublisher;
        this.logToConsole = logToConsole;
        this.order = order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long startTime = System.currentTimeMillis();
        String userText = request.prompt().getUserMessage().getText();
        ChatClientResponse response = null;
        String status = "SUCCESS";
        String responseText = "";

        try {
            response = chain.nextCall(request);
            if (response != null && response.chatResponse() != null
                    && response.chatResponse().getResult() != null
                    && response.chatResponse().getResult().getOutput() != null) {
                responseText = response.chatResponse().getResult().getOutput().getText();
            }
            return response;
        } catch (Exception e) {
            status = "ERROR: " + e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            publishAuditEvent(userText, responseText, status, duration);
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long startTime = System.currentTimeMillis();
        String userText = request.prompt().getUserMessage().getText();
        StringBuilder accumulatedResponse = new StringBuilder();

        return chain.nextStream(request)
                .doOnNext(chunk -> {
                    if (chunk != null && chunk.chatResponse() != null
                            && chunk.chatResponse().getResult() != null
                            && chunk.chatResponse().getResult().getOutput() != null
                            && chunk.chatResponse().getResult().getOutput().getText() != null) {
                        accumulatedResponse.append(chunk.chatResponse().getResult().getOutput().getText());
                    }
                })
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    publishAuditEvent(userText, accumulatedResponse.toString(), "SUCCESS", duration);
                })
                .doOnError(e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    publishAuditEvent(userText, accumulatedResponse.toString(), "ERROR: " + e.getMessage(), duration);
                });
    }

    private void publishAuditEvent(String promptText, String responseText, String status, long durationMs) {
        String traceId = "AUDIT-" + Long.toHexString(System.currentTimeMillis());
        EgovAiAuditEvent event = new EgovAiAuditEvent(this, traceId, promptText, responseText, status, durationMs);

        if (logToConsole) {
            log.info("[AI 감사 로그] status={} elapsedMs={} prompt='{}' response='{}'",
                    status, durationMs, promptText, responseText);
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }

    @Override
    public String getName() {
        return "EgovAiAuditLogAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
