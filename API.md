# ddan-ddan API

이 문서는 클라이언트 개발자와 개발 에이전트가 API 계약을 빠르게 찾기 위한 진입점입니다.

## 문서 탐색 순서

1. 인증, 공통 헤더, 에러 형식은 [공통 규칙](docs/api/common.md)을 먼저 확인합니다.
2. 기능별 호출 순서와 업무 규칙은 아래 도메인 문서를 확인합니다.
3. 정확한 HTTP 경로, 요청·응답 스키마와 enum은 [OpenAPI 스냅샷](docs/api/openapi.yaml)을 기준으로 합니다.
4. 실행 중인 서버의 최신 명세는 `/swagger-ui/index.html` 또는 `/v3/api-docs.yaml`에서 확인합니다.

## 서버

| 환경 | Base URL |
|---|---|
| Development | `https://dev-ddan-ddan-api.ddmz.org` |
| Production | `https://ddan-ddan-api.ddmz.org` |
| Local | `http://localhost:8080` |

## 도메인 문서

- [인증](docs/api/auth.md)
- [사용자](docs/api/users.md)
- [펫 및 펫 카탈로그](docs/api/pets.md)
- [친구](docs/api/friends.md)
- [랭킹](docs/api/ranking.md)
- [응원](docs/api/cheers.md)
- [운영자 API](docs/api/admin.md)

## 문서 관리 규칙

- 컨트롤러, 요청·응답 DTO, 인증 정책, 상태 코드 또는 에러 코드가 바뀌면 같은 변경에서 API 문서를 갱신합니다.
- HTTP 계약은 `docs/api/openapi.yaml`, 클라이언트 호출 흐름과 업무 규칙은 도메인 Markdown에 기록합니다.
- OpenAPI 스냅샷은 애플리케이션 실행 후 `./scripts/update-openapi.sh [base-url]`로 갱신합니다.
- 문서 변경 후 `./scripts/validate-api-docs.sh`로 OpenAPI YAML과 Markdown 링크를 검증합니다.
- 내부 구현만 변경되어 API 계약에 영향이 없다면 문서를 수정하지 않고 API 문서 에이전트 산출물에 그 근거를 남깁니다.
