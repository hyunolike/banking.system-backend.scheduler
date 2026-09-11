---
name: Bug template
about: 버그 발생 시 사용하는 템플릿입니다
title: "[BE]"
labels: ''
assignees: hyunolike

---

## 버그 기능
- 어떤 기능에서 발생했는지 적습니다. (스냅샷 수집 / API 호출 / 실행 시간 계측 / 배포 등)

### 버그 상황 재연
- 어떤 상황에서 버그가 발생하는지 적습니다.

### 기대 동작
- 원래 기대하던 정상 동작에 대해 작성합니다.

### 현재 동작
- 기대하던 동작에 반해 지금 문제가 되는 동작을 작성합니다.

### 로그
- 관련 로그를 붙입니다. 스케줄 작업은 아래 두 줄이 함께 나오는지 확인해 주세요.

```
snapshot collected :: users=... accounts=... totalBalance=...
auto get api :: SnapshotCollectTask.collectSnapshot() :: returned :: ... ms
```

- `auto get api` 만 찍히고 `snapshot collected` 가 없다면 작업 본문이 실행되지 않은 것입니다.
- `snapshot collect failed` 가 보이면 API 서버 호출 자체가 실패한 것입니다.

### 실행 환경
- 실행 방식: (로컬 / Docker / NCP 배포)
- `API_BASE_URL`:
- `SNAPSHOT_CRON`:
- API 서버 상태: (정상 / 응답 없음 / 401 · 403)
