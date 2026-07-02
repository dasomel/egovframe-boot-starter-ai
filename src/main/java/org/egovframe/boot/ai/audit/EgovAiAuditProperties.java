package org.egovframe.boot.ai.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 감사 로그 및 이벤트 수집 설정 프로퍼티.
 * {@code egovframe.ai.audit} 프리픽스를 사용한다.
 */
@ConfigurationProperties("egovframe.ai.audit")
public class EgovAiAuditProperties {

    private boolean enabled = true;
    private boolean logToConsole = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public void setLogToConsole(boolean logToConsole) {
        this.logToConsole = logToConsole;
    }
}
