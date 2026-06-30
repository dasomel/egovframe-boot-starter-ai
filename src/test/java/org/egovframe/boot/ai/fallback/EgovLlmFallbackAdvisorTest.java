package org.egovframe.boot.ai.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EgovLlmFallbackAdvisorTest {

    private ChatClientRequest req() {
        return ChatClientRequest.builder().prompt(new Prompt("hi")).build();
    }

    @Test void returnsFallbackResponseWhenEnabled() {
        EgovLlmFallbackAdvisor advisor =
            new EgovLlmFallbackAdvisor(new EgovLlmExceptionMapper(), true, "대체 응답", 0);
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenThrow(new RuntimeException("429 too many requests"));

        String text = advisor.adviseCall(req(), chain)
            .chatResponse().getResult().getOutput().getText();
        assertThat(text).isEqualTo("대체 응답");
    }

    @Test void rethrowsAsEgovLlmExceptionWhenFallbackDisabled() {
        EgovLlmFallbackAdvisor advisor =
            new EgovLlmFallbackAdvisor(new EgovLlmExceptionMapper(), false, "대체 응답", 0);
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenThrow(new RuntimeException("timed out"));

        assertThatThrownBy(() -> advisor.adviseCall(req(), chain))
            .isInstanceOf(EgovLlmException.class)
            .extracting("kind").isEqualTo(EgovLlmException.Kind.TIMEOUT);
    }

    @Test void returnsFallbackResponseOnStreamErrorWhenEnabled() {
        EgovLlmFallbackAdvisor advisor =
            new EgovLlmFallbackAdvisor(new EgovLlmExceptionMapper(), true, "대체 응답", 0);
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        when(chain.nextStream(any())).thenReturn(Flux.error(new RuntimeException("timed out")));

        Flux<ChatClientResponse> flux = advisor.adviseStream(req(), chain);
        ChatClientResponse resp = flux.blockFirst();
        assertThat(resp).isNotNull();
        assertThat(resp.chatResponse().getResult().getOutput().getText()).isEqualTo("대체 응답");
    }

    @Test void rethrowsAsEgovLlmExceptionOnStreamErrorWhenFallbackDisabled() {
        EgovLlmFallbackAdvisor advisor =
            new EgovLlmFallbackAdvisor(new EgovLlmExceptionMapper(), false, "대체 응답", 0);
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        when(chain.nextStream(any())).thenReturn(Flux.error(new RuntimeException("timed out")));

        Flux<ChatClientResponse> flux = advisor.adviseStream(req(), chain);
        assertThatThrownBy(() -> flux.blockFirst())
            .isInstanceOf(EgovLlmException.class)
            .extracting("kind").isEqualTo(EgovLlmException.Kind.TIMEOUT);
    }
}
