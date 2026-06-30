# egovframe-boot-starter-ai

![CI](https://github.com/dasomel/egovframe-boot-starter-ai/actions/workflows/ci.yml/badge.svg)
![Java 17](https://img.shields.io/badge/Java-17-blue)
![Spring AI 1.0.x](https://img.shields.io/badge/Spring%20AI-1.0.x-green)
![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey)

전자정부 표준프레임워크 기반 공공 서비스를 위한 Spring AI 횡단관심사 자동구성 스타터(커뮤니티 구현).

> 본 저장소는 개인/커뮤니티 구현이며 표준프레임워크 센터의 공식 배포가 아닙니다.
> groupId `org.egovframe.boot`는 센터 소유 네임스페이스로, 본 저장소는 소스 및 로컬(`mvn install`) 사용을 전제로 합니다.
> Maven Central 배포 예정이 없으므로 의존성 추가 전 반드시 로컬 설치가 선행되어야 합니다.

**호환성**: eGovFrame 5.0 세대(Spring Boot 3.5.x · Spring Framework 6.2.x · Java 17 · Spring AI 1.0.x).

---

## 소개

Spring AI 1.0.x 위에서 공공서비스 LLM 연동에 필요한 횡단관심사 7종을 자동 배선합니다. 소비자는 의존성 하나만 추가하면 별도 `@Bean` 선언 없이 모든 기능이 `ChatClient`에 적용됩니다.

---

## 아키텍처

### advisor 체인 실행 순서

```
ChatClient.call()
    │
    ▼  order=50
┌─────────────────────┐
│  EgovAiTraceAdvisor │  AI 호출 시작/종료 + MDC traceId 로깅 (가장 바깥)
└──────────┬──────────┘
           │
           ▼  order=75
┌────────────────────────────┐
│  EgovAiSafeGuardAdvisor    │  금칙어·인젝션 탐지 → 위반 시 즉시 차단(LLM 미호출)
└──────────┬─────────────────┘
           │
           ▼  order=100
┌──────────────────────────┐
│  EgovPiiMaskingAdvisor   │  user 메시지 PII 마스킹 후 다음으로 전달
└──────────┬───────────────┘
           │
           ▼  order=200
┌──────────────────────────┐
│  EgovLlmFallbackAdvisor  │  예외 → EgovLlmException 분류 / 폴백 응답 반환
└──────────┬───────────────┘
           │
           ▼  order=250
┌──────────────────────────┐
│  EgovAiAuditLogAdvisor   │  마스킹된 query·응답·소요시간 → 감사 이벤트 발행
└──────────┬───────────────┘
           │
           ▼  order=300
┌──────────────────────────┐
│  EgovTokenUsageAdvisor   │  응답 후 토큰 사용량 로깅 (가장 안쪽)
└──────────┬───────────────┘
           │
           ▼
       ChatModel
```

낮은 order 값이 먼저(바깥쪽) 실행됩니다.

### 프롬프트 템플릿 매니저

`EgovPromptTemplateManager`는 advisor 체인과 독립적으로 동작합니다. 클래스패스의 외부 `.st` 파일을 로드하여 변수 치환 후 문자열을 반환하고, 로드 결과를 메모리에 캐시합니다.

---

## 기능

### PII 마스킹

`EgovPiiMaskingAdvisor`가 user 메시지 전송 전 개인정보를 정규식 기반으로 마스킹합니다.

- `900101-1234567` → `900101-1******`
- `010-1234-5678` → `010-****-5678`
- `1234-5678-9012-3456` → `1234-****-****-3456`
- `M12345678` → `M123****`

**설정**(`egovframe.ai.pii-masking.*`): `enabled`, `types`

### 예외/폴백 표준화

`EgovLlmFallbackAdvisor`가 LLM 호출 예외를 `EgovLlmException.Kind`(TIMEOUT / RATE_LIMIT / SERVER / UNKNOWN)로 분류합니다. `return-fallback=true`(기본)이면 폴백 메시지를 응답으로 반환하고, `false`이면 분류된 예외를 재전파합니다.

**설정**(`egovframe.ai.fallback.*`): `enabled`, `return-fallback`, `fallback-message`

### 토큰 사용량 로깅

`EgovTokenUsageAdvisor`가 응답마다 `LLM token usage - prompt: N, completion: M, total: K` 형식으로 INFO 로깅합니다.

**설정**(`egovframe.ai.token-usage.*`): `enabled`

### AI 호출 추적 로깅

`EgovAiTraceAdvisor`가 AI 호출의 시작/종료를 MDC 추적 ID와 함께 로깅합니다. 가장 바깥에서 감싸므로 전체 체인의 소요 시간을 측정합니다.

**설정**(`egovframe.ai.trace.*`): `enabled`, `include-prompt`, `mdc-key`

### 프롬프트 템플릿 외부화

`EgovPromptTemplateManager`가 클래스패스의 `.st` 파일을 이름으로 로드하고 변수를 치환합니다.

**설정**(`egovframe.ai.prompt-template.*`): `enabled`, `location`, `suffix`

### 보안 가드레일(SafeGuard)

`EgovAiSafeGuardAdvisor`가 user 메시지를 사전 검사하여 위반이 감지되면 LLM을 호출하지 않고 즉시 차단 응답을 반환합니다.

- **금칙어 차단**: 설정한 단어 목록에 해당하는 텍스트를 대소문자 구분 없이 차단합니다.
- **프롬프트 인젝션 탐지**: `ignore previous instructions`, `system prompt`, `developer mode`, `역할을 무시`, `이전 지시를 무시`, `프롬프트를 출력` 등의 패턴을 탐지합니다.
- PII 마스킹보다 앞(order=75)에서 동작하여 불필요한 마스킹 비용 없이 빠르게 차단합니다.

**설정**(`egovframe.ai.safeguard.*`): `enabled`, `blocked-words`, `detect-injection`, `block-message`

### 감사 로그(AuditLog)

`EgovAiAuditLogAdvisor`가 AI 호출마다 `EgovAiAuditEvent`를 Spring 애플리케이션 이벤트로 발행합니다. 소비 측에서 `@EventListener`로 수신하여 기관 DB나 감사 시스템에 저장할 수 있습니다.

- **개인정보 보호**: 이 advisor는 PII 마스킹 이후(order=250, inner)에 위치하므로 `query` 필드에는 이미 마스킹된 텍스트만 담깁니다. 원본 미마스킹 PII는 저장하지 않습니다.
- `traceId`(MDC), `query`(마스킹된 입력), `response`(`includeResponse=false`이면 null), `model`, `elapsedMs`를 기록합니다.
- 스트리밍 모드에서는 응답 청크 집계 복잡성으로 인해 응답 텍스트를 포함하지 않습니다(`response=null`).

**설정**(`egovframe.ai.audit.*`): `enabled`, `include-response`

```java
// 감사 이벤트 소비 예시
@Component
public class AuditEventHandler {
    @EventListener
    public void onAuditEvent(EgovAiAuditEvent event) {
        // 기관 감사 DB 저장 등 처리
    }
}
```

---

## 설치

```bash
# 1. 소스 클론 후 로컬 Maven 저장소에 설치
git clone https://github.com/dasomel/egovframe-boot-starter-ai.git
cd egovframe-boot-starter-ai
mvn install
```

```xml
<!-- 2. 의존성 추가 -->
<dependency>
    <groupId>org.egovframe.boot</groupId>
    <artifactId>egovframe-boot-starter-ai</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## 설정 키 전체 표

| 키 | 기본값 | 설명 |
|----|--------|------|
| `egovframe.ai.pii-masking.enabled` | `true` | PII 마스킹 활성화 |
| `egovframe.ai.pii-masking.types` | (기본 유형 7종) | 활성화할 PII 유형 목록 |
| `egovframe.ai.fallback.enabled` | `true` | 폴백 어드바이저 활성화 |
| `egovframe.ai.fallback.return-fallback` | `true` | true면 폴백 메시지 반환, false면 예외 재전파 |
| `egovframe.ai.fallback.fallback-message` | `"일시적으로 요청을 처리할 수 없습니다..."` | 폴백 응답 메시지 |
| `egovframe.ai.token-usage.enabled` | `true` | 토큰 사용량 로깅 활성화 |
| `egovframe.ai.trace.enabled` | `true` | 추적 로깅 활성화 |
| `egovframe.ai.trace.include-prompt` | `false` | true면 프롬프트 문자 수를 로그에 포함 |
| `egovframe.ai.trace.mdc-key` | `"egovAiTraceId"` | MDC에 등록할 추적 ID 키 이름 |
| `egovframe.ai.prompt-template.enabled` | `true` | 프롬프트 템플릿 매니저 활성화 |
| `egovframe.ai.prompt-template.location` | `"classpath:/egovframe/ai/prompts/"` | 템플릿 파일 기본 경로 |
| `egovframe.ai.prompt-template.suffix` | `".st"` | 템플릿 파일 확장자 |
| `egovframe.ai.safeguard.enabled` | `true` | 보안 가드레일 활성화 |
| `egovframe.ai.safeguard.blocked-words` | `[]` | 차단할 금칙어 목록 (대소문자 무시) |
| `egovframe.ai.safeguard.detect-injection` | `true` | 프롬프트 인젝션 탐지 활성화 |
| `egovframe.ai.safeguard.block-message` | `"요청에 허용되지 않는 내용이..."` | 차단 시 반환 메시지 |
| `egovframe.ai.audit.enabled` | `true` | 감사 로그 활성화 |
| `egovframe.ai.audit.include-response` | `true` | 감사 이벤트에 응답 텍스트 포함 여부 |

---

## PII 유형 표

개인정보보호법 시행령 제19조(고유식별정보) 및 ISMS-P 3.2.3 기준.

| 유형 | 기본 활성 | 설명 |
|------|-----------|------|
| `RRN` | 활성 | 주민등록번호 (뒷 7자리 마스킹) |
| `ARN` | 활성 | 외국인등록번호 |
| `PASSPORT` | 활성 | 여권번호 |
| `DRIVER_LICENSE` | 활성 | 운전면허번호 |
| `CARD` | 활성 | 신용/체크카드 번호 |
| `ACCOUNT` | 활성 | 계좌번호 |
| `MOBILE` | 활성 | 휴대폰번호 |
| `EMAIL` | 비활성 | 이메일 주소 (과탐 위험) |
| `PHONE` | 비활성 | 일반 전화번호 (과탐 위험) |
| `BIRTHDATE` | 비활성 | 생년월일 (과탐 위험) |
| `IP` | 비활성 | IP 주소 |

---

## 사용 예시

### 의존성 추가만으로 자동 적용

```java
@Autowired
ChatClient.Builder builder;

ChatClient client = builder.build();
String response = client.prompt()
    .user("제 주민번호는 900101-1234567입니다. 개인 맞춤 서비스를 신청합니다.")
    .call()
    .content();
// 프롬프트가 "제 주민번호는 900101-1****** 입니다..."로 마스킹된 후 전송됩니다.
```

### 일부 기능 비활성화

```yaml
egovframe:
  ai:
    pii-masking:
      enabled: false
    fallback:
      return-fallback: false
      fallback-message: "서비스 일시 중단 중입니다."
    token-usage:
      enabled: false
    trace:
      include-prompt: true   # 프롬프트 문자 수를 로그에 포함
```

### 프롬프트 템플릿 사용

```
# src/main/resources/egovframe/ai/prompts/greeting.st
안녕하세요, {name}님. 오늘 {topic}에 대해 도움을 드리겠습니다.
```

```java
@Autowired
EgovPromptTemplateManager templateManager;

String prompt = templateManager.render("greeting",
    Map.of("name", "홍길동", "topic", "세금 신고"));
```

### 실행 가능한 데모 (samples/echo-demo)

실 LLM 없이 advisor 체인 전체(PII 마스킹·trace·usage)를 로컬에서 확인할 수 있습니다.

```bash
# 루트에서 스타터 로컬 설치 후 데모 기동
mvn -q -DskipTests install
cd samples/echo-demo
mvn spring-boot:run
```

자세한 실행 방법과 기대 출력은 [samples/echo-demo/README.md](samples/echo-demo/README.md)를 참고하세요.

---

## 한계 및 미검증 항목

- **실 LLM E2E 미검증**: 단위 테스트 및 ApplicationContextRunner 슬라이스 테스트, 스텁 기반 통합 테스트만 수행. 실제 LLM 엔드포인트 연동은 검증되지 않았습니다.
- **정규식 오탐 가능**: 숫자 패턴 기반 탐지이므로 유사 형태의 일반 숫자 데이터가 마스킹될 수 있습니다. 체크디지트 검증 미포함.
- **ACCOUNT 유형 과탐**: 계좌번호 패턴이 느슨하여 일반 숫자 조합과 충돌할 수 있어 기본 활성이나 주의가 필요합니다.
- **Spring AI 버전 고정**: `spring-ai-bom 1.0.1` 기준. 상위 버전은 별도 검증 필요.

---

## 라이선스

Apache License 2.0. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 고지

본 프로젝트는 개인/커뮤니티 기여로 작성된 것으로, 한국지능정보사회진흥원(NIA) 또는 전자정부 표준프레임워크 센터의 공식 제품이 아닙니다.
