# API 공통 규칙

## 인증

- 일반 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.
- `/v1/auth/**`는 일반 사용자 토큰 없이 호출합니다.
- `/v1/admin/auth/**`는 운영자 토큰 없이 호출하며, 그 외 `/v1/admin/**`는 운영자 access token이 필요합니다.
- 일반 사용자 토큰과 운영자 토큰은 서로 대체할 수 없습니다.

## 공통 헤더

| Header | 적용 범위 | 설명 |
|---|---|---|
| `Authorization` | 인증 API | `Bearer {token}` |
| `X-Android-Version` | 일반 `/v1/**` | Android 앱의 semantic version입니다. |
| `X-iOS-Version` | 일반 `/v1/**` | iOS 앱의 semantic version입니다. |
| `X-Mock-OAuth` | `POST /v1/auth/login` | 로컬·개발 환경의 mock OAuth 사용 여부. 기본값은 `false`입니다. |

앱 버전 헤더는 둘 중 현재 플랫폼의 헤더 하나만 전송합니다. 헤더가 없으면 하위 호환을 위해 검사를 건너뛰며, 형식이 잘못되면 `426`과 `AV001`이 반환됩니다.

## 데이터 형식

- MongoDB 식별자는 24자리 ObjectId hex string입니다.
- 날짜는 `YYYY-MM-DD`, 날짜와 시간은 ISO-8601 문자열을 사용합니다.
- JSON 필드명은 OpenAPI 스냅샷의 표기를 그대로 사용합니다.
- 응답에 없는 필드를 클라이언트가 임의로 추론하지 않습니다.

## 에러 응답

```json
{
  "code": "UE001",
  "message": "유저를 찾을 수 없습니다.",
  "data": null
}
```

| HTTP 상태 | 대표 상황 |
|---|---|
| `400` | 도메인 규칙 위반, 요청 본문·파라미터 오류 |
| `401` | OAuth/JWT/운영자 인증 실패 |
| `403` | 권한 부족 |
| `404` | 경로 또는 HTTP 메서드 오류 |
| `409` | 이미 수행한 응원 |
| `426` | 앱 강제 업데이트 필요 |
| `500` | 처리되지 않은 서버 오류 |

주요 코드 접두사는 `DE`(공통), `AC`(인증), `AZ`(권한), `UE`(사용자), `PE`(펫), `RA`(랭킹), `IC`(초대코드), `FR`(친구), `CE`(응원), `PC`(펫 카탈로그), `AA`(운영자 인증)입니다.

## 계약 기준

- 경로, HTTP 메서드, 파라미터, DTO 필드는 [OpenAPI 스냅샷](openapi.yaml)이 기준입니다.
- 업무 규칙, 권장 호출 순서, 캐시 정책은 도메인별 Markdown이 기준입니다.
- 두 문서와 실제 코드가 다르면 컨트롤러·DTO·예외 처리 코드를 확인하고 같은 PR에서 문서를 바로잡습니다.
