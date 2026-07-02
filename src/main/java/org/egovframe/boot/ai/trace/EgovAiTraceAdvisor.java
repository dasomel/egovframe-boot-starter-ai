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
 * advisor 체인에서 order=50으로 가장 바깥에 위치해(order 값이 낮을수록 바깥) 이후의
 * SafeGuard·PII·Fallback·Audit·Usage advisor를 포함한 전체 체인의 소요 시간을 측정하고,
 * 요청 전체에 걸쳐 MDC 추적 ID를 공유할 수 있게 한다({@link org.egovframe.boot.ai.audit.EgovAiAuditLogAdvisor}가
 * 동일 MDC 키로 traceId를 읽어 감사 이벤트에 함께 기록한다).
 */
public class EgovAiTraceAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovAiTraceAdvisor.class);

    private final EgovAiTraceLogFormatter formatter;
    private final EgovAiTraceProperties properties;
    private final int order;

    /**
     * @param formatter  로그 문자열 생성기
     * @param properties trace 설정(프롬프트 문자 수 포함 여부, MDC 키)
     * @param order      advisor 체인 내 실행 순서(낮을수록 바깥쪽)
     */
    public EgovAiTraceAdvisor(EgovAiTraceLogFormatter formatter,
                               EgovAiTraceProperties properties,
                               int order) {
        this.formatter = formatter;
        this.properties = properties;
        this.order = order;
    }

    /** 호출마다 새로운 8자리 추적 ID를 생성한다(UUID 앞부분만 사용해 로그 가독성 확보). */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** {@code includePrompt=true}일 때만 user 메시지의 문자 수를 계산한다. 원문은 로그에 남기지 않는다. */
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

    /** 요청에 설정된 모델 이름을 조회한다. 옵션이 없거나 조회 중 예외가 발생하면 null을 반환한다. */
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

    /**
     * 동기 호출을 감싸 시작/종료 로그를 남기고 MDC에 추적 ID를 등록·해제한다.
     * 하위 체인에서 예외가 발생해도 종료 로그는 반드시 남기며 상태를 ERROR로 표시한 뒤 재전파한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 호출 체인
     * @return 다음 advisor 체인이 반환한 응답
     */
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

    /**
     * 스트리밍 호출을 감싸 시작 로그를 즉시 남기고, 스트림이 종료(정상/에러/취소)될 때
     * {@code doFinally}에서 종료 로그를 남긴다. 스트림 처리는 별도 스레드에서 이어질 수 있으므로
     * 종료 시점에 MDC를 다시 등록한 뒤 로깅하고 즉시 해제한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 스트림 체인
     * @return 다음 advisor 체인이 반환한 응답 스트림
     */
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
