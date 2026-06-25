package org.egovframe.boot.ai.pii;

import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class EgovPiiMaskerTest {
    private final EgovPiiMasker masker = new EgovPiiMasker(EgovPiiType.defaultOn());

    @Test void masksRrnBackSeven() {
        assertThat(masker.mask("주민번호 900101-1234567 입니다"))
            .isEqualTo("주민번호 900101-1****** 입니다");
    }
    @Test void masksArn() {
        assertThat(masker.mask("950101-5234567")).isEqualTo("950101-5******");
    }
    @Test void masksMobileMiddle() {
        assertThat(masker.mask("010-1234-5678")).isEqualTo("010-****-5678");
    }
    @Test void masksCard() {
        assertThat(masker.mask("1234-5678-9012-3456")).isEqualTo("1234-****-****-3456");
    }
    @Test void masksPassport() {
        assertThat(masker.mask("M12345678")).isEqualTo("M123****");
    }
    @Test void leavesPlainTextUnchanged() {
        assertThat(masker.mask("안녕하세요 반갑습니다")).isEqualTo("안녕하세요 반갑습니다");
    }
    @Test void nullAndBlankReturnedAsIs() {
        assertThat(masker.mask(null)).isNull();
        assertThat(masker.mask("   ")).isEqualTo("   ");
    }
    @Test void disabledTypeNotMasked() {
        EgovPiiMasker only = new EgovPiiMasker(EnumSet.of(EgovPiiType.MOBILE));
        assertThat(only.mask("900101-1234567")).isEqualTo("900101-1234567");
    }
    @Test void emailMaskedOnlyWhenEnabled() {
        EgovPiiMasker withEmail = new EgovPiiMasker(EnumSet.of(EgovPiiType.EMAIL));
        assertThat(withEmail.mask("hong@test.com")).isEqualTo("ho****@test.com");
    }
}
