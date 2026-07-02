package org.egovframe.boot.ai.safeguard;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 금칙어·프롬프트 인젝션 탐지 순수 로직. {@link EgovAiSafeGuardAdvisor}가 advisor 체인에서
 * 이 클래스를 호출해 요청 차단 여부를 판정한다. 스프링이나 advisor API에 의존하지 않으므로
 * 단독으로 단위 테스트할 수 있다.
 */
public class EgovAiSafeGuardChecker {

    /**
     * 프롬프트 인젝션 탐지 패턴. 영문 정형 문구(시스템 프롬프트 유출·이전 지시 무시·개발자 모드
     * 요구)와 한국어 변형 표현을 함께 등록한다. 완전한 인젝션 방어는 불가능하므로 이 목록은
     * 자주 관찰되는 대표 패턴만 다루는 1차 방어선이며, {@code detectInjection}으로 켜고 끌 수 있다.
     */
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

    /**
     * @param blockedWords    차단할 금칙어 목록(대소문자 무시). null이면 빈 목록으로 취급한다.
     * @param detectInjection 프롬프트 인젝션 패턴 탐지 활성화 여부
     */
    public EgovAiSafeGuardChecker(List<String> blockedWords, boolean detectInjection) {
        this.blockedWords = blockedWords != null ? blockedWords : List.of();
        this.detectInjection = detectInjection;
    }

    /**
     * 입력 텍스트를 검사한다. 금칙어를 먼저 검사하고(더 저렴한 문자열 포함 검사), 위반이 없으면
     * {@code detectInjection}이 켜져 있을 때만 정규식 기반 인젝션 패턴을 검사한다. 첫 위반을
     * 찾는 즉시 반환하며, 둘 다 위반이어도 사유는 하나만 보고한다.
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
