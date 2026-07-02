package org.egovframe.boot.ai.audit;

/**
 * AI 호출 감사 레코드. {@link EgovAiAuditLogAdvisor}가 호출마다 이 이벤트를 Spring
 * {@code ApplicationEventPublisher}로 발행하며, 소비 측에서 {@code @EventListener}로 수신해
 * 기관 감사 DB 등에 저장할 수 있다(README "감사 로그(AuditLog)" 절 참고).
 *
 * <p>query 필드는 이 advisor가 PII 마스킹 advisor(order=100)보다 안쪽(order=250)에 위치하므로
 * 이미 마스킹된 안전한 텍스트만 담긴다. 원본 미마스킹 PII는 저장하지 않는다.</p>
 */
public class EgovAiAuditEvent {

    /** {@link org.egovframe.boot.ai.trace.EgovAiTraceAdvisor}가 MDC에 등록한 호출 추적 ID. */
    private final String traceId;
    /** 마스킹된 user 메시지 텍스트. */
    private final String query;
    /** 어시스턴트 응답 텍스트. {@code include-response=false}이거나 스트리밍 모드면 null. */
    private final String response;
    /** 요청에 지정된 모델 이름(없으면 null). */
    private final String model;
    /** 호출 소요 시간(밀리초). */
    private final long elapsedMs;

    /**
     * @param traceId   호출 추적 ID
     * @param query     마스킹된 user 메시지
     * @param response  응답 텍스트(포함하지 않는 경우 null)
     * @param model     모델 이름(없으면 null)
     * @param elapsedMs 호출 소요 시간(밀리초)
     */
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
