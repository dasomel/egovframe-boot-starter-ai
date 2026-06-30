package org.egovframe.boot.ai.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * egovframe-boot-starter-ai advisor 체인 시연용 데모 앱.
 *
 * 기동 시 PII가 포함된 프롬프트를 EchoChatModel로 전송한다.
 * 콘솔 로그에서 다음을 확인할 수 있다.
 *   - [TRACE] AI 호출 시작/종료 + traceId
 *   - [PII]   주민번호·휴대폰이 마스킹된 뒤 모델에 전달되는 것
 *   - [USAGE] prompt/completion/total 토큰 로그
 */
@SpringBootApplication
public class DemoApplication {

    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(ChatClient.Builder builder) {
        return args -> {
            ChatClient client = builder.build();

            log.info("=== egovframe-boot-starter-ai advisor 체인 데모 시작 ===");
            log.info("프롬프트에 주민번호·휴대폰 포함 → PII advisor가 마스킹 후 모델에 전달");

            String response = client.prompt()
                    .user("제 주민번호는 900101-1234567이고 연락처는 010-9876-5432입니다. 회원가입을 도와주세요.")
                    .call()
                    .content();

            log.info("=== 모델 응답 ===");
            log.info(response);
            log.info("=== 데모 종료 ===");
        };
    }
}
