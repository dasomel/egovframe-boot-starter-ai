package org.egovframe.boot.ai.usage;

/** 토큰 사용량을 표준 로그 문자열로 변환하는 순수 로직. */
public class EgovTokenUsageFormatter {

    public String format(Integer prompt, Integer completion, Integer total) {
        return "LLM token usage - prompt: " + nz(prompt)
                + ", completion: " + nz(completion)
                + ", total: " + nz(total);
    }

    private int nz(Integer v) { return v == null ? 0 : v; }
}
