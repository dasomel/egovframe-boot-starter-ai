package org.egovframe.boot.ai.usage;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgovTokenUsageFormatterTest {
    private final EgovTokenUsageFormatter formatter = new EgovTokenUsageFormatter();

    @Test void formatsAllTokens() {
        assertThat(formatter.format(10, 20, 30))
            .isEqualTo("LLM token usage - prompt: 10, completion: 20, total: 30");
    }
    @Test void nullsTreatedAsZero() {
        assertThat(formatter.format(null, null, null))
            .isEqualTo("LLM token usage - prompt: 0, completion: 0, total: 0");
    }
}
