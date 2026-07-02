package org.egovframe.boot.ai.usage;

/**
 * 토큰 사용량을 표준 로그 문자열로 변환하는 순수 로직.
 * {@link EgovTokenUsageAdvisor}가 응답의 {@code Usage} 메타데이터를 이 클래스로 포맷해 로깅한다.
 */
public class EgovTokenUsageFormatter {

    /**
     * @param prompt     입력(prompt) 토큰 수. 제공되지 않으면 0으로 표기한다.
     * @param completion 출력(completion) 토큰 수. 제공되지 않으면 0으로 표기한다.
     * @param total      전체 토큰 수. 제공되지 않으면 0으로 표기한다.
     * @return {@code "LLM token usage - prompt: N, completion: M, total: K"} 형식의 로그 문자열
     */
    public String format(Integer prompt, Integer completion, Integer total) {
        return "LLM token usage - prompt: " + nz(prompt)
                + ", completion: " + nz(completion)
                + ", total: " + nz(total);
    }

    /** null을 0으로 치환한다 — 일부 모델·제공자는 Usage 메타데이터를 채우지 않을 수 있다. */
    private int nz(Integer v) { return v == null ? 0 : v; }
}
