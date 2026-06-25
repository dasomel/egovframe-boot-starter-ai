package org.egovframe.boot.ai.fallback;

/** LLM 호출 실패를 공공서비스 표준 분류로 감싸는 예외. */
public class EgovLlmException extends RuntimeException {

    public enum Kind { TIMEOUT, RATE_LIMIT, SERVER, UNKNOWN }

    private final Kind kind;

    public EgovLlmException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() { return kind; }
}
