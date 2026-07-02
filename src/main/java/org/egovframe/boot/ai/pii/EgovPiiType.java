package org.egovframe.boot.ai.pii;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * {@link EgovPiiMasker}가 텍스트에 적용하는 개인정보 유형과 유형별 정규식·마스킹 규칙을 정의한다.
 * 근거는 개인정보 보호법 시행령 제19조(고유식별정보), 신용정보법·전자금융거래법(금융 식별정보),
 * ISMS-P 3.2.3(개인정보 및 중요정보 관리)이다. enum 상수 선언 순서가 곧 마스킹 적용 우선순위이며,
 * {@link EgovPiiMasker#mask}가 이 순서대로 순차 치환하므로 특이하거나 긴 패턴(RRN·ARN·CARD 등)을
 * 앞쪽에, 느슨하고 오탐 위험이 큰 패턴(ACCOUNT·EMAIL 등)을 뒤쪽에 배치한다.
 *
 * <p>마스킹 가드레일의 안전 원칙: 패턴이 일치하면 기본적으로 무조건 마스킹한다(보수적).
 * 체크디지트 검증({@link #isCheckdigitValid})은 opt-in으로, 데이터 품질이 보장되는 환경에서만
 * 활성화한다. 2020년 10월 이후 발급·변경된 주민등록번호는 검증번호(체크섬)가 폐지되어
 * Modulo-11을 통과하지 못하므로, 체크디지트 검증을 기본 활성화하면 실제 PII가 마스킹되지 않고
 * 그대로 LLM에 전달되는 누출 사고로 이어진다. 반면 포맷 배제 필터({@link #isFormatValid})는
 * 다른 유형과의 패턴 충돌을 막기 위한 것으로 검증번호와 무관하게 항상 적용한다.</p>
 */
public enum EgovPiiType {

    /**
     * 주민등록번호. 개인정보 보호법 시행령 제19조 제1호(고유식별정보). 뒷자리 첫 숫자로
     * 성별을 구분하는 형식(예: 900101-1234567)만 매칭하며, 뒤 6자리를 마스킹한다.
     * 기본 활성(activation) — {@link #defaultOn()} 참고.
     */
    RRN("(?<![0-9])\\d{6}[-\\s]?[1-4]\\d{6}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 6) + "-" + d.charAt(6) + "******";
        }
        @Override boolean isCheckdigitValid(String s) {
            String d = digits(s);
            if (d.length() != 13) return false;
            int[] weights = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5};
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                sum += Character.getNumericValue(d.charAt(i)) * weights[i];
            }
            int check = (11 - (sum % 11)) % 10;
            return check == Character.getNumericValue(d.charAt(12));
        }
    },
    /**
     * 외국인등록번호. 개인정보 보호법 시행령 제19조 제4호(고유식별정보), 출입국관리법 제31조.
     * 뒷자리 첫 숫자가 5~8인 점으로 RRN(1~4)과 구분한다. 기본 활성.
     */
    ARN("(?<![0-9])\\d{6}[-\\s]?[5-8]\\d{6}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 6) + "-" + d.charAt(6) + "******";
        }
        @Override boolean isCheckdigitValid(String s) {
            // ARN은 전통적으로 RRN과 동일한 Modulo-11 공식을 사용한다.
            String d = digits(s);
            if (d.length() != 13) return false;
            int[] weights = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5};
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                sum += Character.getNumericValue(d.charAt(i)) * weights[i];
            }
            int check = (11 - (sum % 11)) % 10;
            return check == Character.getNumericValue(d.charAt(12));
        }
    },
    /**
     * 신용/체크카드 번호. 신용정보의 이용 및 보호에 관한 법률상 금융 식별정보. 16자리
     * 번호 중 앞 4자리·뒤 4자리만 남기고 중간 8자리를 마스킹한다. 기본 활성.
     *
     * <p>{@link #isCheckdigitValid}는 카드 번호 발급 규격인 Luhn 알고리즘(모드 10)을 구현한다.
     * 오른쪽 끝에서부터 한 자리씩 건너뛰며 2배로 만들고 자릿수 합이 9를 넘으면 9를 빼서
     * (십의 자리와 일의 자리를 합산하는 것과 동일) 자릿수 합을 10 미만으로 유지한 뒤, 전체
     * 합이 10의 배수인지로 유효성을 판정한다.</p>
     */
    CARD("(?<![0-9])\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 4) + "-****-****-" + d.substring(12);
        }
        @Override boolean isCheckdigitValid(String s) {
            String d = digits(s);
            if (d.length() < 13 || d.length() > 19) return false;
            int sum = 0;
            boolean alternate = false;
            // Luhn 알고리즘: 오른쪽 끝부터 한 자리 건너 2배, 9 초과 시 자릿수 합산(=9를 뺀 값)
            for (int i = d.length() - 1; i >= 0; i--) {
                int n = Character.getNumericValue(d.charAt(i));
                if (alternate) {
                    n *= 2;
                    if (n > 9) {
                        n = (n % 10) + 1;
                    }
                }
                sum += n;
                alternate = !alternate;
            }
            return sum % 10 == 0;
        }
    },

    /**
     * 여권번호. 개인정보 보호법 시행령 제19조 제2호(고유식별정보). 발급 국가·유형을 나타내는
     * 선행 알파벳(M/S/R/O/D)에 8자리 숫자 또는 3자리 숫자+알파벳+4자리 숫자가 뒤따르는
     * 대한민국 여권 번호 체계를 매칭한다. 앞 4자만 남기고 나머지를 마스킹한다. 기본 활성.
     */
    PASSPORT("[MSROD](?:\\d{8}|\\d{3}[A-Z]\\d{4})") {
        @Override String mask(String s) {
            return s.substring(0, 4) + "****";
        }
    },

    /**
     * 운전면허번호. 개인정보 보호법 시행령 제19조 제3호(고유식별정보). "지역코드-발급연도-일련번호-검증숫자"
     * 형식(2+2+6+2자리)을 매칭하며, 앞 4자리(지역코드+발급연도)만 남기고 나머지를 마스킹한다.
     * 기본 활성. 별도의 체크디지트 검증 규칙은 지역별로 상이해 구현하지 않는다.
     */
    DRIVER_LICENSE("(?<![0-9])\\d{2}[-\\s]?\\d{2}[-\\s]?\\d{6}[-\\s]?\\d{2}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 4) + "*".repeat(d.length() - 4);
        }
    },

    /**
     * 사업자등록번호. 부가가치세법 제8조에 근거한 과세사업자 식별번호로, 개인정보 보호법상
     * 고유식별정보는 아니지만 개인사업자의 경우 실질적으로 개인을 특정할 수 있어 마스킹
     * 대상에 포함한다. "관할서코드-개인/법인구분-일련번호+검증숫자" 형식(3+2+5자리)을
     * 매칭하며, 앞 5자리만 남기고 나머지를 마스킹한다. 기본 활성.
     *
     * <p>{@link #isCheckdigitValid}는 국세청 고시 사업자등록번호 검증 공식을 구현한다.
     * 앞 8자리에 고정 가중치({1,3,7,1,3,7,1,3})를 곱해 합산하고, 9번째 자리에는 가중치 5를
     * 곱한 값의 십의 자리와 일의 자리를 더해 합산에 반영한 뒤, 10에서 그 합의 일의 자리를
     * 뺀 값(10의 배수면 0)을 10번째 자리(검증숫자)와 비교한다.</p>
     */
    BIZ_REG_NO("(?<![0-9])\\d{3}[-\\s]?\\d{2}[-\\s]?\\d{5}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 3) + "-" + d.substring(3, 5) + "-*****";
        }
        @Override boolean isCheckdigitValid(String s) {
            String d = digits(s);
            if (d.length() != 10) return false;
            int[] weights = {1, 3, 7, 1, 3, 7, 1, 3, 5};
            int sum = 0;
            for (int i = 0; i < 8; i++) {
                sum += Character.getNumericValue(d.charAt(i)) * weights[i];
            }
            int lastWeightValue = Character.getNumericValue(d.charAt(8)) * weights[8];
            sum += (lastWeightValue / 10) + (lastWeightValue % 10);
            int check = (10 - (sum % 10)) % 10;
            return check == Character.getNumericValue(d.charAt(9));
        }
    },
    /**
     * 휴대폰번호. 개인정보 보호법상 개인을 식별할 수 있는 연락처 정보. 010/011/016/017/018/019
     * 국번으로 시작하는 010-1234-5678, 011-123-4567 형식을 매칭하며, 국번 3자리와 뒤 4자리만
     * 남기고 중간을 마스킹한다. 기본 활성.
     */
    MOBILE("(?<![0-9])01[016789][-\\s]?\\d{3,4}[-\\s]?\\d{4}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 3) + "-****-" + d.substring(d.length() - 4);
        }
    },

    /**
     * 계좌번호. 전자금융거래법상 금융거래 식별정보. 은행마다 자릿수·구분 방식이 제각각이라
     * "2~4자리-2~6자리-2~7자리" 형태의 느슨한 패턴으로 광범위하게 매칭한다. 앞 2자리·뒤 4자리만
     * 남기고 마스킹한다. 기본 활성이지만 README의 "한계 및 미검증 항목"에 명시된 대로 보편적인
     * 체크섬이 없어 다른 숫자열과의 오탐 가능성이 상대적으로 높은 유형이다.
     *
     * <p>{@link #isFormatValid}가 verifyCheckdigit 설정과 무관하게 항상 적용되는 포맷 배제
     * 필터로 이 오탐을 줄인다 — 자세한 배제 규칙은 해당 메서드 재정의부 참고.</p>
     */
    ACCOUNT("(?<![0-9])\\d{2,4}[-\\s]?\\d{2,6}[-\\s]?\\d{2,7}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            if (d.length() < 7) return s;
            return d.substring(0, 2) + "*".repeat(d.length() - 6) + d.substring(d.length() - 4);
        }
        /**
         * ACCOUNT의 포맷 배제 필터 — verifyCheckdigit 설정과 무관하게 항상 적용.
         * 느슨한 정규식이 날짜·전화·카드·주민번호 등 다른 유형과 충돌하는 것을 방지한다.
         * 아래 배제 규칙은 위에서부터 순서대로 판정하며, 하나라도 해당하면 즉시 false를 반환해
         * ACCOUNT로 마스킹하지 않고 원문 그대로 둔다(해당 텍스트는 다른 유형의 매칭에 맡긴다).
         */
        @Override boolean isFormatValid(String s, java.util.regex.MatchResult mr, String text) {
            // 주민/외국인등록번호 형태 배제
            if (Pattern.matches("\\d{6}[-\\s]?[1-8]\\d{6}", s)) {
                return false;
            }
            // 사업자등록번호 형태 배제
            if (Pattern.matches("\\d{3}[-\\s]?\\d{2}[-\\s]?\\d{5}", s)) {
                return false;
            }
            // 카드번호 형태 배제
            if (Pattern.matches("\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}", s)) {
                return false;
            }
            // 카드번호 등 더 긴 숫자열의 부분 매칭 방지 (앞뒤 문맥 확인).
            // ACCOUNT 정규식은 최대 17자리까지만 매칭하므로 매칭 경계 바로 앞/뒤에 숫자가
            // 더 이어지면 실제로는 더 긴 숫자열(카드번호 등)의 일부일 가능성이 높아 배제한다.
            int end = mr.end();
            if (end < text.length()) {
                String remaining = text.substring(end);
                if (remaining.matches("^[-\\s]?\\d+.*")) {
                    return false;
                }
            }
            int start = mr.start();
            if (start > 0) {
                String preceding = text.substring(0, start);
                if (preceding.matches(".*\\d+[-\\s]?$")) {
                    return false;
                }
            }
            // 날짜 형태 배제
            if (Pattern.matches("(?:19|20)?\\d{2}[-\\s/.]?(?:0[1-9]|1[0-2])[-\\s/.]?(?:0[1-9]|[12]\\d|3[01])", s)) {
                return false;
            }
            // 전화번호 형태 배제
            if (Pattern.matches("0\\d{1,2}[-\\s]?\\d{3,4}[-\\s]?\\d{4}", s)) {
                return false;
            }
            return true;
        }
    },

    /**
     * 이메일 주소. 개인정보 보호법상 연락처 정보로 분류될 수 있으나, 업무용 대표 메일 등
     * 개인 식별력이 낮은 경우가 많아 과탐(불필요한 마스킹) 위험이 커 기본 비활성이다.
     * 로컬파트 앞 2자만 남기고 나머지와 `@` 앞부분을 마스킹한다.
     */
    EMAIL("[A-Za-z0-9._%+\\-]{1,}@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}") {
        @Override String mask(String s) {
            int at = s.indexOf('@');
            String id = s.substring(0, at);
            String shown = id.length() <= 2 ? id : id.substring(0, 2);
            return shown + "****" + s.substring(at);
        }
    },

    /**
     * 일반(유선) 전화번호. 지역번호(02, 031 등)로 시작하는 번호를 매칭한다. 대표번호·안내번호처럼
     * 개인 식별력이 낮은 번호가 섞여 있고 다른 숫자열과 혼동될 여지가 커 기본 비활성이다.
     */
    PHONE("(?<![0-9])0[2-9]\\d?[-\\s]?\\d{3,4}[-\\s]?\\d{4}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, d.length() - 8) + "-****-" + d.substring(d.length() - 4);
        }
    },

    /**
     * 생년월일(YYYYMMDD). 단독으로는 개인 식별력이 낮고 날짜·일련번호 등 일반 숫자열과
     * 오탐 가능성이 높아 기본 비활성이다. 앞 4자리(연도)만 남기고 나머지를 마스킹한다.
     */
    BIRTHDATE("(?<![0-9])(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])(?![0-9])") {
        @Override String mask(String s) {
            return s.substring(0, 4) + "****";
        }
    },

    /**
     * IPv4 주소. 특정 상황에서 개인 식별에 활용될 수 있는 정보이나, 로그·설정값 등 업무상
     * 정상적인 노출이 잦아 과탐 위험이 커 기본 비활성이다. 앞 2옥텟만 남기고 마스킹한다.
     */
    IP("(?<![0-9.])(?:\\d{1,3}\\.){3}\\d{1,3}(?![0-9.])") {
        @Override String mask(String s) {
            String[] o = s.split("\\.");
            return o[0] + "." + o[1] + ".*.*";
        }
    };

    private final Pattern pattern;
    EgovPiiType(String regex) { this.pattern = Pattern.compile(regex); }

    /** 매칭된 원문(matched)을 이 유형의 규칙대로 마스킹한 문자열로 변환한다. 상수별로 재정의한다. */
    abstract String mask(String matched);

    /**
     * 포맷 배제 필터 — 항상 적용. verifyCheckdigit 설정과 무관.
     * 정규식이 느슨해서 다른 유형과 충돌하는 경우(예: ACCOUNT)에 사용한다.
     * 기본 구현은 항상 true(포맷 유효)를 반환한다.
     */
    boolean isFormatValid(String matched, java.util.regex.MatchResult mr, String text) { return true; }

    /**
     * 수학적 체크디지트 검증 — verifyCheckdigit=true 일 때만 적용.
     * 기본 구현은 항상 true(검증 통과)를 반환한다.
     *
     * <p>주의: 2020년 10월 이후 주민등록번호(RRN)·외국인등록번호(ARN)는 검증번호가 폐지되어
     * Modulo-11 체크섬을 통과하지 못한다. 이 옵션을 기본 활성화하면 실제 PII가 누출되므로
     * 반드시 opt-in(기본 false)으로 운용해야 한다.</p>
     */
    boolean isCheckdigitValid(String matched) { return true; }

    /**
     * 본 유형의 패턴을 text 전체에 적용해 마스킹한 결과를 반환한다.
     *
     * <p>포맷 배제 필터({@link #isFormatValid})는 항상 적용되고,
     * 체크디지트 검증({@link #isCheckdigitValid})은 verifyCheckdigit가 true일 때만 적용된다.</p>
     */
    String maskAll(String text, boolean verifyCheckdigit) {
        return pattern.matcher(text).replaceAll(mr -> {
            String matched = mr.group();
            // 포맷 배제 필터: 항상 적용 (ACCOUNT 충돌 방지 등)
            if (!isFormatValid(matched, mr, text)) {
                return java.util.regex.Matcher.quoteReplacement(matched);
            }
            // 체크디지트 검증: opt-in일 때만 적용
            if (verifyCheckdigit && !isCheckdigitValid(matched)) {
                return java.util.regex.Matcher.quoteReplacement(matched);
            }
            return java.util.regex.Matcher.quoteReplacement(mask(matched));
        });
    }

    static String digits(String s) { return s.replaceAll("\\D", ""); }

    /** 기본 활성(고유식별정보 + 금융 + 휴대폰). 이름·주소·이메일·일반전화·생년월일·IP는 과탐 위험으로 기본 OFF. */
    public static Set<EgovPiiType> defaultOn() {
        return EnumSet.of(RRN, ARN, PASSPORT, DRIVER_LICENSE, BIZ_REG_NO, CARD, ACCOUNT, MOBILE);
    }
}
