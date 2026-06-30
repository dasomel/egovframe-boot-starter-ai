package org.egovframe.boot.ai.trace;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("egovframe.ai.trace")
public class EgovAiTraceProperties {

    private boolean enabled = true;
    private boolean includePrompt = false;
    private String mdcKey = "egovAiTraceId";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isIncludePrompt() { return includePrompt; }
    public void setIncludePrompt(boolean includePrompt) { this.includePrompt = includePrompt; }
    public String getMdcKey() { return mdcKey; }
    public void setMdcKey(String mdcKey) { this.mdcKey = mdcKey; }
}
