package org.egovframe.boot.ai.fallback;

/**
 * LLM 호출 실패를 공공서비스 표준 분류로 감싸는 예외.
 * {@link EgovLlmExceptionMapper#map}이 다양한 라이브러리·네트워크 예외를 이 타입으로 변환하며,
 * {@link EgovLlmFallbackAdvisor}가 {@code returnFallback=false}로 설정된 경우 이 예외를
 * 호출자에게 그대로 재전파한다. 호출자는 원인(cause) 예외의 구체 타입을 알 필요 없이
 * {@link Kind}만으로 재시도·회로차단 등 후속 처리를 분기할 수 있다.
 */
public class EgovLlmException extends RuntimeException {

    /** 예외 발생 원인의 표준 분류. */
    public enum Kind {
        /** 호출 제한 시간 초과. */
        TIMEOUT,
        /** LLM 제공자의 요청 빈도 제한(HTTP 429 등)에 걸린 경우. */
        RATE_LIMIT,
        /** LLM 제공자 측 서버 오류(HTTP 5xx 등). */
        SERVER,
        /** 위 분류에 해당하지 않는 그 밖의 오류. */
        UNKNOWN
    }

    private final Kind kind;

    /**
     * @param kind    표준 분류
     * @param message 원인 예외의 메시지(원본 그대로 보존)
     * @param cause   원인 예외
     */
    public EgovLlmException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() { return kind; }
}
