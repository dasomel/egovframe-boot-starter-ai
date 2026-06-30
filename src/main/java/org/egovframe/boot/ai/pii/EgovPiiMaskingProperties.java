package org.egovframe.boot.ai.pii;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Set;

@ConfigurationProperties("egovframe.ai.pii-masking")
public class EgovPiiMaskingProperties {
    private boolean enabled = true;
    private Set<EgovPiiType> types;  // null이면 defaultOn()
    /** 체크디지트 검증. 기본 false(보수적: 패턴 매칭만으로 마스킹). 2020년 이후 주민번호는 체크섬 폐지. */
    private boolean verifyCheckdigit = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<EgovPiiType> getTypes() { return types; }
    public void setTypes(Set<EgovPiiType> types) { this.types = types; }
    public boolean isVerifyCheckdigit() { return verifyCheckdigit; }
    public void setVerifyCheckdigit(boolean verifyCheckdigit) { this.verifyCheckdigit = verifyCheckdigit; }
    public Set<EgovPiiType> resolveTypes() {
        return (types == null || types.isEmpty()) ? EgovPiiType.defaultOn() : types;
    }
}
