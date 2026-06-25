package org.egovframe.boot.ai.pii;

import java.util.Set;

/** 활성화된 {@link EgovPiiType}을 enum 선언 순서대로 적용하는 순수 마스킹 로직. */
public class EgovPiiMasker {

    private final Set<EgovPiiType> enabled;

    public EgovPiiMasker(Set<EgovPiiType> enabled) {
        this.enabled = enabled;
    }

    public String mask(String text) {
        if (text == null || text.isBlank() || enabled.isEmpty()) {
            return text;
        }
        String result = text;
        for (EgovPiiType type : EgovPiiType.values()) {  // 선언 순서 = 우선순위
            if (enabled.contains(type)) {
                result = type.maskAll(result);
            }
        }
        return result;
    }
}
