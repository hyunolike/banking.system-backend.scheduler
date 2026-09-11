# banking.system-backend.scheduler

🏦 뱅킹 서버 Scheduler

API 서버([banking.system-backend.api](https://github.com/hyunolike/banking.system-backend.api))의
현황 스냅샷을 주기적으로 수집하는 배치 서버입니다.

| 항목 | 값 |
|---|---|
| 언어 / 런타임 | Kotlin 1.7.22 / Java 17 |
| 프레임워크 | Spring Boot 3.0.4 (Web, AOP, Scheduling) |
| 포트 | 8181 |

---

## 1. 동작

```
SnapshotCollectTask        @Scheduled(cron = banking.scheduler.snapshot-cron)
        │
        ├── @RecordAutoGetAPI → RecordAspect 가 소요 시간을 로깅
        │
        └── BankingApiClient ── GET {banking.api.base-url}/api/internal/stats/snapshot
                                Header: X-Internal-Api-Key
```

수집한 값은 로그로 남습니다.

```
snapshot collected :: users=128 accounts=241 totalBalance=88123400.0000 generatedAt=2024-05-01T10:15:30
auto get api :: SnapshotCollectTask.collectSnapshot() :: returned :: 37 ms
```

## 2. 구조

```
com.banking_system.scheduler_server
├── aspects   RecordAspect, RecordAutoGetAPI      실행 시간 계측
├── client    BankingApiClient, SystemSnapshot    API 서버 호출
├── config    BankingApiProperties, RestTemplateConfig
└── task      SnapshotCollectTask                 스케줄 작업
```

## 3. 설정 (환경변수)

| 변수 | 필수 | 기본값 | 설명 |
|---|:---:|---|---|
| `INTERNAL_API_KEY` | ✅ | | API 서버의 `banking.internal.api-key` 와 **동일한 값** |
| `API_BASE_URL` | | `http://localhost:8182` | API 서버 주소 |
| `SNAPSHOT_CRON` | | `0 * * * * *` | 수집 주기 (1분) |
| `SCHEDULER_ENABLED` | | `true` | 스케줄 on/off |
| `API_CONNECT_TIMEOUT` | | `2s` | 커넥션 타임아웃 |
| `API_READ_TIMEOUT` | | `5s` | 응답 타임아웃 |
| `SERVER_PORT` | | `8181` | |

## 4. 로컬 실행 / 테스트

```bash
cd scheduler_server

INTERNAL_API_KEY=local-internal-api-key \
API_BASE_URL=http://localhost:8182 \
./gradlew bootRun

./gradlew test
```

## 5. 운영 시 주의

- **인스턴스를 2대 이상 띄우면 같은 시각에 중복 실행됩니다.** 다중화가 필요해지면
  ShedLock 같은 분산 락을 먼저 도입해야 합니다.
- API 호출이 실패해도 예외를 밖으로 던지지 않으므로 스케줄은 계속 돕니다.
  연속 실패는 `snapshot collect failed` 로그로 감지하세요.

## 6. 배포

`develop` 에 push 되면 GitHub Actions 가 빌드 → 테스트 → NCP 서버 배포까지 수행합니다.
Pull Request 에서는 빌드·테스트만 돌고 **배포는 실행되지 않습니다.**

서버 준비 사항:

```
/home/<user>/scheduler_server/
├── run.sh          # CI 가 jar 와 함께 업로드
├── .env            # 위 환경변수 (chmod 600, 저장소에 없음)
└── scheduler_server-*.jar
```

필요한 GitHub Secrets: `NCP_HOST`, `NCP_USERNAME`, `NCP_PASS`, `NCP_PORT`
