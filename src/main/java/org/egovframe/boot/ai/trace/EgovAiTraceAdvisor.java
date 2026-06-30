package org.egovframe.boot.ai.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * AI 호출의 시작/종료를 MDC 추적 ID와 함께 로깅하는 advisor.
 * 가장 바깥에서 감싸기 위해 낮은 order 값을 사용한다.
 */
public class EgovAiTraceAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovAiTraceAdvisor.class);

    private final EgovAiTraceLogFormatter formatter;
    private final EgovAiTraceProperties properties;
    private final int order;

    public EgovAiTraceAdvisor(EgovAiTraceLogFormatter formatter,
                               EgovAiTraceProperties properties,
                               int order) {
        this.formatter = formatter;
        this.properties = properties;
        this.order = order;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private Integer resolvePromptChars(ChatClientRequest request) {
        if (!properties.isIncludePrompt()) {
            return null;
        }
        try {
            String text = request.prompt().getUserMessage().getText();
            return text != null ? text.length() : 0;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveModel(ChatClientRequest request) {
        try {
            var options = request.prompt().getOptions();
            if (options != null) {
                return options.getModel();
            }
        } catch (Exception e) {
            // 모델 정보를 가져올 수 없는 경우 null 반환
        }
        return null;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String traceId = generateTraceId();
        MDC.put(properties.getMdcKey(), traceId);
        long start = System.nanoTime();
        boolean error = false;
        try {
            log.info(formatter.start(traceId, resolveModel(request), resolvePromptChars(request)));
            return chain.nextCall(request);
        } catch (Throwable t) {
            error = true;
            throw t;
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info(formatter.end(traceId, elapsedMs, error));
            MDC.remove(properties.getMdcKey());
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String traceId = generateTraceId();
        MDC.put(properties.getMdcKey(), traceId);
        try {
            long start = System.nanoTime();
            log.info(formatter.start(traceId, resolveModel(request), resolvePromptChars(request)));
            return chain.nextStream(request)
                    .doFinally(signal -> {
                        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                        boolean error = signal == reactor.core.publisher.SignalType.ON_ERROR;
                        MDC.put(properties.getMdcKey(), traceId);
                        try {
                            log.info(formatter.end(traceId, elapsedMs, error));
                        } finally {
                            MDC.remove(properties.getMdcKey());
                        }
                    });
        } finally {
            MDC.remove(properties.getMdcKey());
        }
    }

    @Override public String getName() { return "EgovAiTraceAdvisor"; }
    @Override public int getOrder() { return order; }
}
