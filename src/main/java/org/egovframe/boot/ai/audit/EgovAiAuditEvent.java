package org.egovframe.boot.ai.audit;

/**
 * AI 호출 감사 레코드.
 *
 * <p>query 필드는 이 advisor가 PII 마스킹 이후(inner)에 위치하므로
 * 이미 마스킹된 안전한 텍스트만 담긴다. 원본 미마스킹 PII는 저장하지 않는다.
 */
public class EgovAiAuditEvent {

    private final String traceId;
    private final String query;
    private final String response;
    private final String model;
    private final long elapsedMs;

    public EgovAiAuditEvent(String traceId, String query, String response, String model, long elapsedMs) {
        this.traceId = traceId;
        this.query = query;
        this.response = response;
        this.model = model;
        this.elapsedMs = elapsedMs;
    }

    public String getTraceId() { return traceId; }
    public String getQuery() { return query; }
    public String getResponse() { return response; }
    public String getModel() { return model; }
    public long getElapsedMs() { return elapsedMs; }
}
