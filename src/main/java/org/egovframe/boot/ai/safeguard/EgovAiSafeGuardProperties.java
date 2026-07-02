package org.egovframe.boot.ai.safeguard;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code egovframe.ai.safeguard.*} 설정 바인딩. {@link EgovAiSafeGuardAdvisor}와
 * {@link EgovAiSafeGuardChecker}의 동작(활성화 여부, 금칙어 목록, 인젝션 탐지 여부,
 * 차단 메시지)을 제어한다.
 */
@ConfigurationProperties("egovframe.ai.safeguard")
public class EgovAiSafeGuardProperties {

    /** advisor 활성화 여부. */
    private boolean enabled = true;

    /** 차단할 금칙어 목록. 대소문자를 구분하지 않는다. */
    private List<String> blockedWords = new ArrayList<>();

    /** 프롬프트 인젝션 패턴 탐지 활성화 여부. */
    private boolean detectInjection = true;

    /** 차단 시 반환할 안내 메시지. */
    private String blockMessage = "요청에 허용되지 않는 내용이 포함되어 있어 처리할 수 없습니다.";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<String> getBlockedWords() { return blockedWords; }
    public void setBlockedWords(List<String> blockedWords) { this.blockedWords = blockedWords; }

    public boolean isDetectInjection() { return detectInjection; }
    public void setDetectInjection(boolean detectInjection) { this.detectInjection = detectInjection; }

    public String getBlockMessage() { return blockMessage; }
    public void setBlockMessage(String blockMessage) { this.blockMessage = blockMessage; }
}
