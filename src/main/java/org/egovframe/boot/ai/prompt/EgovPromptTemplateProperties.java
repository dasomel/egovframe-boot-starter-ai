package org.egovframe.boot.ai.prompt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code egovframe.ai.prompt-template.*} 설정 바인딩. {@link EgovPromptTemplateManager}가
 * 템플릿을 찾을 기본 경로와 파일 확장자를 이 값으로 결정한다.
 */
@ConfigurationProperties("egovframe.ai.prompt-template")
public class EgovPromptTemplateProperties {

    /** 템플릿 매니저 빈 등록 여부. */
    private boolean enabled = true;
    /** 템플릿 파일을 찾을 기본 클래스패스 경로. */
    private String location = "classpath:/egovframe/ai/prompts/";
    /** 템플릿 파일 확장자. */
    private String suffix = ".st";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
}
