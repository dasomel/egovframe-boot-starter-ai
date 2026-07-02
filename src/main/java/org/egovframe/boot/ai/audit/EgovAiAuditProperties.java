package org.egovframe.boot.ai.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code egovframe.ai.audit.*} 설정 바인딩. {@link EgovAiAuditLogAdvisor}의 활성화 여부와
 * 감사 이벤트에 응답 텍스트를 포함할지를 제어한다.
 */
@ConfigurationProperties("egovframe.ai.audit")
public class EgovAiAuditProperties {

    /** advisor 활성화 여부. */
    private boolean enabled = true;

    /** 감사 이벤트에 응답 텍스트를 포함할지 여부. */
    private boolean includeResponse = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isIncludeResponse() { return includeResponse; }
    public void setIncludeResponse(boolean includeResponse) { this.includeResponse = includeResponse; }
}
