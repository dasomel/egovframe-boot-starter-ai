package org.egovframe.boot.ai.pii;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EgovPiiMaskingAdvisorTest {

    @Test void masksUserMessageBeforeNextCall() {
        EgovPiiMaskingAdvisor advisor = new EgovPiiMaskingAdvisor(
            new EgovPiiMasker(EgovPiiType.defaultOn()), 0);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt("내 번호는 010-1234-5678 이야")).build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(
            ChatClientResponse.builder().chatResponse(null).build());

        advisor.adviseCall(request, chain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
        verify(chain).nextCall(captor.capture());
        String sentText = captor.getValue().prompt().getUserMessage().getText();
        assertThat(sentText).isEqualTo("내 번호는 010-****-5678 이야");
    }
}
