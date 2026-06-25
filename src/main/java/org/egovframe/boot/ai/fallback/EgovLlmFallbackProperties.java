package org.egovframe.boot.ai.fallback;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("egovframe.ai.fallback")
public class EgovLlmFallbackProperties {
    private boolean enabled = true;
    private boolean returnFallback = true;
    private String fallbackMessage = "일시적으로 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isReturnFallback() { return returnFallback; }
    public void setReturnFallback(boolean returnFallback) { this.returnFallback = returnFallback; }
    public String getFallbackMessage() { return fallbackMessage; }
    public void setFallbackMessage(String m) { this.fallbackMessage = m; }
}
