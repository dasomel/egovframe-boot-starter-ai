package org.egovframe.boot.ai.pii;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * 프롬프트 user 메시지의 개인정보를 {@link EgovPiiMasker}로 마스킹한 뒤 다음 advisor로 전달한다.
 * advisor 체인에서 SafeGuard(order=75) 다음, Fallback(order=200) 이전인 order=100에 위치해
 * 차단되지 않은 요청만 마스킹하고, 이후 감사 로그(order=250)에는 마스킹된 텍스트만 전달되도록 한다.
 * 어떤 PII 유형을 마스킹할지는 {@code egovframe.ai.pii-masking.types}로 선택할 수 있으며,
 * advisor 활성화 여부 자체는 {@code egovframe.ai.pii-masking.enabled}(기본 true)로 제어한다.
 */
public class EgovPiiMaskingAdvisor implements CallAdvisor, StreamAdvisor {

    private final EgovPiiMasker masker;
    private final int order;

    /**
     * @param masker user 메시지에 적용할 마스킹 로직
     * @param order  advisor 체인 내 실행 순서(낮을수록 바깥쪽)
     */
    public EgovPiiMaskingAdvisor(EgovPiiMasker masker, int order) {
        this.masker = masker;
        this.order = order;
    }

    /** 요청의 user 메시지 텍스트를 마스킹된 텍스트로 치환한 새 요청을 만든다. */
    private ChatClientRequest maskRequest(ChatClientRequest request) {
        Prompt masked = request.prompt().augmentUserMessage(
            um -> um.mutate().text(masker.mask(um.getText())).build());
        return request.mutate().prompt(masked).build();
    }

    /**
     * 동기 호출 경로에서 user 메시지를 마스킹한 뒤 체인의 다음 advisor로 위임한다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 호출 체인
     * @return 다음 advisor 체인이 반환한 응답
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(maskRequest(request));
    }

    /**
     * 스트리밍 호출 경로에서도 동기 경로와 동일하게 요청 단계에서 마스킹을 적용한다.
     * 마스킹 대상은 최초 요청의 user 메시지이므로 스트리밍 응답 청크에는 영향을 주지 않는다.
     *
     * @param request 원본 요청
     * @param chain   다음 advisor로 이어지는 스트림 체인
     * @return 다음 advisor 체인이 반환한 응답 스트림
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(maskRequest(request));
    }

    @Override public String getName() { return "EgovPiiMaskingAdvisor"; }
    @Override public int getOrder() { return order; }
}
