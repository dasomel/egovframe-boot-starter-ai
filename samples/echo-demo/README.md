# echo-demo

egovframe-boot-starter-ai의 advisor 체인(PII 마스킹 → Trace → Fallback → Token Usage)을 로컬에서 직접 확인하기 위한 최소 데모 앱이다.
실제 LLM 없이 `EchoChatModel`(입력을 그대로 되돌리는 스텁)을 사용하므로 API 키 불필요.

## 실행 방법

```bash
# 1. 루트에서 스타터를 로컬 Maven 저장소에 설치
cd /path/to/egovframe-boot-starter-ai
mvn -q -DskipTests install

# 2. 데모 기동 (CommandLineRunner 완료 후 자동 종료)
cd samples/echo-demo
mvn -q spring-boot:run
```

## 기대 출력

```
INFO  DemoApplication - === egovframe-boot-starter-ai advisor 체인 데모 시작 ===
INFO  DemoApplication - 프롬프트에 주민번호·휴대폰 포함 → PII advisor가 마스킹 후 모델에 전달
INFO  EgovAiTraceAdvisor  - AI 호출 시작 - traceId=<8자리 id>
INFO  EgovTokenUsageAdvisor - LLM token usage - prompt: N, completion: M, total: K
INFO  EgovAiTraceAdvisor  - AI 호출 종료 - traceId=<8자리 id>, elapsedMs=X, status=OK
INFO  DemoApplication - === 모델 응답 ===
INFO  DemoApplication - echo: 제 주민번호는 900101-1****** 이고 연락처는 010-****-5432입니다. ...
INFO  DemoApplication - === 데모 종료 ===
```

- 모델에 전달된 user 메시지에서 주민번호(`900101-1234567`)와 휴대폰(`010-9876-5432`)이 마스킹된 것을 응답(에코)에서 확인할 수 있다.
- token-usage 로그에서 prompt/completion 토큰 수(여기서는 문자 수 근사값)가 출력된다.
- trace 로그에서 각 AI 호출의 시작·종료와 traceId를 확인할 수 있다.

## 구성 파일

`src/main/resources/application.yml`에서 advisor별 활성 여부를 조정할 수 있다.
