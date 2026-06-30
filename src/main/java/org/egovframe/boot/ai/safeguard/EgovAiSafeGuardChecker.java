package org.egovframe.boot.ai.safeguard;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** 금칙어·프롬프트 인젝션 탐지 순수 로직. */
public class EgovAiSafeGuardChecker {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        Pattern.compile("ignore\\s+(all\\s+)?previous\\s+instructions", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\\s+prompt", Pattern.CASE_INSENSITIVE),
        Pattern.compile("developer\\s+mode", Pattern.CASE_INSENSITIVE),
        Pattern.compile("역할을\\s*무시", Pattern.CASE_INSENSITIVE),
        Pattern.compile("이전\\s*지시(?:를)?\\s*무시", Pattern.CASE_INSENSITIVE),
        Pattern.compile("프롬프트를\\s*출력", Pattern.CASE_INSENSITIVE)
    );

    private final List<String> blockedWords;
    private final boolean detectInjection;

    public EgovAiSafeGuardChecker(List<String> blockedWords, boolean detectInjection) {
        this.blockedWords = blockedWords != null ? blockedWords : List.of();
        this.detectInjection = detectInjection;
    }

    /**
     * 입력 텍스트를 검사한다.
     *
     * @param userText 사용자 입력 텍스트
     * @return 위반 시 사유 문자열, 정상이면 empty
     */
    public Optional<String> check(String userText) {
        if (userText == null || userText.isBlank()) {
            return Optional.empty();
        }
        String lower = userText.toLowerCase();
        for (String word : blockedWords) {
            if (word != null && !word.isBlank() && lower.contains(word.toLowerCase())) {
                return Optional.of("금칙어: " + word);
            }
        }
        if (detectInjection) {
            for (Pattern pattern : INJECTION_PATTERNS) {
                if (pattern.matcher(userText).find()) {
                    return Optional.of("프롬프트 인젝션 패턴 탐지");
                }
            }
        }
        return Optional.empty();
    }
}
