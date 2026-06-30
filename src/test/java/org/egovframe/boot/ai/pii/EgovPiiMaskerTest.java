package org.egovframe.boot.ai.pii;

import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class EgovPiiMaskerTest {

    // 기본 masker: verifyCheckdigit=false (보수적 — 패턴만 맞으면 무조건 마스킹)
    private final EgovPiiMasker masker = new EgovPiiMasker(EgovPiiType.defaultOn());

    // ── 기본 마스킹 (패턴 매칭만, 체크디지트 무시) ──

    @Test void masksRrnBackSeven() {
        // 체크디지트 유효 여부와 무관하게 패턴이 맞으면 마스킹
        assertThat(masker.mask("주민번호 900101-1234568 입니다"))
            .isEqualTo("주민번호 900101-1****** 입니다");
    }

    @Test void masksRrnWithInvalidCheckdigit() {
        // 체크디지트가 틀려도 기본 설정에서 마스킹 (보수적)
        assertThat(masker.mask("주민번호 900101-1234567"))
            .isEqualTo("주민번호 900101-1******");
    }

    @Test void masksPost2020Rrn() {
        // 2020년 10월 이후 주민번호: 검증번호 폐지되어 Modulo-11 불통과.
        // 기본 설정(checkdigit=false)에서 반드시 마스킹되어야 함 — 이것이 핵심 회귀 테스트.
        assertThat(masker.mask("개인정보 210315-3000000 보호"))
            .isEqualTo("개인정보 210315-3****** 보호");
    }

    @Test void masksArn() {
        assertThat(masker.mask("950101-5000011")).isEqualTo("950101-5******");
    }

    @Test void masksArnWithInvalidCheckdigit() {
        // ARN도 체크디지트 틀려도 기본적으로 마스킹
        assertThat(masker.mask("950101-5999999")).isEqualTo("950101-5******");
    }

    @Test void masksMobileMiddle() {
        assertThat(masker.mask("010-1234-5678")).isEqualTo("010-****-5678");
    }

    @Test void masksCard() {
        assertThat(masker.mask("1234-5678-9012-3456")).isEqualTo("1234-****-****-3456");
    }

    @Test void masksCardWithInvalidLuhn() {
        // Luhn 실패해도 기본 설정에서 마스킹
        assertThat(masker.mask("1111-2222-3333-4444")).isEqualTo("1111-****-****-4444");
    }

    @Test void masksPassport() {
        assertThat(masker.mask("M12345678")).isEqualTo("M123****");
    }

    @Test void masksBizRegNo() {
        assertThat(masker.mask("123-45-67891")).isEqualTo("123-45-*****");
    }

    @Test void masksBizRegNoWithInvalidCheckdigit() {
        // 사업자번호도 체크디지트 틀려도 기본 마스킹
        assertThat(masker.mask("123-45-67890")).isEqualTo("123-45-*****");
    }

    // ── ACCOUNT 포맷 배제 필터 (verifyCheckdigit와 무관하게 항상 동작) ──

    @Test void doesNotMaskDatesAsAccount() {
        assertThat(masker.mask("오늘 날짜는 2026-06-30 입니다")).isEqualTo("오늘 날짜는 2026-06-30 입니다");
        assertThat(masker.mask("생일 1995/05/15")).isEqualTo("생일 1995/05/15");
    }

    @Test void doesNotMaskDatesAsAccountEvenWithCheckdigitOn() {
        // verifyCheckdigit=true로 켜도 ACCOUNT 포맷 필터는 독립적으로 동작
        EgovPiiMasker strict = new EgovPiiMasker(EgovPiiType.defaultOn(), true);
        assertThat(strict.mask("결제일 2026-06-30 확인")).isEqualTo("결제일 2026-06-30 확인");
    }

    // ── 체크디지트 opt-in 모드 (verifyCheckdigit=true) ──

    @Test void checkdigitOptInRejectsInvalidRrn() {
        EgovPiiMasker strict = new EgovPiiMasker(EgovPiiType.defaultOn(), true);
        // 체크디지트 틀린 주민번호 → 마스킹 안 됨
        assertThat(strict.mask("900101-1234567")).isEqualTo("900101-1234567");
    }

    @Test void checkdigitOptInAcceptsValidRrn() {
        EgovPiiMasker strict = new EgovPiiMasker(EgovPiiType.defaultOn(), true);
        // 체크디지트 맞는 주민번호 → 마스킹 됨
        assertThat(strict.mask("900101-1234568")).isEqualTo("900101-1******");
    }

    @Test void checkdigitOptInRejectsInvalidCard() {
        EgovPiiMasker strict = new EgovPiiMasker(EgovPiiType.defaultOn(), true);
        assertThat(strict.mask("1234-5678-9012-3456")).isEqualTo("1234-5678-9012-3456");
    }

    @Test void checkdigitOptInAcceptsValidCard() {
        EgovPiiMasker strict = new EgovPiiMasker(EgovPiiType.defaultOn(), true);
        // Luhn 통과하는 카드번호
        assertThat(strict.mask("1234-5678-9012-3452")).isEqualTo("1234-****-****-3452");
    }

    @Test void checkdigitOptInRejectsInvalidBizRegNo() {
        EgovPiiMasker strict = new EgovPiiMasker(EgovPiiType.defaultOn(), true);
        assertThat(strict.mask("123-45-67890")).isEqualTo("123-45-67890");
    }

    @Test void checkdigitOptInAcceptsValidBizRegNo() {
        EgovPiiMasker strict = new EgovPiiMasker(EgovPiiType.defaultOn(), true);
        assertThat(strict.mask("123-45-67891")).isEqualTo("123-45-*****");
    }

    // ── 일반 검증 ──

    @Test void leavesPlainTextUnchanged() {
        assertThat(masker.mask("안녕하세요 반갑습니다")).isEqualTo("안녕하세요 반갑습니다");
    }

    @Test void nullAndBlankReturnedAsIs() {
        assertThat(masker.mask(null)).isNull();
        assertThat(masker.mask("   ")).isEqualTo("   ");
    }

    @Test void disabledTypeNotMasked() {
        EgovPiiMasker only = new EgovPiiMasker(EnumSet.of(EgovPiiType.MOBILE));
        assertThat(only.mask("900101-1234568")).isEqualTo("900101-1234568");
    }

    @Test void emailMaskedOnlyWhenEnabled() {
        EgovPiiMasker withEmail = new EgovPiiMasker(EnumSet.of(EgovPiiType.EMAIL));
        assertThat(withEmail.mask("hong@test.com")).isEqualTo("ho****@test.com");
    }
}
