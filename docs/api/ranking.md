# 랭킹 API

## 엔드포인트

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `GET` | `/v1/ranking` | 필요 | 조건과 기간에 따른 랭킹을 조회합니다. |

## Query Parameters

| 이름 | 값 |
|---|---|
| `criteria` | `TOTAL_CALORIES`, `TOTAL_SUCCEEDED_DAYS`, `TOTAL_ATTENDANCE_DAYS` |
| `periodType` | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |

예시:

```http
GET /v1/ranking?criteria=TOTAL_CALORIES&periodType=WEEKLY
Authorization: Bearer {accessToken}
```

응답의 `ranking`은 상위 목록이며 `myRanking`은 현재 사용자의 순위입니다. 현재 사용자가 상위 목록에 있더라도 `myRanking`을 별도 기준으로 사용합니다.

## 주요 에러

- `RA001`: 현재 사용자의 랭킹 정보를 찾을 수 없음
- `DE0003`: 지원하지 않는 enum 또는 파라미터 값
