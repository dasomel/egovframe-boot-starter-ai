package org.egovframe.boot.ai.prompt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("egovframe.ai.prompt-template")
public class EgovPromptTemplateProperties {

    private boolean enabled = true;
    private String location = "classpath:/egovframe/ai/prompts/";
    private String suffix = ".st";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
}
