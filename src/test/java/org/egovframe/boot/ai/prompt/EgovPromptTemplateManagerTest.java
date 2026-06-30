package org.egovframe.boot.ai.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EgovPromptTemplateManagerTest {

    private EgovPromptTemplateManager manager;

    @BeforeEach
    void setUp() {
        EgovPromptTemplateProperties props = new EgovPromptTemplateProperties();
        props.setLocation("classpath:/egovframe/ai/prompts/");
        props.setSuffix(".st");
        manager = new EgovPromptTemplateManager(new DefaultResourceLoader(), props);
    }

    @Test
    void rendersVariables() {
        String result = manager.render("greeting", Map.of("name", "홍길동", "topic", "여권 발급"));
        assertThat(result).isEqualTo("안녕하세요 홍길동님, 여권 발급에 대해 안내드립니다.");
    }

    @Test
    void cacheReturnsSameResult() {
        String first  = manager.render("greeting", Map.of("name", "A", "topic", "B"));
        String second = manager.render("greeting", Map.of("name", "A", "topic", "B"));
        assertThat(first).isEqualTo(second);
    }

    @Test
    void containsReturnsTrueForExistingTemplate() {
        assertThat(manager.contains("greeting")).isTrue();
    }

    @Test
    void containsReturnsFalseForMissingTemplate() {
        assertThat(manager.contains("nonexistent")).isFalse();
    }

    @Test
    void renderThrowsForMissingTemplate() {
        assertThatThrownBy(() -> manager.render("nonexistent", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }
}
