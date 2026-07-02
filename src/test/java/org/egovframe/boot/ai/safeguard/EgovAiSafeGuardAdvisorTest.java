package org.egovframe.boot.ai.safeguard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EgovAiSafeGuardAdvisorTest {

    private EgovAiSafeGuardAdvisor advisor;
    private CallAdvisorChain chain;

    @BeforeEach
    void setUp() {
        advisor = new EgovAiSafeGuardAdvisor(
                List.of("대외비", "비밀번호"),
                "금칙어가 포함되어 차단되었습니다.",
                75
        );
        chain = mock(CallAdvisorChain.class);
    }

    @Test
    void blocksPromptContainingForbiddenKeyword() {
        Prompt prompt = new Prompt(new UserMessage("이 문서는 대외비 정보입니다."));
        ChatClientRequest request = ChatClientRequest.builder().prompt(prompt).build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isNotNull();
        assertThat(response.chatResponse().getResult().getOutput().getText())
                .isEqualTo("금칙어가 포함되어 차단되었습니다.");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void passesPromptWithoutForbiddenKeyword() {
        Prompt prompt = new Prompt(new UserMessage("일반 질의응답 문장입니다."));
        ChatClientRequest request = ChatClientRequest.builder().prompt(prompt).build();
        ChatClientResponse mockResponse = mock(ChatClientResponse.class);

        when(chain.nextCall(request)).thenReturn(mockResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isEqualTo(mockResponse);
        verify(chain, times(1)).nextCall(request);
    }
}
