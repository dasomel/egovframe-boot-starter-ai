package org.egovframe.boot.ai.pii;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 개인정보보호법 시행령 제19조(고유식별정보)·안전성 확보조치 기준·ISMS-P 3.2.3 기준 마스킹 유형.
 * 적용 순서는 오탐 회피를 위해 특이/긴 패턴을 앞에 선언한다.
 *
 * <p>마스킹 가드레일의 안전 원칙: 패턴이 일치하면 기본적으로 무조건 마스킹한다(보수적).
 * 체크디지트 검증은 opt-in으로, 데이터 품질이 보장되는 환경에서만 활성화한다.
 * 2020년 10월 이후 주민등록번호는 검증번호가 폐지되어 Modulo-11을 통과하지 못하므로,
 * 체크디지트 기본 활성 시 실제 PII가 누출된다.</p>
 */
public enum EgovPiiType {
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
    PASSPORT("[MSROD](?:\\d{8}|\\d{3}[A-Z]\\d{4})") {
        @Override String mask(String s) {
            return s.substring(0, 4) + "****";
        }
    },
    DRIVER_LICENSE("(?<![0-9])\\d{2}[-\\s]?\\d{2}[-\\s]?\\d{6}[-\\s]?\\d{2}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 4) + "*".repeat(d.length() - 4);
        }
    },
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
    MOBILE("(?<![0-9])01[016789][-\\s]?\\d{3,4}[-\\s]?\\d{4}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 3) + "-****-" + d.substring(d.length() - 4);
        }
    },
    ACCOUNT("(?<![0-9])\\d{2,4}[-\\s]?\\d{2,6}[-\\s]?\\d{2,7}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            if (d.length() < 7) return s;
            return d.substring(0, 2) + "*".repeat(d.length() - 6) + d.substring(d.length() - 4);
        }
        /**
         * ACCOUNT의 포맷 배제 필터 — verifyCheckdigit 설정과 무관하게 항상 적용.
         * 느슨한 정규식이 날짜·전화·카드·주민번호 등 다른 유형과 충돌하는 것을 방지한다.
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
            // 카드번호 등 더 긴 숫자열의 부분 매칭 방지 (앞뒤 문맥 확인)
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
    EMAIL("[A-Za-z0-9._%+\\-]{1,}@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}") {
        @Override String mask(String s) {
            int at = s.indexOf('@');
            String id = s.substring(0, at);
            String shown = id.length() <= 2 ? id : id.substring(0, 2);
            return shown + "****" + s.substring(at);
        }
    },
    PHONE("(?<![0-9])0[2-9]\\d?[-\\s]?\\d{3,4}[-\\s]?\\d{4}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, d.length() - 8) + "-****-" + d.substring(d.length() - 4);
        }
    },
    BIRTHDATE("(?<![0-9])(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])(?![0-9])") {
        @Override String mask(String s) {
            return s.substring(0, 4) + "****";
        }
    },
    IP("(?<![0-9.])(?:\\d{1,3}\\.){3}\\d{1,3}(?![0-9.])") {
        @Override String mask(String s) {
            String[] o = s.split("\\.");
            return o[0] + "." + o[1] + ".*.*";
        }
    };

    private final Pattern pattern;
    EgovPiiType(String regex) { this.pattern = Pattern.compile(regex); }

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
