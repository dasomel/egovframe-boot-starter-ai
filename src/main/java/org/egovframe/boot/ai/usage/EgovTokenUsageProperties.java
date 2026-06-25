package org.egovframe.boot.ai.usage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("egovframe.ai.token-usage")
public class EgovTokenUsageProperties {
    private boolean enabled = true;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
