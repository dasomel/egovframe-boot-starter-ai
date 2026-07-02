package org.egovframe.boot.ai.usage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code egovframe.ai.token-usage.*} 설정 바인딩. {@link EgovTokenUsageAdvisor} 활성화 여부를 제어한다. */
@ConfigurationProperties("egovframe.ai.token-usage")
public class EgovTokenUsageProperties {
    /** advisor 활성화 여부. */
    private boolean enabled = true;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
