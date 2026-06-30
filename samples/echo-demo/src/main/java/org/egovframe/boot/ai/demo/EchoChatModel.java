package org.egovframe.boot.ai.demo;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 입력된 user 메시지를 그대로 되돌려 주는 에코 ChatModel.
 * 실 LLM 없이 egovframe-boot-starter-ai advisor 체인(PII 마스킹·trace·usage)을 시연하기 위한 스텁.
 */
@Component
public class EchoChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        String userText = prompt.getUserMessage().getText();
        String reply = "echo: " + userText;

        DefaultUsage usage = new DefaultUsage(
                userText.length(),   // prompt 토큰을 문자 수로 근사
                reply.length(),      // completion 토큰을 문자 수로 근사
                userText.length() + reply.length()
        );
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .usage(usage)
                .model("echo-model-v1")
                .build();

        return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))), metadata);
    }
}
