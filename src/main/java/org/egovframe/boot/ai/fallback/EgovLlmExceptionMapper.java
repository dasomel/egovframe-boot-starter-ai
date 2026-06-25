package org.egovframe.boot.ai.fallback;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/** Throwable을 {@link EgovLlmException}으로 분류·변환하는 순수 로직(외부 라이브러리 무의존). */
public class EgovLlmExceptionMapper {

    public EgovLlmException map(Throwable t) {
        if (t instanceof EgovLlmException e) {
            return e;
        }
        EgovLlmException.Kind kind = classify(t);
        return new EgovLlmException(kind, t.getMessage(), t);
    }

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
