# 펫 카탈로그 클라이언트 구현 프롬프트

아래 프롬프트를 클라이언트 저장소의 구현 에이전트에게 전달합니다. 대괄호 항목은 대상 프로젝트에 맞게 바꿉니다.

```text
당신은 [iOS/Android] 클라이언트 저장소에서 펫 카탈로그 기능을 구현한다.

먼저 저장소의 네트워크 계층, 인증 처리, 로컬 저장소, 이미지 캐시, Lottie 렌더링, 상태 관리 구조와 기존 펫 화면을 조사하라. 기존 아키텍처와 명명 규칙을 유지하고 새로운 추상화는 필요한 범위에서만 추가하라.

서버 계약:
- 모든 API는 Authorization: Bearer {accessToken}을 사용한다.
- GET /v1/pets/catalog은 항상 200과 전체 body를 반환한다.
- 응답은 revision과 활성 펫 목록을 포함한다.
- 카탈로그 항목 필드: type, name, colorCode, displayOrder, levels.
- levels 항목 필드: level, imageUrl, lottieDefaultUrl, lottiePlayEatUrl.
- backgrounds, isActive, ETag, X-Pet-Catalog-Version은 사용하지 않는다.
- 사용자 펫 필드: id, type, level, expPercent.
- type이 사용자 펫과 카탈로그를 연결하는 유일한 키다.
- 서버 에셋 level은 1~5지만 펫 level은 5를 초과할 수 있다.
- 펫 level이 6 이상이면 level 5 에셋을 사용한다.
- 배경은 colorCode로 클라이언트가 렌더링한다.
- 이미지 URL은 SVG/PNG/WebP이며 현재 기본 에셋은 SVG다. Lottie는 JSON이다.

사용 API:
- GET /v1/pets/catalog
- GET /v1/pets/me
- GET /v1/pets/{petId}
- GET /v1/users/me/main-pet
- POST /v1/users/me/main-pet, body: { "petId": "..." }
- POST /v1/pets/me, body: { "petType": "DOG" }
- POST /v1/pets/me/random
- POST /v1/pets/me/gacha
- POST /v1/pets/{petId}/food
- POST /v1/pets/{petId}/play

구현 요구사항:
1. 카탈로그 DTO와 도메인 모델을 현재 서버 계약에 맞게 수정한다.
2. 기존 backgrounds와 isActive 의존을 제거한다.
3. 카탈로그 배열로 type 기반 dictionary/map 인덱스를 만든다.
4. 저장된 revision과 서버 revision을 비교하는 로컬 catalog store를 구현한다.
5. revision이 같으면 기존 snapshot과 에셋을 유지한다.
6. revision이 다르면 새 snapshot을 원자적으로 저장하고 이전·신규 URL diff를 계산한다.
7. URL이 변경됐거나 파일이 없는 에셋만 다운로드한다. 같은 URL을 강제 재다운로드하지 않는다.
8. SVG와 Lottie JSON을 비동기 로드하고 URL 기반 디스크 캐시를 사용한다.
9. selectedAssetLevel은 catalog.levels 중 level <= pet.level인 가장 큰 값으로 계산한다.
10. type 누락, 빈 levels, 잘못된 색상, 네트워크·다운로드 실패에 placeholder/fallback을 적용한다.
11. 앱 시작 시 로컬 snapshot을 먼저 표시하고 로그인 후 서버 catalog를 동기화한다.
12. GET /v1/pets/me와 메인 펫 응답의 pet.type으로 catalogByType을 조회해 화면 모델을 만든다.
13. 먹이·놀이 응답의 최신 pet과 user 상태를 반영하고 level 변경 시 에셋을 다시 선택한다.
14. 펫 종류를 enum switch나 클라이언트 상수 목록으로 하드코딩하지 않는다.
15. 동기화는 single-flight로 만들고 오래된 요청이 최신 revision을 덮어쓰지 않게 한다.

검증 테스트:
- revision 동일/변경 분기
- URL diff 다운로드
- type 조인
- level 1~5 매핑과 level 6 이상 → level 5 fallback
- 알 수 없는 type 및 잘못된 catalog fallback
- 네트워크 실패 시 저장 snapshot 사용
- 먹이·놀이 후 level 변경
- backgrounds/isActive 없는 JSON decoding

작업 방식:
- 먼저 관련 파일과 현재 데이터 흐름을 요약한다.
- 변경 설계를 제시한 뒤 실제 구현한다.
- 기존 사용자 변경을 덮어쓰지 않는다.
- 구현 후 단위 테스트와 가능한 통합/UI 테스트를 실행한다.
- 마지막에 변경 파일, 동기화 흐름, 캐시 정책, 테스트 결과와 남은 위험을 보고한다.

서버 상세 문서가 필요하면 다음 문서를 기준으로 사용한다:
- docs/pet-catalog-client-implementation-guide.md
- docs/api/pets.md
- docs/api/openapi.yaml
```
