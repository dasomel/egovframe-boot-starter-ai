package org.egovframe.boot.ai.fallback;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import java.util.List;

/**
 * LLM 호출 실패 시 표준 예외 분류·폴백 응답을 담당하는 advisor.
 * advisor 체인에서 order=200으로 PII 마스킹(order=100) 다음, 감사 로그(order=250) 이전에
 * 위치해 하위(ChatModel에 더 가까운) advisor나 실제 LLM 호출에서 발생한 예외를
 * {@link EgovLlmExceptionMapper}로 {@link EgovLlmException.Kind}에 매핑한 뒤,
 * {@code returnFallback} 설정에 따라 안내 메시지로 흡수하거나 분류된 예외로 재전파한다.
 * 감사 로그 advisor보다 바깥에 있으므로, 폴백 응답을 반환하는 경우에도 감사 이벤트에는
 * 폴백 메시지가 정상 응답처럼 기록된다.
 */
public class EgovLlmFallbackAdvisor implements CallAdvisor, StreamAdvisor {

    private final EgovLlmExceptionMapper mapper;
    private final boolean returnFallback;
    private final String fallbackMessage;
    private final int order;

    /**
     * @param mapper          예외를 {@link EgovLlmException}으로 분류하는 로직
     * @param returnFallback  true면 폴백 메시지를 응답으로 반환, false면 분류된 예외를 재전파
     * @param fallbackMessage 폴백 응답으로 사용할 메시지
     * @param order           advisor 체인 내 실행 순서(낮을수록 바깥쪽)
     */
    public EgovLlmFallbackAdvisor(EgovLlmExceptionMapper mapper, boolean returnFallback,
                                  String fallbackMessage, int order) {
        this.mapper = mapper;
        this.returnFallback = returnFallback;
        this.fallbackMessage = fallbackMessage;
        this.order = order;
    }

    /** 설정된 폴백 메시지를 단일 어시스턴트 응답으로 감싼 {@link ChatClientResponse}를 만든다. */
    private ChatClientResponse fallbackResponse() {
        ChatResponse cr = new ChatResponse(List.of(new Generation(new AssistantMessage(fallbackMessage))));
        return ChatClientResponse.builder().chatResponse(cr).build();
    }

    /**
     * 동기 호출 중 발생한 예외를 표준 분류로 변환하고, 설정에 따라 폴백 응답 또는
     * 분류된 예외를 반환한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 호출 체인
     * @return 정상 응답, 또는 {@code returnFallback=true}일 때의 폴백 응답
     * @throws EgovLlmException {@code returnFallback=false}일 때 분류된 예외를 재전파
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        try {
            return chain.nextCall(request);
        } catch (Throwable t) {
            EgovLlmException mapped = mapper.map(t);
            if (returnFallback) { return fallbackResponse(); }
            throw mapped;
        }
    }

    /**
     * 스트리밍 호출 중 방출되는 에러 신호를 동기 경로와 동일한 규칙으로 처리한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 스트림 체인
     * @return 정상 응답 스트림, 또는 {@code returnFallback} 설정에 따른 폴백 응답/에러 스트림
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(request).onErrorResume(t -> {
            EgovLlmException mapped = mapper.map(t);
            return returnFallback ? Flux.just(fallbackResponse()) : Flux.error(mapped);
        });
    }

    @Override public String getName() { return "EgovLlmFallbackAdvisor"; }
    @Override public int getOrder() { return order; }
}
