package org.egovframe.boot.ai.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EgovAiAuditLogAdvisorTest {

    private ChatClientRequest req(String text) {
        return ChatClientRequest.builder().prompt(new Prompt(text)).build();
    }

    private ChatClientResponse responseWith(String text) {
        ChatResponse cr = new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        return ChatClientResponse.builder().chatResponse(cr).build();
    }

    private EgovAiAuditProperties propsWithResponse(boolean includeResponse) {
        EgovAiAuditProperties p = new EgovAiAuditProperties();
        p.setIncludeResponse(includeResponse);
        return p;
    }

    @Test void publishesAuditEventAfterCall() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        EgovAiAuditLogAdvisor advisor =
            new EgovAiAuditLogAdvisor(publisher, propsWithResponse(true), 250);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(responseWith("응답 텍스트"));

        advisor.adviseCall(req("마스킹된 질의"), chain);

        ArgumentCaptor<EgovAiAuditEvent> captor = ArgumentCaptor.forClass(EgovAiAuditEvent.class);
        verify(publisher).publishEvent(captor.capture());
        EgovAiAuditEvent event = captor.getValue();
        assertThat(event.getQuery()).isEqualTo("마스킹된 질의");
        assertThat(event.getResponse()).isEqualTo("응답 텍스트");
        assertThat(event.getElapsedMs()).isGreaterThanOrEqualTo(0);
    }

    @Test void omitsResponseWhenIncludeResponseFalse() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        EgovAiAuditLogAdvisor advisor =
            new EgovAiAuditLogAdvisor(publisher, propsWithResponse(false), 250);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(responseWith("응답"));

        advisor.adviseCall(req("질의"), chain);

        ArgumentCaptor<EgovAiAuditEvent> captor = ArgumentCaptor.forClass(EgovAiAuditEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getResponse()).isNull();
    }

    @Test void publishesAuditEventAfterStream() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        EgovAiAuditLogAdvisor advisor =
            new EgovAiAuditLogAdvisor(publisher, propsWithResponse(true), 250);

        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        when(chain.nextStream(any())).thenReturn(Flux.just(responseWith("청크")));

        advisor.adviseStream(req("스트림 질의"), chain).blockLast();

        ArgumentCaptor<EgovAiAuditEvent> captor = ArgumentCaptor.forClass(EgovAiAuditEvent.class);
        verify(publisher).publishEvent(captor.capture());
        EgovAiAuditEvent event = captor.getValue();
        assertThat(event.getQuery()).isEqualTo("스트림 질의");
        // 스트리밍 모드에서는 응답 텍스트를 저장하지 않는다
        assertThat(event.getResponse()).isNull();
    }

    @Test void returnsCorrectOrder() {
        EgovAiAuditLogAdvisor advisor =
            new EgovAiAuditLogAdvisor(mock(ApplicationEventPublisher.class), propsWithResponse(true), 250);
        assertThat(advisor.getOrder()).isEqualTo(250);
    }
}
