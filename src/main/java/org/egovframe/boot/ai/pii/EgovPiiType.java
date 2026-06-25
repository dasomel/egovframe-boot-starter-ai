package org.egovframe.boot.ai.pii;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 개인정보보호법 시행령 제19조(고유식별정보)·안전성 확보조치 기준·ISMS-P 3.2.3 기준 마스킹 유형.
 * 적용 순서는 오탐 회피를 위해 특이/긴 패턴을 앞에 선언한다.
 */
public enum EgovPiiType {
    RRN("(?<![0-9])\\d{6}[-\\s]?[1-4]\\d{6}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 6) + "-" + d.charAt(6) + "******";
        }
    },
    ARN("(?<![0-9])\\d{6}[-\\s]?[5-8]\\d{6}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 6) + "-" + d.charAt(6) + "******";
        }
    },
    CARD("(?<![0-9])\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}(?![0-9])") {
        @Override String mask(String s) {
            String d = digits(s);
            return d.substring(0, 4) + "-****-****-" + d.substring(12);
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

    /** 본 유형의 패턴을 text 전체에 적용해 마스킹한 결과를 반환한다. */
    String maskAll(String text) {
        return pattern.matcher(text).replaceAll(mr -> java.util.regex.Matcher.quoteReplacement(mask(mr.group())));
    }

    static String digits(String s) { return s.replaceAll("\\D", ""); }

    /** 기본 활성(고유식별정보 + 금융 + 휴대폰). 이름·주소·이메일·일반전화·생년월일·IP는 과탐 위험으로 기본 OFF. */
    public static Set<EgovPiiType> defaultOn() {
        return EnumSet.of(RRN, ARN, PASSPORT, DRIVER_LICENSE, CARD, ACCOUNT, MOBILE);
    }
}
