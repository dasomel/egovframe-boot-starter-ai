package org.egovframe.boot.ai.fallback;

import org.junit.jupiter.api.Test;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import static org.assertj.core.api.Assertions.assertThat;

class EgovLlmExceptionMapperTest {
    private final EgovLlmExceptionMapper mapper = new EgovLlmExceptionMapper();

    @Test void timeoutException() {
        assertThat(mapper.map(new TimeoutException("timed out")).getKind())
            .isEqualTo(EgovLlmException.Kind.TIMEOUT);
    }
    @Test void socketTimeout() {
        assertThat(mapper.map(new SocketTimeoutException("read timed out")).getKind())
            .isEqualTo(EgovLlmException.Kind.TIMEOUT);
    }
    @Test void rateLimitByMessage() {
        assertThat(mapper.map(new RuntimeException("HTTP 429 Too Many Requests")).getKind())
            .isEqualTo(EgovLlmException.Kind.RATE_LIMIT);
    }
    @Test void serverErrorByMessage() {
        assertThat(mapper.map(new RuntimeException("500 Internal Server Error")).getKind())
            .isEqualTo(EgovLlmException.Kind.SERVER);
    }
    @Test void unknownOtherwise() {
        assertThat(mapper.map(new IllegalStateException("bad")).getKind())
            .isEqualTo(EgovLlmException.Kind.UNKNOWN);
    }
    @Test void preservesCause() {
        Throwable cause = new TimeoutException("x");
        assertThat(mapper.map(cause).getCause()).isSameAs(cause);
    }
    @Test void passesThroughExistingEgovLlmException() {
        EgovLlmException e = new EgovLlmException(EgovLlmException.Kind.SERVER, "x", null);
        assertThat(mapper.map(e)).isSameAs(e);
    }
}
