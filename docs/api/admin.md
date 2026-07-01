# 운영자 API

운영자 API는 일반 사용자 API와 별도의 JWT 인증 체계를 사용합니다. 운영자 로그인과 토큰 발급은 별도 Admin 서버가 담당하며 이 서버는 로그인 API를 제공하지 않습니다.

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

- `AA002`: 운영자 인증 필요
- `AA003`: 운영자 토큰 오류
- `AA004`: 운영자 토큰 만료
