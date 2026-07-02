package org.egovframe.boot.ai.fallback;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code egovframe.ai.fallback.*} 설정 바인딩. {@link EgovLlmFallbackAdvisor}가 예외를
 * 폴백 메시지로 흡수할지, 분류된 {@link EgovLlmException}으로 재전파할지를 이 값으로 결정한다.
 */
@ConfigurationProperties("egovframe.ai.fallback")
public class EgovLlmFallbackProperties {
    /** advisor 활성화 여부. */
    private boolean enabled = true;
    /** true(기본)면 폴백 메시지를 응답으로 반환, false면 분류된 예외를 그대로 재전파한다. */
    private boolean returnFallback = true;
    /** {@code returnFallback=true}일 때 응답으로 반환할 안내 메시지. */
    private String fallbackMessage = "일시적으로 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isReturnFallback() { return returnFallback; }
    public void setReturnFallback(boolean returnFallback) { this.returnFallback = returnFallback; }
    public String getFallbackMessage() { return fallbackMessage; }
    public void setFallbackMessage(String m) { this.fallbackMessage = m; }
}
