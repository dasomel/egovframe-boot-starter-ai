package org.egovframe.boot.ai.trace;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EgovAiTraceAdvisorTest {

    private final EgovAiTraceLogFormatter formatter = new EgovAiTraceLogFormatter();
    private final EgovAiTraceProperties props = new EgovAiTraceProperties();

    private ChatClientRequest req() {
        return ChatClientRequest.builder().prompt(new Prompt("hi")).build();
    }

    @Test void adviseCallPutsAndClearsMdc() {
        EgovAiTraceAdvisor advisor = new EgovAiTraceAdvisor(formatter, props, 0);
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenAnswer(invocation -> {
            // During execution, MDC should contain the traceId
            assertThat(MDC.get(props.getMdcKey())).isNotNull();
            return ChatClientResponse.builder().chatResponse(null).build();
        });

        // Initially MDC should be empty
        MDC.remove(props.getMdcKey());
        assertThat(MDC.get(props.getMdcKey())).isNull();

        advisor.adviseCall(req(), chain);

        // Afterward MDC should be empty
        assertThat(MDC.get(props.getMdcKey())).isNull();
    }

    @Test void adviseStreamClearsMdcImmediatelyOnInitiatingThreadAndSetsDuringFinally() {
        EgovAiTraceAdvisor advisor = new EgovAiTraceAdvisor(formatter, props, 0);
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenAnswer(invocation -> {
            // Inside nextStream (still initiating thread), MDC should still be active
            assertThat(MDC.get(props.getMdcKey())).isNotNull();
            return Flux.just(ChatClientResponse.builder().chatResponse(null).build());
        });

        // Initially MDC should be empty
        MDC.remove(props.getMdcKey());
        assertThat(MDC.get(props.getMdcKey())).isNull();

        Flux<ChatClientResponse> flux = advisor.adviseStream(req(), chain);

        // After calling adviseStream, MDC MUST be cleared immediately on initiating thread!
        assertThat(MDC.get(props.getMdcKey())).isNull();

        // Let's run the stream
        flux.blockLast();

        // Afterward MDC should be empty
        assertThat(MDC.get(props.getMdcKey())).isNull();
    }
}
