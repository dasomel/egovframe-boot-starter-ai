package org.egovframe.boot.ai.trace;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EgovAiTraceLogFormatterTest {

    private final EgovAiTraceLogFormatter formatter = new EgovAiTraceLogFormatter();

    @Test
    void startIncludesTraceIdAndModel() {
        String msg = formatter.start("abc12345", "gpt-4o", 200);
        assertThat(msg).contains("abc12345").contains("gpt-4o").contains("200");
    }

    @Test
    void startWithNullModelAndChars() {
        String msg = formatter.start("abc12345", null, null);
        assertThat(msg).contains("abc12345").doesNotContain("model=").doesNotContain("promptChars=");
    }

    @Test
    void endOkStatus() {
        String msg = formatter.end("abc12345", 123L, false);
        assertThat(msg).contains("abc12345").contains("123").contains("OK");
    }

    @Test
    void endErrorStatus() {
        String msg = formatter.end("abc12345", 50L, true);
        assertThat(msg).contains("ERROR");
    }
}
