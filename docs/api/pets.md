# 펫 API

> **Breaking change:** 공개 전 카탈로그 v1 계약에서 `revision`을 추가하고 `isActive`, `backgrounds`를 제거했으며 `levels`를 배열로 변경했습니다. 이전 응답 모델과 호환되지 않습니다.

## 엔드포인트

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `GET` | `/v1/pets/catalog` | 필요 | 활성 펫 카탈로그를 조회합니다. |
| `POST` | `/v1/pets/me` | 필요 | 지정한 종류의 펫을 추가합니다. |
| `POST` | `/v1/pets/me/random` | 필요 | 랜덤 펫을 추가합니다. |
| `POST` | `/v1/pets/me/gacha` | 필요 | 티켓 한 장으로 랜덤 펫을 뽑습니다. |
| `GET` | `/v1/pets/me` | 필요 | 내가 소유한 펫을 모두 조회합니다. |
| `GET` | `/v1/pets/{petId}` | 필요 | 내가 소유한 특정 펫을 조회합니다. |
| `POST` | `/v1/pets/{petId}/food` | 필요 | 먹이를 소모해 펫을 성장시킵니다. |
| `POST` | `/v1/pets/{petId}/play` | 필요 | 장난감을 소모해 펫을 성장시킵니다. |

## 펫 추가

```json
{
  "petType": "DOG"
}
```

`petType`은 카탈로그의 `type` 값을 사용합니다. 클라이언트에 종류를 하드코딩하기보다 카탈로그 응답을 기준으로 선택지를 구성합니다.

## 카탈로그 동기화

- 공개 응답에는 활성 항목만 포함되며 `isActive`, `backgrounds` 필드는 없습니다. endpoint는 항상 `200`과 전체 body를 반환합니다.
- body의 `revision`은 응답에 포함된 활성 항목들의 `updatedAt` 최댓값을 ISO-8601 문자열로 표현합니다. 활성 항목이 없으면 `1970-01-01T00:00:00Z`입니다.
- 로그인 이후 클라이언트는 모든 API 요청에 저장한 `revision`을 `X-Pet-Catalog-Version`으로 전송합니다. 응답의 `X-Pet-Catalog-Download-Required`가 `true`면 카탈로그 API를 호출해 새 body를 저장하고, 이전 catalog와 URL을 비교해 URL이 달라진 에셋만 다운로드합니다.
- 카탈로그 API 응답의 body `revision`은 로컬에 저장해 이후 요청 헤더 값으로 사용합니다.
- 항목 정렬은 `(displayOrder ASC, type ASC)`, `levels`는 `level` 오름차순 배열입니다. 서버 에셋 level은 정확히 `1..5`입니다. 세부 스키마는 [OpenAPI 스냅샷](openapi.yaml)을 확인합니다.
- `imageUrl`은 `.svg`, `.png`, `.webp`를 지원하며 현재 기준 에셋 형식은 SVG입니다. Lottie URL은 `.json`입니다.
- 배경은 `backgrounds`가 아니라 uppercase `#RRGGBB` 형식의 `colorCode`를 기준으로 구성합니다.

## 에셋 URL 불변성

- 에셋 콘텐츠를 변경할 때는 versioned path·파일명 또는 query를 사용해 새 URL을 발급합니다.
- 같은 URL의 CDN 객체를 다른 바이트로 덮어쓰지 않습니다. 서버는 원격 콘텐츠 변경을 자동 감지하지 않습니다.
- 현재 live URL은 최초 immutable baseline으로 유지하며, 이후 콘텐츠가 바뀌는 시점부터 새 versioned URL을 사용합니다.

## 성장 API

- 먹이 지급은 사용자 먹이를, 놀아주기는 장난감을 소비합니다.
- 응답의 `user` 재고와 `pet` 레벨·경험치를 화면 상태의 기준으로 사용합니다.
- 펫은 level 5를 초과해 계속 성장할 수 있습니다. 카탈로그의 `1..5`는 성장 상한이 아니라 에셋 단계입니다.
- 펫 level이 5를 초과하면 level 5 에셋을 사용합니다. 방어적으로는 `level <= pet.level`인 항목 중 가장 큰 level을 선택합니다.

## 주요 에러

- `UE002`: 먹이 부족
- `UE003`: 장난감 부족
- `UE004`: 티켓 부족
- `PE001`: 펫을 찾을 수 없음
- `PE002`: 펫 소유자 불일치
- `PE003`: 최대 레벨
- `PC001`: 카탈로그 항목 없음
- `PC003`: 비활성 펫 종류
