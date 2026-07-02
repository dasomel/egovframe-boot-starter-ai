package org.egovframe.boot.ai.fallback;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Throwable을 {@link EgovLlmException}으로 분류·변환하는 순수 로직.
 * Spring AI나 특정 LLM 클라이언트 SDK의 예외 타입에 의존하지 않고 표준 예외 타입과
 * 메시지 문자열만으로 분류하므로, 구현체가 던지는 예외 클래스가 무엇이든(WebClient,
 * RestClient, 벤더별 SDK 등) 동일한 규칙으로 {@link EgovLlmException.Kind}에 매핑할 수 있다.
 */
public class EgovLlmExceptionMapper {

    /**
     * 예외를 표준 {@link EgovLlmException}으로 변환한다.
     * 이미 {@link EgovLlmException}이면(예: advisor 체인 상위에서 재전파된 경우) 재분류하지 않고
     * 그대로 반환해, 분류 정보가 중첩 래핑으로 소실되지 않도록 한다.
     *
     * @param t 원인 예외
     * @return 표준 분류가 적용된 {@link EgovLlmException}
     */
    public EgovLlmException map(Throwable t) {
        if (t instanceof EgovLlmException e) {
            return e;
        }
        EgovLlmException.Kind kind = classify(t);
        return new EgovLlmException(kind, t.getMessage(), t);
    }

    /**
     * 예외를 {@link EgovLlmException.Kind}로 분류한다.
     *
     * <p>먼저 예외의 구체 타입({@link TimeoutException}, {@link SocketTimeoutException})으로
     * TIMEOUT을 판정하고(가장 신뢰도 높은 신호), 타입만으로 판단할 수 없는 경우 메시지 문자열의
     * 키워드로 보강 판정한다. 키워드 검사는 TIMEOUT → RATE_LIMIT(HTTP 429류) → SERVER(HTTP 5xx류)
     * 순으로 확인하며, 어느 것에도 해당하지 않으면 UNKNOWN으로 분류한다. 원인 예외 체인
     * (cause)은 별도로 순회하지 않는다 — 최상위 예외의 타입·메시지만으로 충분히 분류되도록
     * 하위 라이브러리가 의미 있는 메시지를 최상위 예외에 담아 던진다는 전제에 기반한다.</p>
     */
    private EgovLlmException.Kind classify(Throwable t) {
        if (t instanceof TimeoutException || t instanceof SocketTimeoutException) {
            return EgovLlmException.Kind.TIMEOUT;
        }
        String msg = (t.getMessage() == null ? "" : t.getMessage()).toLowerCase();
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return EgovLlmException.Kind.TIMEOUT;
        }
        if (msg.contains("429") || msg.contains("too many requests") || msg.contains("rate limit")) {
            return EgovLlmException.Kind.RATE_LIMIT;
        }
        if (msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504")
                || msg.contains("server error")) {
            return EgovLlmException.Kind.SERVER;
        }
        return EgovLlmException.Kind.UNKNOWN;
    }
}
