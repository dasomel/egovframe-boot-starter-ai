package org.egovframe.boot.ai.pii;

import java.util.Set;

/**
 * 활성화된 {@link EgovPiiType}을 enum 선언 순서대로 순차 적용하는 순수 마스킹 로직.
 * 스프링 컨텍스트나 advisor 체인과 무관하게 문자열만 받아 문자열을 반환하므로 단독으로
 * 단위 테스트할 수 있다. {@link org.egovframe.boot.ai.pii.EgovPiiMaskingAdvisor}가 이
 * 클래스를 감싸 advisor 체인(order=100, PII 마스킹 단계)에 연결한다.
 */
public class EgovPiiMasker {

    private final Set<EgovPiiType> enabled;
    private final boolean verifyCheckdigit;

    /** 체크디지트 검증 없이(기본값 false) 활성 유형만으로 마스커를 생성한다. */
    public EgovPiiMasker(Set<EgovPiiType> enabled) {
        this(enabled, false);
    }

    /**
     * @param enabled          마스킹을 적용할 {@link EgovPiiType} 집합
     * @param verifyCheckdigit true면 유형별 체크디지트 검증을 통과한 경우에만 마스킹한다.
     *                         기본값은 false(보수적) — 자세한 이유는 {@link EgovPiiType} 클래스 문서 참고.
     */
    public EgovPiiMasker(Set<EgovPiiType> enabled, boolean verifyCheckdigit) {
        this.enabled = enabled;
        this.verifyCheckdigit = verifyCheckdigit;
    }

    /**
     * 텍스트에 활성화된 PII 유형을 모두 적용해 마스킹한 결과를 반환한다.
     *
     * <p>{@link EgovPiiType} enum 선언 순서대로 순차 치환하므로, 특이하고 긴 패턴(RRN 등)이
     * 먼저 마스킹되어 뒤이은 느슨한 패턴(ACCOUNT 등)과의 충돌을 줄인다. 이미 마스킹된 구간은
     * 뒤따르는 유형의 정규식과 일반적으로 일치하지 않으므로 이중 마스킹 위험은 낮다.</p>
     *
     * @param text 원본 텍스트(널이거나 공백이면 그대로 반환)
     * @return 마스킹된 텍스트. 활성 유형이 없으면 원본을 그대로 반환한다.
     */
    public String mask(String text) {
        if (text == null || text.isBlank() || enabled.isEmpty()) {
            return text;
        }
        String result = text;
        for (EgovPiiType type : EgovPiiType.values()) {  // 선언 순서 = 우선순위
            if (enabled.contains(type)) {
                result = type.maskAll(result, verifyCheckdigit);
            }
        }
        return result;
    }
}
