package org.egovframe.boot.ai.trace;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code egovframe.ai.trace.*} 설정 바인딩. {@link EgovAiTraceAdvisor}의 동작(활성화 여부,
 * 프롬프트 문자 수 로깅 여부, MDC 키 이름)을 제어한다.
 */
@ConfigurationProperties("egovframe.ai.trace")
public class EgovAiTraceProperties {

    /** advisor 활성화 여부. */
    private boolean enabled = true;
    /** true면 시작 로그에 프롬프트 문자 수(길이만, 원문 아님)를 포함한다. 기본 false로 로그를 간결하게 유지한다. */
    private boolean includePrompt = false;
    /** 추적 ID를 등록할 MDC 키 이름. */
    private String mdcKey = "egovAiTraceId";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isIncludePrompt() { return includePrompt; }
    public void setIncludePrompt(boolean includePrompt) { this.includePrompt = includePrompt; }
    public String getMdcKey() { return mdcKey; }
    public void setMdcKey(String mdcKey) { this.mdcKey = mdcKey; }
}
