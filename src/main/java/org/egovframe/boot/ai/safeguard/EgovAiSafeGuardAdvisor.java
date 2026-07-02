package org.egovframe.boot.ai.safeguard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * 금칙어·프롬프트 인젝션 탐지 시 LLM 호출 없이 즉시 차단 응답을 반환하는 advisor.
 * advisor 체인에서 order=75로 Trace(order=50) 다음, PII 마스킹(order=100) 이전에 위치한다.
 * PII 마스킹보다 앞에 두는 이유는, 위반이 확정된 요청까지 마스킹 비용을 들일 필요가 없고
 * LLM 호출 자체를 막아야 하기 때문이다 — 위반 시 {@link EgovAiSafeGuardChecker}가 이유를
 * 반환하고, 이 advisor는 하위 체인({@code chain.nextCall}/{@code nextStream})을 아예
 * 호출하지 않은 채 차단 응답을 즉시 반환한다.
 */
public class EgovAiSafeGuardAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EgovAiSafeGuardAdvisor.class);

    private final EgovAiSafeGuardChecker checker;
    private final String blockMessage;
    private final int order;

    /**
     * @param checker      금칙어·인젝션 판정 로직
     * @param blockMessage 위반 시 반환할 안내 메시지
     * @param order        advisor 체인 내 실행 순서(낮을수록 바깥쪽)
     */
    public EgovAiSafeGuardAdvisor(EgovAiSafeGuardChecker checker, String blockMessage, int order) {
        this.checker = checker;
        this.blockMessage = blockMessage;
        this.order = order;
    }

    /** 요청의 user 메시지 텍스트를 조회한다. 메시지가 없거나 조회 중 예외가 발생하면 null. */
    private String resolveUserText(ChatClientRequest request) {
        try {
            return request.prompt().getUserMessage().getText();
        } catch (Exception e) {
            return null;
        }
    }

    /** 설정된 차단 메시지를 단일 어시스턴트 응답으로 감싼 {@link ChatClientResponse}를 만든다. */
    private ChatClientResponse blockResponse() {
        ChatResponse cr = new ChatResponse(List.of(new Generation(new AssistantMessage(blockMessage))));
        return ChatClientResponse.builder().chatResponse(cr).build();
    }

    /**
     * user 메시지를 검사해 위반이면 LLM을 호출하지 않고 즉시 차단 응답을 반환하고,
     * 정상이면 다음 advisor로 위임한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 호출 체인
     * @return 정상 시 다음 advisor 체인의 응답, 위반 시 차단 응답
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Optional<String> violation = checker.check(resolveUserText(request));
        if (violation.isPresent()) {
            log.warn("[SafeGuard] 요청 차단 — 사유: {}", violation.get());
            return blockResponse();
        }
        return chain.nextCall(request);
    }

    /**
     * 스트리밍 호출에서도 동기 경로와 동일하게 요청 단계에서 검사한다. 위반 시 단일 원소
     * {@link Flux}로 차단 응답을 반환해, 정상 스트림과 동일한 반환 타입을 유지한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 스트림 체인
     * @return 정상 시 다음 advisor 체인의 응답 스트림, 위반 시 차단 응답 1건짜리 스트림
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Optional<String> violation = checker.check(resolveUserText(request));
        if (violation.isPresent()) {
            log.warn("[SafeGuard] 스트리밍 요청 차단 — 사유: {}", violation.get());
            return Flux.just(blockResponse());
        }
        return chain.nextStream(request);
    }

    @Override public String getName() { return "EgovAiSafeGuardAdvisor"; }
    @Override public int getOrder() { return order; }
}
