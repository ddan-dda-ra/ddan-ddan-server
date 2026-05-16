# 설계: 펫 카탈로그 색상 코드(`colorCode`) 필드 추가

## 결정 사항 요약 (입력에서 확정된 사항 반영)

| 항목 | 결정 |
|---|---|
| 구조 | **단일 hex 문자열 1개**. nested value object 없음. |
| 필드명 | **`colorCode`** (camelCase, 도메인/엔티티/응답 DTO/요청 DTO 동일). MongoDB는 `SnakeCaseFieldNamingStrategy`로 `color_code`로 영속화. |
| nullability | **non-null `String`** (필수). |
| hex 검증 | `@field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")` — 6자리만. 3자리 단축 / 알파 채널 / `rgba(...)` 불가. |
| 시드 기본값 | MOLE `#D0DAE4`, DOG `#9B6CFF`, CAT `#FD85FF`, PENGUIN `#4E95FF`, HAMSTER `#46F8A2`. |
| 클라이언트 사용 | wire-first (PR #308과 동일 패턴). |
| DB 마이그레이션 | dev/prod 모두 mongosh `$set`. **코드 배포보다 먼저** 실행. 키는 **snake_case `color_code`**. |

## backgrounds 패턴과의 차이점 (의도된 분기)

| 비교 항목 | `backgrounds` (PR #308) | `colorCode` (본 작업) |
|---|---|---|
| 구조 | nested value object 3-필드 (`PetCatalogBackgrounds(homeUrl, homeCompactUrl, friendCardUrl)`) | 평탄한 단일 `String` 필드 |
| 도메인 클래스 신설 | `PetCatalogBackgrounds` 1개 추가 | **없음** — `PetCatalogItem`에 필드 1줄만 추가 |
| 엔티티 nested class | `PetCatalogBackgroundsEntity` | **없음** |
| 응답 nested DTO | `PetCatalogBackgroundsResponse` (응답 + 어드민 응답 각각) | **없음** |
| 요청 nested DTO | `PetCatalogBackgroundsRequest` | **없음** |
| `*Url` 접미사 컨벤션 | 모든 필드(`homeUrl` 등)가 URL이므로 `Url` 접미사 통일 (commit `7e1bb8c`) | **`colorCode`는 hex 문자열이지 URL이 아니므로 접미사 없음.** 컨벤션 정합 — "type을 접미사로 드러낸다"가 원칙이고, `code` 자체가 의미 표시 접미사 역할을 한다 |
| 시드 헬퍼 함수 | `buildBackgrounds(species)` — species → CDN URL 매핑 | **없음** — `PetSeed.colorCode: String` 필드 추가, 5종에 직접 lookup |
| 검증 | 각 URL `@field:NotBlank` 3개 | hex 정규식 `@field:Pattern` 1개 |
| 마이그레이션 키 | `backgrounds.home_url` 등 nested 경로 | 평탄 `color_code` (root) |

## 도메인 모델

### `PetCatalogItem` (domain/model/petcatalog/PetCatalog.kt)

`backgrounds` 직후, `isActive` 직전에 평탄한 단일 필드를 추가한다.

```kotlin
data class PetCatalogItem(
    val type: String,
    val name: String,
    val backgrounds: PetCatalogBackgrounds,
    val colorCode: String,        // 신규 — non-null hex (예: "#9B6CFF")
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevel>,
)
```

**비즈니스 규칙:**
- `colorCode`는 6자리 hex 문자열이며, 어드민 입력에서 정규식으로 검증된다. 도메인 모델 자체에는 검증 로직을 두지 않는다 (DTO `@field:Pattern`이 단일 시점에서 보장하고, 도메인은 string으로 단순 운반). 이는 backgrounds의 각 URL이 도메인 단에서 별도 검증 로직을 갖지 않는 것과 일관된다.
- 신규 value object를 만들지 않은 이유: 펫당 1개 hex뿐이고, 의미상 분해할 하위 필드가 없으므로 nested 클래스 도입은 과도한 캡슐화다.

### `PetCatalogBackgrounds`, `PetCatalogLevel`

**변경 없음.**

## API 명세

### 클라이언트 응답 — `GET /v1/pets/catalog` (변경 영향)

요청/엔드포인트 자체는 **무변경**. 응답 body JSON에 `pets[].colorCode` 신규 필드가 노출된다.

| Method | Path | Request | Response 변경 | Auth |
|---|---|---|---|---|
| GET | `/v1/pets/catalog` | (없음) | `pets[].colorCode: string` 필드 추가 (필수, 6자리 hex) | 공개 (filter 통과) |

응답 헤더 `X-Pet-Catalog-Version`은 그대로. 색상 변경도 `updatedAt` 갱신을 통해 자연스럽게 버전이 올라간다.

### 어드민 — `POST /v1/admin/pet-catalog`, `PUT /v1/admin/pet-catalog/{type}` (요청 body 변경)

| Method | Path | Request 변경 | Response 변경 | Auth |
|---|---|---|---|---|
| POST | `/v1/admin/pet-catalog` | body에 `colorCode: string`(hex 6자리 필수) 추가 | `colorCode` 필드 응답에 노출 | Admin |
| PUT | `/v1/admin/pet-catalog/{type}` | 동상 | 동상 | Admin |
| GET | `/v1/admin/pet-catalog` | 무변경 | `pets[].colorCode` 노출 | Admin |
| DELETE | `/v1/admin/pet-catalog/{type}` | 무변경 | `colorCode` 필드 응답에 노출 | Admin |

요청 예시:

```json
POST /v1/admin/pet-catalog
{
  "type": "QUOKKA",
  "name": "쿼카",
  "backgrounds": { "homeUrl": "...", "homeCompactUrl": "...", "friendCardUrl": "..." },
  "colorCode": "#FFCC00",
  "isActive": true,
  "displayOrder": 5,
  "levels": { ... }
}
```

검증 실패 케이스 (`MethodArgumentNotValidException` → 400):
- 누락: `colorCode` 없음 → Kotlin non-null 역직렬화 실패 (Jackson `MissingKotlinParameterException` → 400)
- 형식 위반: `"red"`, `"#GGGGGG"`, `"#FFF"`, `"#FFCC0080"`, `"rgba(...)"` → `@Pattern` 위반 → 400

## 서비스 시그니처

### `PetCatalogService` (application/service/PetCatalogService.kt 수정)

`create(...)` 및 `update(...)`의 시그니처에 `colorCode: String`을 `backgrounds` 직후, `isActive` 직전에 추가한다 (도메인 모델과 같은 순서).

```kotlin
@Service
class PetCatalogService(
    private val repository: PetCatalogRepository,
) {
    // 기존 메서드들 (시그니처 무변경): getActiveCatalog, currentVersion, getAllForAdmin,
    // pickRandomActiveExcluding, getName, requireActive, softDelete

    fun create(
        type: String,
        name: String,
        backgrounds: PetCatalogBackgrounds,
        colorCode: String,            // 신규
        isActive: Boolean,
        displayOrder: Int,
        levels: Map<Int, PetCatalogLevel>,
    ): PetCatalogItem

    fun update(
        type: String,
        name: String,
        backgrounds: PetCatalogBackgrounds,
        colorCode: String,            // 신규
        isActive: Boolean,
        displayOrder: Int,
        levels: Map<Int, PetCatalogLevel>,
    ): PetCatalogItem
}
```

**구현 변경:**
- `create()` 내부 `PetCatalogEntity(...)` 생성자 호출에 `colorCode = colorCode` 1줄 추가.
- `update()` 내부 `existing.copy(...)` 호출에 `colorCode = colorCode` 1줄 추가.
- 다른 메서드(`softDelete`, `getActiveCatalog` 등)는 무변경.

## MongoDB 스키마

### `PetCatalogEntity` (infrastructure/database/entity/PetCatalogEntity.kt)

`backgrounds` 필드 직후에 `colorCode: String` 추가. **nested entity 클래스는 만들지 않는다.**

```kotlin
@Document("pet_catalog")
data class PetCatalogEntity(
    @Id
    val id: ObjectId = ObjectId(),
    @Indexed(unique = true)
    val type: String,
    val name: String,
    val backgrounds: PetCatalogBackgroundsEntity,
    val colorCode: String,        // 신규 — Mongo: color_code
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val levels: Map<Int, PetCatalogLevelEntity>,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
```

**매퍼 변경:**
- `toDomain()`에 `colorCode = colorCode` 1줄 추가
- `fromDomain(item)`에 `colorCode = item.colorCode` 1줄 추가

**MongoDB 컬렉션 필드명:** `SnakeCaseFieldNamingStrategy` 적용에 따라 DB에서는 `color_code`로 저장된다. Kotlin 필드명은 `colorCode`.

**인덱스/쿼리:** `colorCode`는 정렬/필터 키가 아니므로 신규 인덱스 불필요. 기존 `type` unique index 그대로.

### 마이그레이션 필요 여부

**필요.** 기존 dev/prod의 5개 도큐먼트(CAT/HAMSTER/PENGUIN/DOG/MOLE)에 `color_code`가 없으면 Spring Data가 Kotlin `non-null String` 필드 역직렬화에서 실패하여 `GET /v1/pets/catalog` 등 카탈로그 의존 API 전부가 다운된다. **코드 배포 전에 mongosh 마이그레이션 선행 필수.**

### mongosh 마이그레이션 스크립트

**핵심 주의: 키는 반드시 snake_case `color_code` 사용** (MEMORY 항목, PR #308 사고 재발 방지).

#### dev (`dev-ddan-ddan-db`)

```javascript
use("dev-ddan-ddan-db");

// 5종 모두에 color_code 채움 + updatedAt 갱신 (캐시 무효화 효과)
const now = new Date();
const colorMap = {
  MOLE: "#D0DAE4",
  DOG: "#9B6CFF",
  CAT: "#FD85FF",
  PENGUIN: "#4E95FF",
  HAMSTER: "#46F8A2",
};
for (const [type, color] of Object.entries(colorMap)) {
  db.pet_catalog.updateOne(
    { type: type },
    { $set: { color_code: color, updated_at: now } }
  );
}

// 검증: 5건 모두 color_code 채워졌는지 확인
db.pet_catalog.find({}, { type: 1, color_code: 1, _id: 0 }).toArray();
// 기대값: 5건, color_code 모두 #으로 시작하는 6자리 hex
```

#### prod (`ddan-ddan-db`)

```javascript
use("ddan-ddan-db");

const now = new Date();
const colorMap = {
  MOLE: "#D0DAE4",
  DOG: "#9B6CFF",
  CAT: "#FD85FF",
  PENGUIN: "#4E95FF",
  HAMSTER: "#46F8A2",
};
for (const [type, color] of Object.entries(colorMap)) {
  db.pet_catalog.updateOne(
    { type: type },
    { $set: { color_code: color, updated_at: now } }
  );
}

db.pet_catalog.find({}, { type: 1, color_code: 1, _id: 0 }).toArray();
```

**실행 순서:**
1. mongosh로 dev 마이그레이션 → 검증 쿼리로 5건 확인 → dev 앱이 기존 코드로도 동작하는지 (color_code 필드 무시) 확인.
2. dev 코드 배포 → smoke 테스트로 `colorCode` 응답 노출 확인.
3. prod 마이그레이션 → 검증.
4. prod 코드 배포.

**캐시 영향:** mongosh로 `updated_at`도 함께 갱신했으므로, 코드 배포 후 첫 `currentVersion()` 호출(또는 60초 TTL 만료 후)에 새 version이 반영된다. `X-Pet-Catalog-Version` 헤더로 클라이언트 캐시도 자연스럽게 무효화.

## 예외

**신규 도메인 예외 없음.** 입력 검증 실패는 Spring `MethodArgumentNotValidException` (이미 `WebExceptionHandler`에서 처리되리라 가정 — 기존 backgrounds 검증과 동일 경로). 도메인 단에서는 `colorCode`를 plain `String`으로 운반하므로 도메인 예외 발생 지점 없음.

## 시드 (PetCatalogSeeder)

### `PetSeed` 데이터 클래스에 `colorCode` 필드 추가

```kotlin
private data class PetSeed(
    val type: String,
    val name: String,
    val species: String,
    val colorCode: String,    // 신규
)
```

### `DEFAULT_PETS`에 색상 채움

```kotlin
private val DEFAULT_PETS =
    listOf(
        PetSeed(type = "CAT",     name = "고양이",   species = "cat",     colorCode = "#FD85FF"),
        PetSeed(type = "HAMSTER", name = "햄스터",   species = "hamster", colorCode = "#46F8A2"),
        PetSeed(type = "PENGUIN", name = "펭귄",     species = "penguin", colorCode = "#4E95FF"),
        PetSeed(type = "DOG",     name = "강아지",   species = "dog",     colorCode = "#9B6CFF"),
        PetSeed(type = "MOLE",    name = "두더지",   species = "mole",    colorCode = "#D0DAE4"),
    )
```

### `seed()` 내부 entity 생성부

`PetCatalogEntity(...)` 생성자 호출에 `colorCode = pet.colorCode` 1줄 추가 (backgrounds 직후).

```kotlin
PetCatalogEntity(
    type = pet.type,
    name = pet.name,
    backgrounds = buildBackgrounds(pet.species),
    colorCode = pet.colorCode,        // 신규
    isActive = true,
    displayOrder = index,
    levels = buildLevels(pet.species),
    createdAt = now,
    updatedAt = now,
)
```

### `buildBackgrounds` / `buildLevels`

**무변경.** `colorCode`는 species 기반 lookup이 아니라 type 기반 고정값이므로 헬퍼 함수 불필요. `PetSeed`에 필드를 추가하는 방식이 더 자연스럽다 (backgrounds처럼 species → URL 변환 규칙이 있는 게 아니라 단순 상수 매핑).

## 응답 DTO (presentation/dto/response/PetCatalogResponse.kt)

### `PetCatalogItemResponse`

`backgrounds` 직후에 `colorCode: String` 추가.

```kotlin
data class PetCatalogItemResponse(
    val type: String,
    val name: String,
    val backgrounds: PetCatalogBackgroundsResponse,
    val colorCode: String,            // 신규
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevelResponse>,
) {
    companion object {
        fun from(item: PetCatalogItem): PetCatalogItemResponse =
            PetCatalogItemResponse(
                type = item.type,
                name = item.name,
                backgrounds = PetCatalogBackgroundsResponse.from(item.backgrounds),
                colorCode = item.colorCode,    // 신규
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.mapValues { PetCatalogLevelResponse.from(it.value) },
            )
    }
}
```

`PetCatalogResponse`, `PetCatalogBackgroundsResponse`, `PetCatalogLevelResponse`는 **무변경**.

## 어드민 DTO (presentation/dto/admin/PetCatalogAdminDto.kt)

### `PetCatalogAdminCreateRequest`

`backgrounds` 직후에 `colorCode` 추가. hex 정규식 검증.

```kotlin
data class PetCatalogAdminCreateRequest(
    @field:NotBlank
    val type: String,
    @field:NotBlank
    val name: String,
    @field:Valid
    val backgrounds: PetCatalogBackgroundsRequest,
    @field:NotBlank
    @field:Pattern(regexp = HEX_COLOR_REGEX, message = "colorCode는 #으로 시작하는 6자리 hex(예: #FFCC00) 여야 합니다")
    val colorCode: String,
    val isActive: Boolean = true,
    @field:Min(0)
    val displayOrder: Int = 0,
    @field:NotEmpty
    @field:Valid
    val levels: Map<Int, PetCatalogLevelRequest>,
) {
    fun levelsToDomain(): Map<Int, PetCatalogLevel> = levels.mapValues { it.value.toDomain() }
}
```

### `PetCatalogAdminUpdateRequest`

```kotlin
data class PetCatalogAdminUpdateRequest(
    @field:NotBlank
    val name: String,
    @field:Valid
    val backgrounds: PetCatalogBackgroundsRequest,
    @field:NotBlank
    @field:Pattern(regexp = HEX_COLOR_REGEX, message = "colorCode는 #으로 시작하는 6자리 hex(예: #FFCC00) 여야 합니다")
    val colorCode: String,
    val isActive: Boolean,
    @field:Min(0)
    val displayOrder: Int,
    @field:NotEmpty
    @field:Valid
    val levels: Map<Int, PetCatalogLevelRequest>,
) {
    fun levelsToDomain(): Map<Int, PetCatalogLevel> = levels.mapValues { it.value.toDomain() }
}
```

### hex 정규식 상수 — 파일 최상단(또는 companion object) 추가

```kotlin
private const val HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$"
```

> `kotlin.text.Regex` 패턴이 아니라 `@Pattern`(jakarta.validation)이 사용할 `String` 상수. backgrounds DTO와 같은 파일(`PetCatalogAdminDto.kt`)의 top-level에 둔다.

### `PetCatalogAdminItemResponse`

`backgrounds` 직후에 `colorCode: String` 추가.

```kotlin
data class PetCatalogAdminItemResponse(
    val type: String,
    val name: String,
    val backgrounds: PetCatalogBackgroundsResponse,
    val colorCode: String,        // 신규
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevelResponse>,
) {
    companion object {
        fun from(item: PetCatalogItem): PetCatalogAdminItemResponse =
            PetCatalogAdminItemResponse(
                type = item.type,
                name = item.name,
                backgrounds = PetCatalogBackgroundsResponse.from(item.backgrounds),
                colorCode = item.colorCode,    // 신규
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.mapValues { PetCatalogLevelResponse.from(it.value) },
            )
    }
}
```

`PetCatalogAdminListResponse`, 어드민 측 nested DTO(`PetCatalogBackgroundsResponse`, `PetCatalogLevelResponse`, `PetCatalogBackgroundsRequest`, `PetCatalogLevelRequest`)는 **무변경**.

## 컨트롤러 (PetCatalogAdminController.kt)

시그니처 변경 없음. `create()`/`update()` 본문에서 `petCatalogService.create/update` 호출 시 `colorCode = request.colorCode` 1줄을 `backgrounds` 직후에 추가.

```kotlin
@PostMapping
fun create(
    @RequestBody @Valid request: PetCatalogAdminCreateRequest,
): PetCatalogAdminItemResponse =
    PetCatalogAdminItemResponse.from(
        petCatalogService.create(
            type = request.type,
            name = request.name,
            backgrounds = request.backgrounds.toDomain(),
            colorCode = request.colorCode,    // 신규
            isActive = request.isActive,
            displayOrder = request.displayOrder,
            levels = request.levelsToDomain(),
        ),
    )
```

`update()`도 동일 패턴. `list()`, `softDelete()`는 본문 무변경.

`PetCatalogController.kt`(클라이언트용)는 **완전 무변경** — 응답 DTO `from(...)`이 `colorCode`를 자동 매핑하므로.

## 변경 파일 목록

### 생성

**없음.** 신규 클래스 없음 (단일 평탄 필드 추가이므로).

### 수정

| 파일 | 위치/라인 | 변경 내용 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/model/petcatalog/PetCatalog.kt` | 10-17 (`PetCatalogItem`) | `backgrounds` 직후, `isActive` 직전에 `val colorCode: String` 1줄 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/entity/PetCatalogEntity.kt` | 14-26 (`PetCatalogEntity` 본문) | `backgrounds` 직후, `isActive` 직전에 `val colorCode: String` 1줄 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/entity/PetCatalogEntity.kt` | 27-46 (`toDomain`, `fromDomain`) | 각각에 `colorCode = ...` 1줄씩 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/seed/PetCatalogSeeder.kt` | 22-33 (entity 생성부) | `colorCode = pet.colorCode` 1줄 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/seed/PetCatalogSeeder.kt` | 59-63 (`PetSeed`) | `val colorCode: String` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/seed/PetCatalogSeeder.kt` | 68-75 (`DEFAULT_PETS`) | 5개 항목 모두에 `colorCode = "#..."` 추가 (위 표 값) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/PetCatalogService.kt` | 53-82 (`create`) | 시그니처에 `colorCode: String` 파라미터 추가, `PetCatalogEntity(...)` 생성자 호출에 `colorCode = colorCode` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/PetCatalogService.kt` | 84-106 (`update`) | 동상 — 시그니처 추가 + `existing.copy(...)`에 `colorCode = colorCode` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/response/PetCatalogResponse.kt` | 21-40 (`PetCatalogItemResponse`) | `colorCode: String` 필드 + `from()` 매핑 1줄 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 파일 상단 (import 직후) | `import jakarta.validation.constraints.Pattern` + `private const val HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$"` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 11-26 (`PetCatalogAdminCreateRequest`) | `colorCode` 필드 + `@field:NotBlank` + `@field:Pattern(regexp = HEX_COLOR_REGEX, ...)` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 28-41 (`PetCatalogAdminUpdateRequest`) | 동상 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 75-94 (`PetCatalogAdminItemResponse`) | `colorCode` 필드 + `from()` 매핑 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/admin/PetCatalogAdminController.kt` | 30-44 (`create`) | service 호출 인자에 `colorCode = request.colorCode` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/admin/PetCatalogAdminController.kt` | 46-61 (`update`) | 동상 |

### 무변경 (영향 없음)

- `src/main/kotlin/.../presentation/controller/PetCatalogController.kt` — 응답 DTO가 자동으로 `colorCode` 노출
- `src/main/kotlin/.../infrastructure/database/repository/PetCatalogRepository.kt`
- `src/main/kotlin/.../presentation/filter/PetCatalogVersionFilter.kt`

### 테스트 파일

테스트 파일은 test-engineer가 별도로 추가/보강한다 (아래 "테스트 계획" 참고).

## 테스트 계획

### 단위 테스트 fixture 보강

| 파일 | 변경 |
|---|---|
| `src/test/kotlin/.../application/service/PetCatalogServiceTest.kt` | (1) `entity(...)` 헬퍼: `colorCode = "#$key".padEnd(7, 'A')` 등 임의 hex 또는 고정 `"#AABBCC"` 추가. (2) `sampleBackgrounds()` 옆에 색상 인자를 직접 넘기는 형태로 `service.create(...)` / `service.update(...)` 호출부 4곳에 `colorCode = "#FFCC00"` 추가. (3) "create는 colorCode를 entity에 저장한다" 단언 신규 테스트 1건 추가 — `saved.captured.colorCode shouldBe "#FFCC00"`. (4) "update는 colorCode를 갱신한다" 단언 신규 테스트 1건 추가. |
| `src/test/kotlin/.../presentation/controller/PetCatalogControllerIntegrationTest.kt` | (1) `PetCatalogItem` fixture에 `colorCode = "#FFCC00"` 추가 (1곳). (2) jsonPath 단언 추가: `.andExpect(jsonPath("$.pets[0].colorCode").value("#FFCC00"))`. |
| `src/test/kotlin/.../presentation/controller/admin/PetCatalogAdminControllerIntegrationTest.kt` | (1) `item(...)` fixture에 `colorCode = "#FFCC00"` 추가. (2) `PetCatalogAdminCreateRequest` / `PetCatalogAdminUpdateRequest` 빌더에 `colorCode = "#FFCC00"` 추가 (2곳). (3) 단언 추가: `jsonPath("$.pets[0].colorCode").value("#FFCC00")` (GET list), `jsonPath("$.colorCode").value("#FFCC00")` (POST/PUT). (4) **신규 테스트: hex 정규식 검증 실패** — 잘못된 colorCode(`"red"`, `"#GGG"`, `"#FFCC0080"`)로 POST 요청 시 400 응답 확인. (5) `verify { petCatalogService.create(... any() ... ) }` 시그니처에 인자 1개 추가(`any()` 추가 또는 명시값). |

### 시드 단위 테스트 (선택)

`PetCatalogSeederTest`가 이미 존재한다면 5종에 대해 `entity.colorCode`가 위 5색 매핑과 일치하는지 단언 추가. (현재 미확인. 존재하지 않으면 신규 작성 생략.)

### 회귀 방지 통합 테스트 (이미 존재하는 통합 테스트 보강으로 충분)

`PetCatalogControllerIntegrationTest` + `PetCatalogAdminControllerIntegrationTest`의 jsonPath 단언 추가로 충분. 별도 통합 테스트 신설 불필요.

### `PetCatalogVersionFilterTest`

**영향 없음.** 헤더만 검증하므로 colorCode와 무관.

## 결정 이유

### 왜 평탄한 단일 필드인가 (사용자 결정 반영)

- 펫당 색상이 1개로 확정됐고, primary/secondary 등 의미 분해도 없다. nested value object는 "복수의 연관 필드를 묶는다"가 핵심 가치인데, 이 경우 묶을 형제 필드가 없다.
- backgrounds가 nested인 이유는 `homeUrl/homeCompactUrl/friendCardUrl` 3개가 항상 함께 변하는 단위였기 때문. colorCode는 그런 단위가 아니다.
- 도메인/엔티티/요청 DTO/응답 DTO/어드민 DTO 모두 **클래스 신설 0개**로 처리 가능 — 변경 라인 수도 backgrounds보다 훨씬 적다.

### 왜 필드명이 `colorCode`인가 (사용자 결정 반영)

- `themeColor`도 후보였으나 사용자가 **`colorCode`**로 결정 (camelCase 일관성).
- backgrounds의 `*Url` 접미사 컨벤션(`homeUrl` 등 commit `7e1bb8c`)과 정합: 접미사는 "필드의 타입(URL인지 코드인지)"을 드러내는 역할이고, `colorCode`의 `Code` 자체가 그 역할을 한다. URL이 아니므로 `Url` 접미사는 부적합.
- 단순 `color`보다 `colorCode`가 명시적이다. hex 코드라는 표기법을 필드명으로 드러낸다.

### 왜 hex 정규식 검증을 어드민 DTO에만 두는가

- 도메인 모델은 `String` 그대로 받고 비즈니스 규칙 메서드는 두지 않는다. backgrounds의 URL도 도메인 단에서 별도 검증을 하지 않는 것과 일관.
- 입력 진입점(어드민 요청)에서 한 번 검증하면 충분. 시드 코드는 우리가 작성한 상수이므로 컴파일 타임에 신뢰 가능.
- 정규식 위치는 `PetCatalogAdminDto.kt` top-level 상수로 두어 `Create`/`Update` 양쪽에서 재사용.

### 왜 hex 6자리 strict (3자리 단축 / 알파 / rgba 불가)

- 사용자 결정.
- 클라이언트(iOS) 측에서 hex string 파싱 시 6자리 strict가 가장 단순. 3자리 단축/알파/rgba를 허용하면 클라이언트 파서가 분기를 가져야 한다.
- 디자이너 공급값 5색이 모두 6자리 hex이므로 strict 정책 적용 가능.

### 왜 마이그레이션이 코드 배포보다 선행해야 하는가

- `colorCode`는 non-null `String`. 마이그레이션 없이 코드 배포하면 기존 5개 도큐먼트 역직렬화 시 `MappingException`/`NullValueException`이 발생하여 `GET /v1/pets/catalog`, `POST /v1/pets`(랜덤 펫 추가), `requireActive(...)` 등 카탈로그 의존 API가 전부 다운된다.
- PR #308의 backgrounds와 정확히 동일한 위험. 동일 절차로 처리.
- **대안 비교:**
  - nullable로 추가 후 백필: 클라이언트가 fallback UI를 가져야 하고, non-null 전환 시 마이그레이션을 또 해야 한다. 2단계 배포가 됨. **탈락.**
  - 도메인 default 값(`val colorCode: String = "#000000"`): Spring Data Mongo는 default를 항상 인식하지 않으며 (kotlin reflect 의존), 잘못된 default가 운영에 나갈 위험. **탈락.**
  - mongosh 선행 마이그레이션 (현재 안): PR #308과 정합, 가장 단순. **채택.**

### 왜 마이그레이션 키가 snake_case `color_code`인가

- MongoDB는 `SnakeCaseFieldNamingStrategy` 적용. Kotlin `colorCode` ↔ DB `color_code`.
- mongosh로 `{ $set: { colorCode: "..." } }` 라고 쓰면 DB에는 `colorCode`라는 별도 키가 생기고, Spring Data는 `color_code`를 읽으려 해 null이 된다 → 역직렬화 실패.
- MEMORY 항목(PR #308 사고)에서 이 사고가 이미 발생. 반드시 snake_case로 작성한다.

### 왜 마이그레이션에서 `updated_at`도 함께 갱신하는가

- `currentVersion()`은 `max(updatedAt)`을 반환하므로, 마이그레이션 후 클라이언트가 새로 받은 카탈로그의 버전이 갱신되어야 캐시 무효화가 자연스럽게 동작한다.
- 60초 TTL이 끝나면 자동 반영되지만, 명시적으로 `updated_at`을 `new Date()`로 박아두면 그 시점 이후 첫 호출부터 즉시 반영된다.

### 왜 신규 컨트롤러 / 신규 엔드포인트를 만들지 않는가

- 요구사항은 "기존 `GET /v1/pets/catalog` 응답에 색상 노출". 신규 엔드포인트가 필요 없는 wire-additive 변경.
- 어드민 CRUD도 이미 존재하므로 요청/응답 DTO에 필드만 추가하면 끝.

### 왜 wire-first인가 (사용자 결정 반영)

- PR #308과 동일 패턴. iOS 즉시 사용 여부와 무관하게 backend는 필드 노출만 보장.
- 따라서 통합 테스트는 "응답 JSON에 필드가 노출되는지" + "어드민 요청에서 검증되는지" 두 가지로 충분. 실제 화면 노출 시점은 iOS 작업에 위임.
