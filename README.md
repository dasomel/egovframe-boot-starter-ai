# egovframe-boot-starter-ai

전자정부 표준프레임워크 기반 공공 서비스를 위한 Spring AI 횡단관심사 자동구성 스타터(커뮤니티 구현).

> 본 저장소는 개인/커뮤니티 구현이며 표준프레임워크 센터의 공식 배포가 아닙니다.
> groupId `org.egovframe.boot`는 센터 소유 네임스페이스로, 본 저장소는 소스 및 로컬(`mvn install`) 사용을 전제로 합니다.
> Maven Central 배포 예정이 없으므로 의존성 추가 전 반드시 로컬 설치가 선행되어야 합니다.

## 소개

Spring AI 1.0.x 위에 공공서비스 LLM 연동에 필요한 횡단관심사 세 가지를 자동 배선합니다.

**호환성**: eGovFrame 5.0 세대(Spring Boot 3.5.x · Spring Framework 6.2.x · Java 17 · Spring AI 1.0.x)를 기준으로 합니다. `org.egovframe.boot:egovframe-boot-starter-parent:5.0.0` 또는 `spring-boot-starter-parent:3.5.6` 기반 프로젝트에 적용할 수 있습니다.

- **PII 마스킹**: 프롬프트 전송 전 개인정보(주민등록번호·여권번호·카드번호·휴대폰번호 등)를 정규식 기반으로 길이 보존 마스킹
- **예외/폴백 표준화**: LLM 호출 실패를 `EgovLlmException`(TIMEOUT/RATE_LIMIT/SERVER/UNKNOWN)으로 분류하고, 설정에 따라 폴백 메시지 응답 또는 예외 재전파
- **토큰 사용량 로깅**: 응답마다 `LLM token usage - prompt: N, completion: M, total: K` 형식으로 INFO 로깅

소비자는 의존성만 추가하면 세 기능이 자동 적용됩니다. 별도 `@Bean` 선언 불필요.

## 설치

```bash
# 1. 소스 클론 후 로컬 Maven 저장소에 설치
git clone <this-repo>
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

## 기능

### PII 마스킹

`EgovPiiMaskingAdvisor`가 `CallAdvisor`/`StreamAdvisor`로 동작하여 user 메시지 전송 전에 개인정보를 마스킹합니다.

마스킹 결과 예시:
- `900101-1234567` → `900101-1******`
- `010-1234-5678` → `010-****-5678`
- `1234-5678-9012-3456` → `1234-****-****-3456`
- `M12345678` → `M123****`

### 예외/폴백 표준화

`EgovLlmFallbackAdvisor`가 LLM 호출 예외를 잡아 `EgovLlmException.Kind`로 분류합니다. `returnFallback=true`(기본값)이면 `fallbackMessage`를 응답으로 반환하고, `false`이면 분류된 예외를 재전파합니다.

### 토큰 사용량 로깅

`EgovTokenUsageAdvisor`가 응답 후 `org.slf4j.Logger`로 토큰 사용량을 기록합니다.

## 설정 키

| 키 | 기본값 | 설명 |
|----|--------|------|
| `egovframe.ai.pii-masking.enabled` | `true` | PII 마스킹 활성화 |
| `egovframe.ai.pii-masking.types` | (기본 유형) | 활성화할 PII 유형 목록 |
| `egovframe.ai.fallback.enabled` | `true` | 폴백 어드바이저 활성화 |
| `egovframe.ai.fallback.return-fallback` | `true` | true면 폴백 메시지 반환, false면 예외 재전파 |
| `egovframe.ai.fallback.fallback-message` | (한국어 안내 문구) | 폴백 응답 메시지 |
| `egovframe.ai.token-usage.enabled` | `true` | 토큰 사용량 로깅 활성화 |

## PII 유형표

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

## 사용 예시

```java
// 의존성만 추가하면 ChatClient에 자동 적용됩니다.
@Autowired
ChatClient.Builder builder;

ChatClient client = builder.build();
String response = client.prompt()
    .user("제 주민번호는 900101-1234567입니다. 개인 맞춤 서비스를 신청합니다.")
    .call()
    .content();
// → 프롬프트가 "제 주민번호는 900101-1****** 입니다. 개인 맞춤 서비스를 신청합니다."로 마스킹된 후 전송됩니다.
```

특정 기능만 비활성화하려면:

```yaml
egovframe:
  ai:
    pii-masking:
      enabled: false   # PII 마스킹 비활성화
    fallback:
      return-fallback: false   # 폴백 대신 예외 재전파
      fallback-message: "서비스 일시 중단 중입니다."
    token-usage:
      enabled: false   # 토큰 로깅 비활성화
```

## 한계 및 미검증 항목

- **실 LLM E2E 미검증**: 단위 테스트 및 ApplicationContextRunner 슬라이스 테스트만 수행. 실제 LLM 엔드포인트 연동은 검증되지 않았습니다.
- **정규식 오탐 가능**: 숫자 패턴 기반 탐지이므로 유사한 형태의 일반 숫자 데이터가 마스킹될 수 있습니다. 체크디지트 검증 미포함.
- **ACCOUNT 유형 과탐**: 계좌번호 패턴이 느슨하여 일반 숫자 조합과 충돌할 수 있어 기본 활성이나 주의가 필요합니다.
- **Spring AI 버전 고정**: `spring-ai-bom 1.0.1` 기준. 상위 버전은 별도 검증 필요.

## 라이선스

Apache License 2.0. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 고지

본 프로젝트는 개인/커뮤니티 기여로 작성된 것으로, 한국지능정보사회진흥원(NIA) 또는 전자정부 표준프레임워크 센터의 공식 제품이 아닙니다.
