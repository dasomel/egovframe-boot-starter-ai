package org.egovframe.boot.ai.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 응답의 토큰 사용량을 표준 포맷으로 로깅하는 advisor.
 * advisor 체인에서 order=300으로 가장 안쪽(ChatModel과 가장 가까운 쪽)에 위치한다 —
 * 앞선 advisor(Trace/SafeGuard/PII/Fallback/Audit)를 모두 통과해 실제로 LLM이 호출된
 * 경우에만 토큰 사용량이 존재하므로, 이 위치가 자연스럽다.
 */
public class EgovTokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovTokenUsageAdvisor.class);

    private final EgovTokenUsageFormatter formatter;
    private final int order;

    /**
     * @param formatter 로그 문자열 생성기
     * @param order     advisor 체인 내 실행 순서(낮을수록 바깥쪽)
     */
    public EgovTokenUsageAdvisor(EgovTokenUsageFormatter formatter, int order) {
        this.formatter = formatter;
        this.order = order;
    }

    /** 응답에 Usage 메타데이터가 있으면 포맷해 INFO로 로깅한다. 없으면 조용히 넘어간다(폴백 응답 등). */
    private void logUsage(ChatClientResponse response) {
        ChatResponse cr = response == null ? null : response.chatResponse();
        if (cr == null || cr.getMetadata() == null) { return; }
        Usage u = cr.getMetadata().getUsage();
        if (u == null) { return; }
        log.info(formatter.format(u.getPromptTokens(), u.getCompletionTokens(), u.getTotalTokens()));
    }

    /**
     * 동기 호출 완료 후 응답의 토큰 사용량을 로깅하고 응답을 그대로 반환한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 호출 체인
     * @return 다음 advisor 체인이 반환한 응답(변경 없이 그대로 반환)
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        logUsage(response);
        return response;
    }

    /**
     * 스트리밍 응답의 각 청크마다 Usage 메타데이터 존재 여부를 확인해 로깅한다.
     * 일반적으로 Usage는 스트림의 마지막 청크에만 채워지므로 그 시점에만 실제로 로그가 남는다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 스트림 체인
     * @return 다음 advisor 체인이 반환한 응답 스트림(변경 없이 그대로 반환)
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(request).doOnNext(this::logUsage);
    }

    @Override public String getName() { return "EgovTokenUsageAdvisor"; }
    @Override public int getOrder() { return order; }
}
