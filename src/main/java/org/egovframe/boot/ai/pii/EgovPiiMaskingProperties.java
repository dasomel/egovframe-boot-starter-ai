package org.egovframe.boot.ai.pii;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Set;

@ConfigurationProperties("egovframe.ai.pii-masking")
public class EgovPiiMaskingProperties {
    private boolean enabled = true;
    private Set<EgovPiiType> types;  // null이면 defaultOn()

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<EgovPiiType> getTypes() { return types; }
    public void setTypes(Set<EgovPiiType> types) { this.types = types; }
    public Set<EgovPiiType> resolveTypes() {
        return (types == null || types.isEmpty()) ? EgovPiiType.defaultOn() : types;
    }
}
