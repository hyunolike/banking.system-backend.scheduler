# 🏦 banking.system-backend.scheduler

> API 서버의 현황 스냅샷을 주기적으로 수집하는 배치 서버

<br>

## 📌 프로젝트 개요

두 개의 서버로 구성된 뱅킹 시스템의 **스케줄러 서버**입니다.
정해진 주기마다 API 서버의 내부 엔드포인트를 호출해 시스템 현황을 수집하고 기록합니다.

```
┌────────────────────────┐         ┌────────────────────────┐
│   scheduler server     │         │      api server        │
│        :8181           │────────▶│        :8182           │──────▶ Oracle
│  현황 스냅샷 주기 수집   │ 내부 API │  인증 · 계좌 · 거래     │
└────────────────────────┘   키    └────────────────────────┘
```

| | |
|---|---|
| **API 서버** | [banking.system-backend.api](https://github.com/hyunolike/banking.system-backend.api) |
| **포트** | `8181` |
| **수집 주기** | 기본 1분 (`SNAPSHOT_CRON` 으로 변경) |

<br>

## 🛠 기술 스택

| 구분 | 기술 |
|---|---|
| **Language** | Kotlin 1.7.22 / Java 17 |
| **Framework** | Spring Boot 3.0.4 |
| **Scheduling** | Spring Scheduling (`@Scheduled`) |
| **AOP** | Spring AOP (AspectJ) |
| **HTTP** | RestTemplate |
| **Build** | Gradle 7.6.1 (Kotlin DSL) |
| **Test** | JUnit 5, AssertJ, MockK, MockRestServiceServer |
| **CI/CD** | GitHub Actions → NCP |

<br>

## 🚀 기능 목록

### 스냅샷 수집

- [x] 설정된 주기(cron)마다 API 서버의 현황 스냅샷 조회
- [x] 가입자 수 · 계좌 수 · 총 잔액 수집 및 로깅
- [x] `SCHEDULER_ENABLED=false` 로 스케줄 비활성화 (점검 · 테스트용)
- [x] 수집 주기를 설정으로 분리 — 코드 변경 없이 조정

### API 통신

- [x] `X-Internal-Api-Key` 헤더로 내부 API 인증
- [x] API 서버 주소를 설정으로 분리 — 다른 호스트 배포 가능
- [x] 커넥션 · 응답 타임아웃 지정
- [x] `RestTemplate` 을 빈으로 한 번만 생성
- [x] 호출 실패 시 로깅 후 다음 주기에 재시도 — 스케줄이 멈추지 않음

### 실행 시간 계측

- [x] `@RecordAutoGetAPI` 가 붙은 메서드의 소요 시간 측정
- [x] 성공 · 예외 양쪽 모두 계측
- [x] 예외는 삼키지 않고 그대로 전파
- [x] SLF4J 로 로깅

<br>

## 🎯 설계 원칙

- **어드바이스는 대상 메서드의 동작을 바꾸지 않는다.** 계측만 하고 결과와 예외를 그대로 통과시킨다.
- **환경에 따라 달라지는 값은 코드에 두지 않는다.** 주소 · 키 · 주기는 전부 설정으로 뺀다.
- **외부 호출에는 반드시 타임아웃을 건다.** 상대가 응답하지 않을 때 이쪽이 묶이지 않도록.
- **한 번의 실패가 스케줄 전체를 멈추게 하지 않는다.**

<br>

## 🏗 아키텍처

```
com.banking_system.scheduler_server
├── task      SnapshotCollectTask      @Scheduled 진입점
├── client    BankingApiClient         API 서버 호출
│             SystemSnapshot           응답 모델
├── config    BankingApiProperties     주소 · 키 · 타임아웃
│             RestTemplateConfig       타임아웃 걸린 RestTemplate 빈
└── aspects   RecordAspect             실행 시간 계측
              RecordAutoGetAPI         계측 대상 표시 애노테이션
```

### 동작 흐름

```
@Scheduled(cron)
      │
      ▼
SnapshotCollectTask.collectSnapshot()
      │
      ├─▶ RecordAspect  ─── 소요 시간 측정 (proceed 전후)
      │
      └─▶ BankingApiClient
              └─▶ GET {base-url}/api/internal/stats/snapshot
                   Header: X-Internal-Api-Key
```

### 로그 예시

```
snapshot collected :: users=128 accounts=241 totalBalance=88123400.0000 generatedAt=2024-05-01T10:15:30
auto get api :: SnapshotCollectTask.collectSnapshot() :: returned :: 37 ms
```

<br>

## 💡 기술적 고민과 해결 과정

### 1. 스케줄 작업이 한 번도 실행되지 않고 있었다

**문제** — 10초마다 로그는 정상적으로 찍히는데, API 호출은 나가지 않았습니다.
원인은 `@Around` 어드바이스에 **`proceed()` 호출이 빠져 있던 것**이었습니다.

```kotlin
@Around("@annotation(RecordAutoGetAPI)")
fun getAPI(joinPoint: ProceedingJoinPoint) {
    val start = System.currentTimeMillis()
    val signature = joinPoint.signature.toShortString()
    val duration = System.currentTimeMillis() - start   // ← start 직후, 항상 0ms
    logger.info("auto get api :: $signature :: duration :: $duration ms")
}
```

`@Around` 는 대상 메서드를 감싸는 어드바이스라 **`proceed()` 를 호출해야 원본이 실행**됩니다.
호출이 없으니 `collectSnapshot()` 의 본문이 통째로 건너뛰어졌습니다.
게다가 `start` 바로 다음 줄에서 경과 시간을 계산해 측정값은 **언제나 0ms** 였습니다.
로그가 정상으로 보였기 때문에 오히려 발견이 늦어질 수 있는 형태였습니다.

**해결** — `proceed()` 를 호출하고 반환값을 그대로 전달합니다.
반환 타입도 `Unit` 에서 `Any?` 로 바꿔 원본의 리턴값이 유실되지 않게 했습니다.
예외가 나도 소요 시간을 남기되, 예외 자체는 삼키지 않고 다시 던집니다.

```kotlin
@Around("@annotation(com.banking_system.scheduler_server.aspects.RecordAutoGetAPI)")
fun record(joinPoint: ProceedingJoinPoint): Any? {
    val start = System.nanoTime()
    return try {
        joinPoint.proceed().also { log.info("... returned :: {} ms", elapsedMillis(start)) }
    } catch (e: Throwable) {
        log.warn("... threw :: {} ms :: {}", elapsedMillis(start), e.toString())
        throw e
    }
}
```

**재발 방지** — 같은 실수를 잡아내는 회귀 테스트를 추가했습니다.
`AspectJProxyFactory` 로 프록시를 만들어 **대상 메서드가 실제로 호출됐는지**, **예외가 전파되는지**를 검증합니다.

### 2. Kotlin 주석은 중첩된다

**문제** — KDoc 안에 `/api/internal/**` 를 적었더니 컴파일이 깨졌습니다.

```
e: BankingApiProperties.kt: (24, 1): Unclosed comment
```

Kotlin 의 블록 주석은 **중첩을 지원**합니다. 문서 안의 `/**` 가 새 주석을 열어버리고,
닫는 `*/` 가 안쪽 주석만 닫으면서 바깥 주석이 끝나지 않은 상태가 됐습니다.
Java 였다면 문제되지 않았을 문자열입니다.

**해결** — 주석에서 해당 표기를 걷어냈습니다. 사소하지만 Java 감각으로 Kotlin 을 쓸 때 걸리는 지점이었습니다.

### 3. 타임아웃 없는 HTTP 호출

**문제** — 매 실행마다 `RestTemplate()` 을 새로 만들고 있었고, 기본 타임아웃은 **무제한**입니다.
API 서버가 응답하지 않으면 스케줄러 스레드가 그대로 묶이고,
10초 주기로 계속 쌓이면서 스레드 고갈로 이어집니다.

**해결** — `RestTemplateBuilder` 로 커넥션 · 응답 타임아웃을 건 인스턴스를 **빈으로 한 번만** 만들어 주입합니다.
타임아웃 값도 설정으로 빼 환경마다 조정할 수 있게 했습니다.

### 4. 인증 없이 사용자 목록 전체를 긁어오던 호출

**문제** — 수집 대상이 `http://localhost:8182/api/userDatas` 로 **하드코딩**돼 있었습니다.
두 서버가 같은 호스트에 있어야만 동작하고, 인증 없이 **사용자 테이블 전체**를 10초마다 가져오고 있었습니다.
하루 8,640번, 필요한 것은 집계값뿐인데 말입니다.

**해결** — API 서버에 내부 전용 집계 엔드포인트를 두고, 주소와 키를 설정으로 분리했습니다.
수집 주기도 기본 1분으로 늦췄습니다. 필요한 데이터만, 인증된 경로로 가져옵니다.

<br>

## ⚙️ 실행 방법

### 로컬

```bash
cd scheduler_server

INTERNAL_API_KEY=local-internal-api-key \
API_BASE_URL=http://localhost:8182 \
./gradlew bootRun
```

### Docker Compose

```bash
cat > .env <<'ENV'
API_BASE_URL=http://host.docker.internal:8182
INTERNAL_API_KEY=<api 서버와 동일한 값>
ENV

docker compose up -d --build
```

api 저장소의 compose 와 같은 네트워크에서 띄우려면 `docker-compose.yml` 의
`networks` 주석을 풀고 `API_BASE_URL` 을 `http://banking-api:8182` 로 지정합니다.

### 환경변수

| 변수 | 필수 | 기본값 | 설명 |
|---|:---:|---|---|
| `INTERNAL_API_KEY` | ✅ | | API 서버의 `banking.internal.api-key` 와 **동일한 값** |
| `API_BASE_URL` | | `http://localhost:8182` | API 서버 주소 |
| `SNAPSHOT_CRON` | | `0 * * * * *` | 수집 주기 (1분) |
| `SCHEDULER_ENABLED` | | `true` | 스케줄 on/off |
| `API_CONNECT_TIMEOUT` | | `2s` | 커넥션 타임아웃 |
| `API_READ_TIMEOUT` | | `5s` | 응답 타임아웃 |
| `SERVER_PORT` | | `8181` | |

<br>

## 🧪 테스트

```bash
cd scheduler_server && ./gradlew test
```

총 **8건**.

| 테스트 | 건수 | 검증 내용 |
|---|:---:|---|
| `RecordAspectTest` | 3 | **proceed 호출 여부** · 반환값 전달 · 예외 전파 |
| `BankingApiClientTest` | 2 | 내부 키 헤더 · 응답 매핑 · 401 처리 |
| `SnapshotCollectTaskTest` | 2 | 수집 동작 · API 실패 시 스케줄 유지 |
| `SchedulerServerApplicationTests` | 1 | 컨텍스트 로딩 |

<br>

## ⚠️ 운영 시 주의

- **인스턴스를 2대 이상 띄우면 같은 시각에 중복 실행됩니다.**
  다중화가 필요해지면 ShedLock 같은 분산 락을 먼저 도입해야 합니다.
- API 호출이 실패해도 예외를 밖으로 던지지 않으므로 스케줄은 계속 돕니다.
  연속 실패는 `snapshot collect failed` 로그로 감지하세요.
- 계측 로그의 `returned` 는 **메서드가 정상 반환했다**는 뜻입니다.
  작업이 내부에서 예외를 잡아 로깅했다면 `returned` 와 `ERROR` 가 함께 보일 수 있습니다.

<br>

## 🚢 배포

`develop` 에 push 되면 GitHub Actions 가 빌드 → 테스트 → NCP 배포를 수행합니다.
**Pull Request 에서는 빌드 · 테스트만 돌고 배포는 실행되지 않습니다.**

```
/home/<user>/scheduler_server/
├── run.sh          # CI 가 jar 와 함께 업로드
├── .env            # 환경변수 (chmod 600, 저장소에 없음)
└── scheduler_server-*.jar
```

필요한 Secrets: `NCP_HOST` · `NCP_USERNAME` · `NCP_PASS` · `NCP_PORT`

<br>

## 📝 남은 과제

- [ ] ShedLock 도입 — 다중 인스턴스 중복 실행 방지
- [ ] 수집 결과 영속화 — 현재는 로그로만 남음
- [ ] 연속 실패 시 알림 (Slack 등)
- [ ] `RestTemplate` → `RestClient` 전환
- [ ] Docker 이미지 빌드 검증
