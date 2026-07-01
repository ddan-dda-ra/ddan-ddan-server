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

## 펫 카탈로그

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/v1/admin/pet-catalog` | 비활성 항목을 포함한 전체 조회 |
| `POST` | `/v1/admin/pet-catalog` | 항목 등록 |
| `PUT` | `/v1/admin/pet-catalog/{type}` | 항목 전체 수정 |

- DELETE endpoint는 제공하지 않습니다.
- 항목은 비활성 상태로 생성할 수 있고 `false → true` 출시는 허용합니다. 출시된 항목의 `true → false` 변경은 `PC004`로 거부합니다.
- `colorCode`는 `#RRGGBB`, `displayOrder`는 0 이상이어야 합니다.
- 요청·응답의 `levels`는 `level` 필드가 있는 배열이며 정확히 level `1..5`를 중복 없이 포함해야 합니다. 응답은 level 오름차순입니다.
- 이미지 URL은 `.svg`, `.png`, `.webp`, Lottie URL은 `.json`만 지원합니다.
- 활성 항목을 수정하거나 비활성 항목을 활성화하면 `updatedAt`이 갱신되어 public `revision`이 변경됩니다. 비활성 준비 항목만 수정하면 public `revision`은 변경되지 않습니다.
- 에셋 콘텐츠 변경 시 새 versioned path·파일명 또는 query URL을 입력해야 하며 같은 URL의 객체를 덮어쓰면 안 됩니다.

## 주요 에러

- `AA001`: 운영자 자격 증명 오류
- `AA002`: 운영자 인증 필요
- `AA003`: 운영자 토큰 오류
- `AA004`: 운영자 토큰 만료
- `PC001`: 카탈로그 항목 없음
- `PC002`: 중복 카탈로그 키
- `PC003`: 비활성 펫 종류
- `PC004`: 카탈로그 입력값 또는 활성화 전환 규칙 위반
