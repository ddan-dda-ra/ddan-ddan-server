# iOS 펫 카탈로그 handoff

이 문서는 공개 iOS 적용과 출시 gate를 정의합니다. 정확한 HTTP 스키마는 [OpenAPI 스냅샷](api/openapi.yaml), 서버 업무 규칙은 [펫 API](api/pets.md)를 기준으로 합니다.

## 모델 변경

- public `PetCatalog`에 필수 `revision: String`을 추가하고 `backgrounds`, `isActive`를 제거합니다. `revision`은 ISO-8601 문자열입니다.
- 각 항목에 필수 `colorCode: String`을 추가합니다. 서버 값은 uppercase `#RRGGBB`이며 배경은 이 값으로 생성합니다. 파싱 실패 시 앱의 안전한 기본 배경색을 사용합니다.
- `levels`를 `[String: PetCatalogLevel]`에서 `[PetCatalogLevel]`로 변경하고 각 원소의 필수 `level: Int`를 사용합니다. 서버 응답은 level `1...5`가 오름차순으로 제공됩니다.

## 카탈로그 동기화

1. `GET /v1/pets/catalog`의 `200 OK` body를 decode합니다.
2. 응답 `revision`이 저장값과 같으면 기존 catalog와 에셋을 유지합니다.
3. `revision`이 다르면 새 catalog를 저장하고 이전 catalog와 각 `imageUrl`, `lottieDefaultUrl`, `lottiePlayEatUrl`을 비교합니다.
4. URL이 달라진 에셋만 다운로드하고 URL이 같은 에셋은 기존 메모리·디스크 캐시를 유지합니다.

## 에셋 URL 불변성

- 서버 운영에서는 에셋 바이트를 변경할 때 versioned path·파일명 또는 query로 새 URL을 발급합니다.
- 같은 URL의 원격 객체가 다른 콘텐츠로 덮어써진다고 가정해 재다운로드하지 않습니다.
- 현재 URL은 immutable baseline이며 이후 콘텐츠 변경은 반드시 새 URL로 전달됩니다.

## SVG와 에셋 선택

- `imageUrl`의 공식 지원 형식에 SVG가 포함되며 현재 dev/prod 기준 에셋은 SVG입니다. 원격 SVG는 SVGKit으로 download → decode → render하고 메모리·디스크 캐시 재조회까지 검증합니다.
- SVG 다운로드·파싱·렌더 실패나 손상 캐시에서는 `last-known-good` → 동일 type/level 번들 이미지 → 공통 placeholder 순서로 fallback합니다. 빈 화면이나 crash는 허용하지 않습니다.
- 실제 펫은 level 5를 초과해 성장할 수 있습니다. 에셋은 `assetLevel = min(pet.level, 5)`로 선택합니다. 더 방어적으로는 `levels`에서 `level <= pet.level`인 원소 중 가장 큰 level을 선택합니다.

## 공개 gate

- 서버 fixture가 `backgrounds`, `isActive` 없이 decode되고 `revision`, `colorCode`, 배열 `levels`, level `1...5`를 검증해야 합니다.
- 같은 `revision`에서는 catalog와 에셋 캐시를 유지하고, 다른 `revision`에서는 URL이 달라진 에셋만 다운로드해야 합니다.
- level 6 이상 펫 fixture가 level 5 에셋을 선택해야 합니다.
- 실제 CDN SVG fixture의 SVGKit decode/render와 메모리·디스크 cache hit를 검증해야 합니다.
- 실패 fixture에서 `last-known-good` → 번들 → placeholder fallback 순서를 검증해야 합니다.
- 기존 type/level 하드코딩을 카탈로그 repository 기반 조회로 전환하고 revision 비교 통합 테스트가 통과해야 합니다.

위 gate를 모두 통과하기 전에는 해당 iOS 버전을 공개하지 않습니다.
