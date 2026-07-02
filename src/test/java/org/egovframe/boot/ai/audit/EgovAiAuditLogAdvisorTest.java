package org.egovframe.boot.ai.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EgovAiAuditLogAdvisorTest {

    private ApplicationEventPublisher eventPublisher;
    private EgovAiAuditLogAdvisor advisor;
    private CallAdvisorChain chain;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        advisor = new EgovAiAuditLogAdvisor(eventPublisher, true, 250);
        chain = mock(CallAdvisorChain.class);
    }

    @Test
    void publishesAuditEventOnSuccessfulCall() {
        Prompt prompt = new Prompt(new UserMessage("테스트 질문입니다."));
        ChatClientRequest request = ChatClientRequest.builder().prompt(prompt).build();

        AssistantMessage assistantMessage = new AssistantMessage("테스트 답변입니다.");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        ChatClientResponse response = new ChatClientResponse(chatResponse, Map.of());

        when(chain.nextCall(request)).thenReturn(response);

        ChatClientResponse actualResponse = advisor.adviseCall(request, chain);

        assertThat(actualResponse).isEqualTo(response);

        ArgumentCaptor<EgovAiAuditEvent> captor = ArgumentCaptor.forClass(EgovAiAuditEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        EgovAiAuditEvent event = captor.getValue();
        assertThat(event.getPromptText()).isEqualTo("테스트 질문입니다.");
        assertThat(event.getResponseText()).isEqualTo("테스트 답변입니다.");
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
    }
}
