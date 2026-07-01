# 운영자 API

운영자 API는 일반 사용자 API와 별도의 JWT 인증 체계를 사용합니다.

## 인증

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `POST` | `/v1/admin/auth/login` | 불필요 | 운영자 access token을 발급합니다. |

```json
{
  "username": "admin",
  "password": "password"
}
```

응답의 `accessToken`을 그 외 `/v1/admin/**` 요청의 Bearer 토큰으로 사용합니다.

## 사용자 조회

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/v1/admin/users` | 검색·정렬·페이징된 사용자 목록 |
| `GET` | `/v1/admin/users/{id}` | 사용자 상세 |
| `GET` | `/v1/admin/users/{id}/daily-calories` | 기간별 일일 칼로리 기록 |

- 목록의 `page`는 0부터 시작합니다.
- `size`는 `1..100`, 기본값은 `20`입니다.
- `sort` 기본값은 `LATEST_LOGIN`입니다.
- 일일 칼로리 기간을 생략하면 최근 30일이며 최대 366일까지 조회합니다.

## 통계

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/v1/admin/stats/dashboard` | 사용자·펫·가입 추이 통계 |

`seriesDays`는 `1..90`, 기본값은 `14`이며 날짜 기준은 KST입니다.

## 주요 에러

- `AA001`: 운영자 자격 증명 오류
- `AA002`: 운영자 인증 필요
- `AA003`: 운영자 토큰 오류
- `AA004`: 운영자 토큰 만료
