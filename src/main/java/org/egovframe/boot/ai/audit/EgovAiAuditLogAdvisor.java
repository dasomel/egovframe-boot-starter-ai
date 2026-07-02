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

    /**
     * @param publisher 감사 이벤트를 발행할 Spring 이벤트 퍼블리셔
     * @param props     감사 로그 설정(활성화 여부, 응답 포함 여부)
     * @param order     advisor 체인 내 실행 순서(낮을수록 바깥쪽)
     */
    public EgovAiAuditLogAdvisor(ApplicationEventPublisher publisher,
                                  EgovAiAuditProperties props,
                                  int order) {
        this.publisher = publisher;
        this.props = props;
        this.order = order;
    }

    /** 요청의 user 메시지 텍스트를 조회한다. 메시지가 없거나 조회 중 예외가 발생하면 null. */
    private String resolveUserText(ChatClientRequest request) {
        try {
            return request.prompt().getUserMessage().getText();
        } catch (Exception e) {
            return null;
        }
    }

    /** 요청에 지정된 모델 이름을 조회한다. 옵션이 없거나 조회 중 예외가 발생하면 null. */
    private String resolveModel(ChatClientRequest request) {
        try {
            var options = request.prompt().getOptions();
            return options != null ? options.getModel() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 응답의 어시스턴트 텍스트를 조회한다. 결과가 없거나 조회 중 예외가 발생하면 null. */
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

    /**
     * 동기 호출 완료 후 감사 이벤트를 발행한다. 소요 시간은 하위 체인 호출 전체(이 advisor보다
     * 안쪽에 위치한 Usage advisor 포함)를 포괄해 측정한다.
     *
     * <p>traceId는 MDC에서 고정 키 {@code "egovAiTraceId"}로 직접 조회한다. 이는
     * {@link org.egovframe.boot.ai.trace.EgovAiTraceProperties}의 기본 MDC 키와 동일하며,
     * trace advisor가 커스텀 {@code mdc-key}로 재설정된 환경에서는 traceId가 조회되지 않을 수 있다.</p>
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 호출 체인
     * @return 다음 advisor 체인이 반환한 응답(변경 없이 그대로 반환)
     */
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

    /**
     * 스트리밍 호출이 종료(정상/에러/취소)될 때 감사 이벤트를 발행한다. 청크 단위로 도착하는
     * 응답 텍스트를 집계하는 복잡성을 피하기 위해 스트리밍 모드에서는 항상 {@code response=null}로
     * 기록한다({@code include-response} 설정과 무관).
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 스트림 체인
     * @return 다음 advisor 체인이 반환한 응답 스트림(변경 없이 그대로 반환)
     */
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
