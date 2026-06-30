package org.egovframe.boot.ai.audit;

import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

/**
 * AI 호출 감사 레코드를 {@link EgovAiAuditEvent}로 발행하는 advisor.
 *
 * <p>이 advisor는 PII 마스킹 advisor보다 안쪽(order 큼)에 위치하므로
 * 캡처되는 query는 이미 마스킹된 텍스트다. 원본 PII는 저장되지 않는다.
 *
 * <p>스트리밍 모드에서는 응답 청크 집계 복잡성으로 인해 query와 소요시간만 기록하며
 * 응답 텍스트는 포함하지 않는다({@code response=null}).
 */
public class EgovAiAuditLogAdvisor implements CallAdvisor, StreamAdvisor {

    private final ApplicationEventPublisher publisher;
    private final EgovAiAuditProperties props;
    private final int order;

    public EgovAiAuditLogAdvisor(ApplicationEventPublisher publisher,
                                  EgovAiAuditProperties props,
                                  int order) {
        this.publisher = publisher;
        this.props = props;
        this.order = order;
    }

    private String resolveUserText(ChatClientRequest request) {
        try {
            return request.prompt().getUserMessage().getText();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveModel(ChatClientRequest request) {
        try {
            var options = request.prompt().getOptions();
            return options != null ? options.getModel() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveResponseText(ChatClientResponse response) {
        try {
            ChatResponse cr = response.chatResponse();
            if (cr == null) { return null; }
            var result = cr.getResult();
            if (result == null) { return null; }
            return result.getOutput().getText();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String query = resolveUserText(request);
        String model = resolveModel(request);
        String traceId = MDC.get("egovAiTraceId");
        long start = System.nanoTime();

        ChatClientResponse response = chain.nextCall(request);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        String responseText = props.isIncludeResponse() ? resolveResponseText(response) : null;
        publisher.publishEvent(new EgovAiAuditEvent(traceId, query, responseText, model, elapsedMs));
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String query = resolveUserText(request);
        String model = resolveModel(request);
        String traceId = MDC.get("egovAiTraceId");
        long start = System.nanoTime();

        return chain.nextStream(request).doFinally(signal -> {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            // 스트리밍 모드에서는 응답 텍스트를 집계하지 않는다
            publisher.publishEvent(new EgovAiAuditEvent(traceId, query, null, model, elapsedMs));
        });
    }

    @Override public String getName() { return "EgovAiAuditLogAdvisor"; }
    @Override public int getOrder() { return order; }
}
