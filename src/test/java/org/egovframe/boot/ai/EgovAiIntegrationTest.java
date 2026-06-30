package org.egovframe.boot.ai;

import org.egovframe.boot.ai.autoconfigure.EgovAiChatClientCustomizer;
import org.egovframe.boot.ai.fallback.EgovLlmException;
import org.egovframe.boot.ai.fallback.EgovLlmExceptionMapper;
import org.egovframe.boot.ai.fallback.EgovLlmFallbackAdvisor;
import org.egovframe.boot.ai.pii.EgovPiiMasker;
import org.egovframe.boot.ai.pii.EgovPiiMaskingAdvisor;
import org.egovframe.boot.ai.pii.EgovPiiType;
import org.egovframe.boot.ai.trace.EgovAiTraceAdvisor;
import org.egovframe.boot.ai.trace.EgovAiTraceLogFormatter;
import org.egovframe.boot.ai.trace.EgovAiTraceProperties;
import org.egovframe.boot.ai.usage.EgovTokenUsageAdvisor;
import org.egovframe.boot.ai.usage.EgovTokenUsageFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EgovAiChatClientCustomizer로 advisor 체인을 구성한 ChatClient의 E2E 통합 테스트.
 * 실 LLM 없이 스텁 ChatModel로 advisor 동작을 검증한다.
 */
class EgovAiIntegrationTest {

    private static final int ORDER_TRACE    = 50;
    private static final int ORDER_PII      = 100;
    private static final int ORDER_FALLBACK = 200;
    private static final int ORDER_USAGE    = 300;

    private ChatModel stubModel;

    @BeforeEach
    void setUp() {
        stubModel = mock(ChatModel.class);
    }

    private ChatClient buildChatClient(ChatModel model, List<Advisor> advisors) {
        ChatClient.Builder builder = ChatClient.builder(model);
        new EgovAiChatClientCustomizer(advisors).customize(builder);
        return builder.build();
    }

    private ChatResponse stubResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    // -----------------------------------------------------------------------
    // PII 마스킹 시나리오
    // -----------------------------------------------------------------------

    @Test
    void piiAdvisor_masksRrnAndMobileBeforeReachingModel() {
        EgovPiiMaskingAdvisor piiAdvisor =
                new EgovPiiMaskingAdvisor(new EgovPiiMasker(EgovPiiType.defaultOn()), ORDER_PII);

        when(stubModel.call(any(Prompt.class))).thenReturn(stubResponse("ok"));

        ChatClient client = buildChatClient(stubModel, List.of(piiAdvisor));
        client.prompt()
              .user("주민번호 900101-1234567 전화 010-1234-5678")
              .call()
              .content();

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(stubModel).call(captor.capture());
        String sentText = captor.getValue().getUserMessage().getText();

        assertThat(sentText).contains("900101-1******");
        assertThat(sentText).contains("010-****-5678");
        assertThat(sentText).doesNotContain("1234567");
    }

    // -----------------------------------------------------------------------
    // 폴백 시나리오
    // -----------------------------------------------------------------------

    @Test
    void fallbackAdvisor_returnsFallbackMessageOnException() {
        EgovLlmFallbackAdvisor fallbackAdvisor = new EgovLlmFallbackAdvisor(
                new EgovLlmExceptionMapper(), true, "서비스 일시 중단 중입니다.", ORDER_FALLBACK);

        when(stubModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("503 Service Unavailable"));

        ChatClient client = buildChatClient(stubModel, List.of(fallbackAdvisor));
        String result = client.prompt().user("테스트").call().content();

        assertThat(result).isEqualTo("서비스 일시 중단 중입니다.");
    }

    @Test
    void fallbackAdvisor_rethrowsEgovLlmExceptionWhenFallbackDisabled() {
        EgovLlmFallbackAdvisor fallbackAdvisor = new EgovLlmFallbackAdvisor(
                new EgovLlmExceptionMapper(), false, "사용 안 함", ORDER_FALLBACK);

        when(stubModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("429 too many requests"));

        ChatClient client = buildChatClient(stubModel, List.of(fallbackAdvisor));

        assertThatThrownBy(() -> client.prompt().user("테스트").call().content())
                .isInstanceOf(EgovLlmException.class)
                .extracting(e -> ((EgovLlmException) e).getKind())
                .isEqualTo(EgovLlmException.Kind.RATE_LIMIT);
    }

    // -----------------------------------------------------------------------
    // 전체 체인 통과 시나리오
    // -----------------------------------------------------------------------

    @Test
    void fullChain_allAdvisorsPassWithoutException() {
        EgovAiTraceProperties traceProps = new EgovAiTraceProperties();
        EgovAiTraceAdvisor traceAdvisor = new EgovAiTraceAdvisor(
                new EgovAiTraceLogFormatter(), traceProps, ORDER_TRACE);

        EgovPiiMaskingAdvisor piiAdvisor = new EgovPiiMaskingAdvisor(
                new EgovPiiMasker(EgovPiiType.defaultOn()), ORDER_PII);

        EgovLlmFallbackAdvisor fallbackAdvisor = new EgovLlmFallbackAdvisor(
                new EgovLlmExceptionMapper(), true, "대체 응답", ORDER_FALLBACK);

        Usage stubUsage = new Usage() {
            @Override public Integer getPromptTokens() { return 10; }
            @Override public Integer getCompletionTokens() { return 20; }
            @Override public Object getNativeUsage() { return null; }
        };
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(stubUsage).build();
        ChatResponse responseWithUsage = new ChatResponse(
                List.of(new Generation(new AssistantMessage("응답"))), metadata);

        EgovTokenUsageAdvisor usageAdvisor = new EgovTokenUsageAdvisor(
                new EgovTokenUsageFormatter(), ORDER_USAGE);

        when(stubModel.call(any(Prompt.class))).thenReturn(responseWithUsage);

        List<Advisor> advisors = List.of(traceAdvisor, piiAdvisor, fallbackAdvisor, usageAdvisor);
        ChatClient client = buildChatClient(stubModel, advisors);

        String result = client.prompt()
                              .user("내 번호 010-9999-0000 으로 연락 주세요")
                              .call()
                              .content();

        assertThat(result).isEqualTo("응답");
        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(stubModel).call(captor.capture());
        assertThat(captor.getValue().getUserMessage().getText()).contains("010-****-0000");
    }
}
