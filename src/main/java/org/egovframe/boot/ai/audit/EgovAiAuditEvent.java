package org.egovframe.boot.ai.audit;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 공공부문 AI 실태평가 및 감사 대응을 위한 AI 처리 감사 이벤트 객체.
 */
public class EgovAiAuditEvent extends ApplicationEvent {

    private final String traceId;
    private final String promptText;
    private final String responseText;
    private final String status;
    private final long executionTimeMs;
    private final LocalDateTime eventTimestamp;

    public EgovAiAuditEvent(Object source, String traceId, String promptText, String responseText,
                           String status, long executionTimeMs) {
        super(source);
        this.traceId = traceId;
        this.promptText = promptText;
        this.responseText = responseText;
        this.status = status;
        this.executionTimeMs = executionTimeMs;
        this.eventTimestamp = LocalDateTime.now();
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPromptText() {
        return promptText;
    }

    public String getResponseText() {
        return responseText;
    }

    public String getStatus() {
        return status;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }
}
