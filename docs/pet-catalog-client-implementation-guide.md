# 펫 카탈로그 클라이언트 구현 가이드

## 목표

클라이언트는 서버의 펫 카탈로그를 표현 정보의 기준으로 사용하고, 사용자 펫 응답의 `type`으로 카탈로그 항목을 찾습니다.

- `Pet.id`: 먹이, 놀이, 메인 펫 설정 등 사용자 행동의 대상
- `Pet.type`: 펫 카탈로그 조인 키
- `Pet.level`: 사용할 카탈로그 에셋 단계 선택
- `Catalog.revision`: 로컬 카탈로그 갱신 여부 판단
- `Catalog.colorCode`: 클라이언트 배경 렌더링

서버는 `backgrounds`, Admin API, ETag 및 카탈로그 전용 header를 제공하지 않습니다.

## 사용하는 API

| 목적 | Method | Path |
|---|---|---|
| 카탈로그 동기화 | `GET` | `/v1/pets/catalog` |
| 보유 펫 조회 | `GET` | `/v1/pets/me` |
| 단일 펫 조회 | `GET` | `/v1/pets/{petId}` |
| 메인 펫 조회 | `GET` | `/v1/users/me/main-pet` |
| 메인 펫 설정 | `POST` | `/v1/users/me/main-pet` |
| 지정 펫 추가 | `POST` | `/v1/pets/me` |
| 랜덤 펫 추가 | `POST` | `/v1/pets/me/random` |
| 티켓 펫 뽑기 | `POST` | `/v1/pets/me/gacha` |
| 먹이 주기 | `POST` | `/v1/pets/{petId}/food` |
| 놀아주기 | `POST` | `/v1/pets/{petId}/play` |

모든 API는 `Authorization: Bearer {accessToken}`을 사용합니다.

## 권장 모델

```text
PetCatalog {
  revision: String
  pets: [PetCatalogItem]
}

PetCatalogItem {
  type: String
  name: String
  colorCode: String
  displayOrder: Int
  levels: [PetCatalogLevel]
}

PetCatalogLevel {
  level: Int
  imageUrl: URL
  lottieDefaultUrl: URL
  lottiePlayEatUrl: URL
}

Pet {
  id: String
  type: String
  level: Int
  expPercent: Double
}
```

조회 성능과 결합 안정성을 위해 카탈로그 배열과 별도로 다음 인덱스를 구성합니다.

```text
catalogByType: Map<String, PetCatalogItem>
```

`type`은 서버 값을 그대로 보존하고 대문자로 정규화하여 조회합니다. 화면 표시명으로 조인하지 않습니다.

## 앱 시작 흐름

1. 저장된 카탈로그와 `revision`을 로컬 저장소에서 읽습니다.
2. 저장된 카탈로그가 있으면 즉시 `catalogByType`을 구성해 화면을 복원합니다.
3. 로그인 이후 `GET /v1/pets/catalog`을 호출합니다.
4. 응답 `revision`이 저장값과 같으면 기존 카탈로그와 에셋을 유지합니다.
5. 다르면 새 카탈로그를 검증한 뒤 원자적으로 저장합니다.
6. 이전 카탈로그와 새 카탈로그의 URL을 비교해 변경된 URL만 다운로드합니다.
7. `GET /v1/pets/me`와 `GET /v1/users/me/main-pet`을 호출합니다.
8. 각 펫의 `type`으로 카탈로그 항목을 찾아 화면 모델을 생성합니다.

카탈로그 API는 조건부 요청을 사용하지 않고 항상 `200`과 전체 body를 반환합니다.

## revision과 로컬 저장

최소 저장 단위는 `revision`, 전체 카탈로그 JSON, 에셋 URL별 로컬 파일 정보입니다.

```text
StoredCatalog {
  revision
  catalogJSON
  assetCache[url] = localFile
}
```

새 카탈로그 저장은 중간 상태가 노출되지 않게 임시 저장 후 교체하거나 DB transaction을 사용합니다. 다운로드 실패 시 새 카탈로그 메타데이터는 저장하되 기존 에셋을 fallback으로 사용할 수 있습니다.

## 에셋 선택

서버 카탈로그 에셋은 정확히 level 1~5입니다. 펫 자체 레벨은 5를 초과할 수 있습니다.

```text
selectedLevel = catalog.levels
  .filter(level <= pet.level)
  .maxBy(level)
```

일반적인 데이터에서는 `min(pet.level, 5)`와 같습니다. 펫 레벨이 6 이상이면 항상 level 5 에셋을 사용합니다.

렌더링 매핑:

- 배경색: `catalog.colorCode`
- 정적 이미지: `selectedLevel.imageUrl`
- 기본 애니메이션: `selectedLevel.lottieDefaultUrl`
- 먹이·놀이 애니메이션: `selectedLevel.lottiePlayEatUrl`
- 표시 이름: `catalog.name`

현재 정적 이미지는 SVG이므로 플랫폼별 SVG 렌더러가 필요합니다. Lottie 파일은 JSON입니다.

## 에셋 다운로드 정책

카탈로그 revision이 변경됐다고 모든 에셋을 다시 받지 않습니다.

1. 이전·신규 카탈로그를 `type + level + asset role`로 비교합니다.
2. URL이 같으면 기존 파일을 유지합니다.
3. URL이 달라졌거나 로컬 파일이 없으면 다운로드합니다.
4. 다운로드 완료 후 URL과 로컬 파일을 연결합니다.
5. 새 카탈로그에서 참조하지 않는 파일은 즉시 또는 용량 정책에 따라 제거합니다.

같은 URL의 콘텐츠가 바뀌는 상황은 지원하지 않습니다. 서버 운영 정책상 에셋 변경은 새 URL로 배포됩니다.

## 사용자 행동 후 상태 갱신

### 펫 추가

지정 추가 요청은 카탈로그에서 선택한 `type`을 `petType`으로 전송합니다.

```json
{ "petType": "DOG" }
```

랜덤 추가와 티켓 뽑기는 응답의 `type`을 사용해 카탈로그와 결합합니다.

### 먹이와 놀이

`POST /v1/pets/{petId}/food` 또는 `/play` 응답의 `user`와 `pet`을 화면 상태에 즉시 반영합니다. 레벨이 변경되면 에셋을 다시 선택합니다.

### 메인 펫

메인 펫 설정에는 `type`이 아니라 보유 펫의 `petId`를 전송합니다. 응답 및 메인 펫 조회 결과의 `type`으로 카탈로그를 결합합니다.

## 오류와 fallback

- `type`에 해당하는 카탈로그가 없으면 앱을 종료하지 말고 기본 이름·색상·placeholder를 표시하고 진단 로그를 남깁니다.
- level 목록이 비었거나 선택 가능한 level이 없으면 placeholder를 사용합니다.
- 에셋 다운로드 실패 시 캐시된 동일 type/level 에셋 또는 placeholder를 사용합니다.
- 잘못된 `colorCode`는 플랫폼 기본 배경색으로 대체합니다.
- 카탈로그 요청 실패 시 저장된 카탈로그가 있으면 오프라인 상태로 계속 사용합니다.
- 저장된 카탈로그도 없으면 재시도 UI와 최소 placeholder UI를 제공합니다.

## 동시성

- 카탈로그 갱신 작업은 하나만 실행되도록 single-flight 처리합니다.
- 여러 화면은 immutable snapshot 또는 observable store 하나를 구독합니다.
- 늦게 완료된 과거 요청이 최신 revision을 덮어쓰지 않도록 응답 적용 순서를 검사합니다.
- 이미지·Lottie 다운로드는 제한된 동시성으로 수행합니다.

## 필수 테스트

- 같은 revision이면 메타데이터와 에셋을 다시 저장·다운로드하지 않음
- 다른 revision이면 URL이 변경된 에셋만 다운로드함
- `type`으로 보유 펫과 카탈로그가 결합됨
- level 1~5는 같은 level 에셋을 사용함
- level 6 이상은 level 5 에셋을 사용함
- 알 수 없는 type, 빈 levels, 잘못된 색상에 fallback 적용
- 카탈로그 네트워크 실패 시 저장된 snapshot 사용
- 먹이·놀이 후 level 변경 시 화면 에셋 갱신
- 메인 펫 설정은 petId를 전송하고 type으로 렌더링
- 응답 모델에 `backgrounds`, `isActive`를 요구하지 않음

## 완료 기준

- 클라이언트 코드에 펫 종류별 switch/하드코딩이 없음
- 모든 표현 정보가 `catalogByType[pet.type]`에서 결정됨
- 레벨 상한을 5로 제한하지 않고 에셋 선택만 level 5로 fallback함
- revision 비교와 URL diff 다운로드가 동작함
- 캐시가 없어도 안전하게 placeholder를 표시함
- API 모델과 [서버 OpenAPI](api/openapi.yaml)가 일치함

