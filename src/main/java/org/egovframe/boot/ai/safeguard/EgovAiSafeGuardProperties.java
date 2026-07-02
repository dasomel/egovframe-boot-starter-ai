package org.egovframe.boot.ai.safeguard;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 내부 기밀/금칙어 사전 검사 가드레일 설정 프로퍼티.
 * {@code egovframe.ai.safeguard} 프리픽스를 사용한다.
 */
@ConfigurationProperties("egovframe.ai.safeguard")
public class EgovAiSafeGuardProperties {

    private boolean enabled = true;
    private List<String> keywords = new ArrayList<>();
    private String blockMessage = "입력하신 문장에 비공개 금칙어 또는 보안 부적절 단어가 포함되어 있어 처리할 수 없습니다.";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getBlockMessage() {
        return blockMessage;
    }

    public void setBlockMessage(String blockMessage) {
        this.blockMessage = blockMessage;
    }
}
