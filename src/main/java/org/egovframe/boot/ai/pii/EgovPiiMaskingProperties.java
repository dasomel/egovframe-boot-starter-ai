package org.egovframe.boot.ai.pii;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Set;

/**
 * {@code egovframe.ai.pii-masking.*} 설정 바인딩. {@link org.egovframe.boot.ai.autoconfigure.EgovAiAutoConfiguration}이
 * 이 값으로 {@link EgovPiiMasker}·{@link org.egovframe.boot.ai.pii.EgovPiiMaskingAdvisor}를 구성한다.
 */
@ConfigurationProperties("egovframe.ai.pii-masking")
public class EgovPiiMaskingProperties {
    /** advisor 활성화 여부. */
    private boolean enabled = true;
    /** 마스킹할 PII 유형 목록. 비워 두면(null) {@link EgovPiiType#defaultOn()}이 적용된다. */
    private Set<EgovPiiType> types;
    /** 체크디지트 검증. 기본 false(보수적: 패턴 매칭만으로 마스킹). 2020년 이후 주민번호는 체크섬 폐지. */
    private boolean verifyCheckdigit = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<EgovPiiType> getTypes() { return types; }
    public void setTypes(Set<EgovPiiType> types) { this.types = types; }
    public boolean isVerifyCheckdigit() { return verifyCheckdigit; }
    public void setVerifyCheckdigit(boolean verifyCheckdigit) { this.verifyCheckdigit = verifyCheckdigit; }

    /** 설정된 유형이 없으면 {@link EgovPiiType#defaultOn()}으로 대체해 실제 적용 대상을 반환한다. */
    public Set<EgovPiiType> resolveTypes() {
        return (types == null || types.isEmpty()) ? EgovPiiType.defaultOn() : types;
    }
}
