package org.egovframe.boot.ai.usage;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EgovTokenUsageAdvisorTest {

    private ChatClientRequest req() {
        return ChatClientRequest.builder().prompt(new Prompt("hi")).build();
    }

    private ChatClientResponse resp(Usage usage) {
        ChatResponseMetadata meta = ChatResponseMetadata.builder().usage(usage).build();
        ChatResponse cr = new ChatResponse(List.of(new Generation(new AssistantMessage("reply"))), meta);
        return ChatClientResponse.builder().chatResponse(cr).build();
    }

    @Test void logsUsageOnCall() {
        EgovTokenUsageFormatter formatter = mock(EgovTokenUsageFormatter.class);
        EgovTokenUsageAdvisor advisor = new EgovTokenUsageAdvisor(formatter, 0);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        Usage usage = new Usage() {
            @Override public Integer getPromptTokens() { return 5; }
            @Override public Integer getCompletionTokens() { return 10; }
            @Override public Object getNativeUsage() { return null; }
        };
        when(chain.nextCall(any())).thenReturn(resp(usage));

        advisor.adviseCall(req(), chain);
        verify(formatter).format(5, 10, 15);
    }

    @Test void logsUsageOnStream() {
        EgovTokenUsageFormatter formatter = mock(EgovTokenUsageFormatter.class);
        EgovTokenUsageAdvisor advisor = new EgovTokenUsageAdvisor(formatter, 0);

        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        Usage usage = new Usage() {
            @Override public Integer getPromptTokens() { return 7; }
            @Override public Integer getCompletionTokens() { return 14; }
            @Override public Object getNativeUsage() { return null; }
        };
        when(chain.nextStream(any())).thenReturn(Flux.just(resp(usage)));

        Flux<ChatClientResponse> flux = advisor.adviseStream(req(), chain);
        flux.blockFirst(); // Trigger stream execution

        verify(formatter).format(7, 14, 21);
    }
}
