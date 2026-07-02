package org.egovframe.boot.ai.trace;

/**
 * AI 호출 추적 로그 문자열을 생성하는 순수 로직.
 * {@link EgovAiTraceAdvisor}(advisor 체인 최외곽, order=50)가 호출 시작/종료 시점에 이 클래스로
 * 로그 문자열을 만들어 남긴다. 로깅 프레임워크나 advisor API에 의존하지 않으므로
 * 외부 의존성 없이 단독으로 단위 테스트 가능하다.
 */
public class EgovAiTraceLogFormatter {

    /**
     * 호출 시작 로그 문자열을 생성한다.
     *
     * @param traceId     추적 ID
     * @param model       모델 이름(없으면 null)
     * @param promptChars 프롬프트 문자 수(없으면 null)
     * @return 시작 로그 문자열
     */
    public String start(String traceId, String model, Integer promptChars) {
        StringBuilder sb = new StringBuilder("AI 호출 시작 - traceId=").append(traceId);
        if (model != null) {
            sb.append(", model=").append(model);
        }
        if (promptChars != null) {
            sb.append(", promptChars=").append(promptChars);
        }
        return sb.toString();
    }

    /**
     * 호출 종료 로그 문자열을 생성한다.
     *
     * @param traceId   추적 ID
     * @param elapsedMs 소요 시간(밀리초)
     * @param error     오류 발생 여부
     * @return 종료 로그 문자열
     */
    public String end(String traceId, long elapsedMs, boolean error) {
        return "AI 호출 종료 - traceId=" + traceId
                + ", elapsedMs=" + elapsedMs
                + ", status=" + (error ? "ERROR" : "OK");
    }
}
