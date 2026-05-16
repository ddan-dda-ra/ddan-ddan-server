# 이슈 분석: 펫 카탈로그 색상 코드 필드 추가

## 요구사항 요약

펫 카탈로그(`PetCatalog`)의 각 항목에 "색상 코드(color code)" 필드를 추가하여 클라이언트(`GET /v1/pets/catalog`) 응답에 노출한다. 직전 PR #308(commit `0439b7f`, `7e1bb8c`)에서 같은 도메인에 `backgrounds(homeUrl/homeCompactUrl/friendCardUrl)`를 추가했던 작업과 구조적으로 동일한 패턴 — 도메인 모델 → 엔티티 → 응답 DTO → 어드민 DTO → 시드 → 기존 DB 마이그레이션의 5중 변경이 필요하다. 다만 "색상 코드"의 정확한 의미(단일 hex 1개인지, primary/secondary 등 여러 개인지, nested object 형태인지)는 입력에서 결정되지 않았고, 본 단계에서 추측하지 않는다 — 미해결 질문으로 분류한다.

## 영향 받는 레이어

- [x] domain — `PetCatalogItem`에 색상 필드 추가 (+ 색상이 nested 구조라면 `PetCatalogColors` 같은 신규 value object 추가)
- [x] application — `PetCatalogService.create/update` 시그니처에 색상 파라미터 추가
- [x] infrastructure — `PetCatalogEntity` 필드 추가 + nested 엔티티(필요 시) + `PetCatalogSeeder` 기본값 + **기존 5개 도큐먼트 마이그레이션**
- [x] presentation — 일반 응답 DTO `PetCatalogResponse` + 어드민 요청/응답 DTO `PetCatalogAdmin*` 양쪽 모두

## 관련 코드

| 파일 | 라인 | 역할 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/model/petcatalog/PetCatalog.kt` | 10-17 | `PetCatalogItem` — 색상 필드를 어디에 끼울지 결정할 1차 지점. `backgrounds: PetCatalogBackgrounds` 바로 옆/아래가 자연 위치. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/model/petcatalog/PetCatalog.kt` | 19-23 | `PetCatalogBackgrounds` — nested value object의 **참조 모델**. 색상이 다중 컬러라면 동일 패턴으로 `PetCatalogColors(...)` 신규 클래스 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/entity/PetCatalogEntity.kt` | 14-26 | `PetCatalogEntity` 본문 — `backgrounds: PetCatalogBackgroundsEntity` 옆에 색상 필드 추가. MongoDB는 `SnakeCaseFieldNamingStrategy` 사용(camelCase ↔ snake_case 자동) — DB 마이그레이션 작성 시 키 표기 주의(MEMORY 항목 참조). |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/entity/PetCatalogEntity.kt` | 27-46 | `toDomain()` / `fromDomain()` 매퍼 — 색상 매핑 라인 양방향 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/entity/PetCatalogEntity.kt` | 50-70 | `PetCatalogBackgroundsEntity` — nested 엔티티의 **참조 모델**. 다중 컬러 구조라면 동일하게 `PetCatalogColorsEntity`를 만든다. 단일 hex라면 `PetCatalogEntity`에 `val themeColor: String` 한 줄로 끝. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/seed/PetCatalogSeeder.kt` | 22-33 | 시드 entity 생성부 — 색상 기본값 매핑 1줄 추가 필요. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/seed/PetCatalogSeeder.kt` | 43-48, 68-75 | `buildBackgrounds(species)` 헬퍼 + `DEFAULT_PETS` — 펫 5종(CAT/HAMSTER/PENGUIN/DOG/MOLE)의 기본 색상 값을 species별로 어떻게 할당할지 정해야 한다. backgrounds처럼 species 문자열로 CDN 경로를 만드는 방식이 아니므로 단순 lookup 또는 `PetSeed`에 `color` 필드 추가 형태가 자연스럽다. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/PetCatalogService.kt` | 53-82 | `create(...)` — 시그니처에 색상 파라미터 추가. backgrounds와 동일 위치(displayOrder 직전)에 끼우는 패턴. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/PetCatalogService.kt` | 84-106 | `update(...)` — 동일 패턴. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/response/PetCatalogResponse.kt` | 21-40 | `PetCatalogItemResponse` — 색상 필드 + `from()` 매핑 추가. **클라이언트 노출 지점**(요구사항 본문에 명시된 "클라이언트에 내려줘야 한다"의 실체). |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/response/PetCatalogResponse.kt` | 42-55 | `PetCatalogBackgroundsResponse` — nested 응답 DTO **참조 모델**. 다중 컬러 구조면 같은 패턴으로 `PetCatalogColorsResponse` 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 11-26 | `PetCatalogAdminCreateRequest` — 색상 요청 필드 + `@field:Valid`/`@field:NotBlank` 검증 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 28-41 | `PetCatalogAdminUpdateRequest` — 동상. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 43-57 | `PetCatalogBackgroundsRequest` — 요청 nested DTO **참조 모델**. 다중 컬러면 동일 패턴. 단일 hex면 `PetCatalogAdmin*Request`에 `val themeColor: String` 한 줄. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 75-94 | `PetCatalogAdminItemResponse` + `from()` — 어드민 응답에도 색상 표시. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 96-109 | `PetCatalogBackgroundsResponse`(어드민용) — 어드민 응답 nested DTO 참조 모델. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/admin/PetCatalogAdminController.kt` | 30-44 | `create(...)` — 컨트롤러 자체는 `request.backgrounds.toDomain()` 라인 옆에 `request.colors.toDomain()`(또는 `request.themeColor`) 1줄 추가. 시그니처는 그대로. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/admin/PetCatalogAdminController.kt` | 46-61 | `update(...)` — 동상. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/PetCatalogController.kt` | 18-22 | `getCatalog()` — 컨트롤러는 무변경. 응답 DTO의 `from(...)`이 색상까지 매핑하므로 자동 노출됨. **요구사항의 핵심 노출 엔드포인트.** |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/repository/PetCatalogRepository.kt` | 7-15 | 변경 없음. 색상은 정렬/조회 키가 아니므로 신규 쿼리 메서드 불필요. |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/service/PetCatalogServiceTest.kt` | 24-51 | 테스트 fixture(`entity(...)`)가 backgrounds를 명시적으로 만들어 넣는 패턴 — 색상도 동일하게 fixture 보강 필요. mockk 단위 테스트라 stub만 수정. |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/PetCatalogControllerIntegrationTest.kt` | (PR #308 diff 기준 +7라인) | 응답 JSON에 색상 필드가 노출되는지 통합 테스트로 검증. backgrounds와 동일 패턴으로 jsonPath 단언 추가. |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/admin/PetCatalogAdminControllerIntegrationTest.kt` | (PR #308 diff 기준 +24라인) | 어드민 CRUD 통합 테스트 — 색상 필드 요청/응답 검증. |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/filter/PetCatalogVersionFilterTest.kt` | 전체 | 영향 없음. version 헤더만 검증하므로 색상과 무관. |

## 참조할 기존 패턴

- **PR #308 `backgrounds` 추가 (commits `0439b7f`, `7e1bb8c`)** — 본 작업의 1:1 미러링 대상. backgrounds는 nested value object(`homeUrl`/`homeCompactUrl`/`friendCardUrl`) 3-필드 구조로, 도메인 → 엔티티 → 응답 DTO → 어드민 DTO → 시드의 5중 변경을 거쳤다. 색상도 nested 구조라면 동일 매핑이 적용된다. 또한 `7e1bb8c`에서 `home/homeCompact/friendCard` → `homeUrl/homeCompactUrl/friendCardUrl`로 `*Url` 접미사 컨벤션을 통일한 이력이 있다 — **색상 필드명도 동일 컨벤션 점검 필요** (예: `color` 단일이라면 `themeColor` / `colorCode`, hex 문자열이라면 접미사 없이 그대로 두는 게 맞는지).
- **`PetCatalogBackgrounds` (도메인) / `PetCatalogBackgroundsEntity` (인프라) / `PetCatalogBackgroundsResponse` (응답·어드민)** — nested value object 3계층의 표준 패턴. 색상이 다중 컬러면 정확히 동일하게 3개 클래스를 만든다.
- **`PetCatalogLevel` 매핑** — `Map<Int, PetCatalogLevel>` 방식은 색상에는 부적합. 색상은 펫당 1개(또는 고정 키셋 nested 1개)이므로 levels가 아닌 backgrounds 패턴을 따른다.
- **`PetCatalogSeeder.buildBackgrounds(species)` (43-48)** — 시드 헬퍼 함수의 형태. 색상은 species → URL 매핑이 아니라 species → hex 매핑이므로 함수 시그니처는 비슷하되 본문은 단순 `when (species)` 룩업 또는 `PetSeed.color` 필드 추가가 자연스럽다.
- **기존 도큐먼트 `$set` 마이그레이션 컨벤션 (PR #308 커밋 메시지 명시: "기존 5 docs는 별도 마이그레이션으로 \$set")** — 코드 시드는 신규 환경(빈 컬렉션)에서만 동작(`if (repository.count() > 0L) return`)이므로 dev/prod의 기존 5개 도큐먼트(CAT/HAMSTER/PENGUIN/DOG/MOLE)는 mongosh `$set`으로 별도 마이그레이션해야 한다. **MongoDB는 SnakeCaseFieldNamingStrategy 사용 — 마이그레이션 스크립트의 키는 snake_case로 작성** (MEMORY 항목: PR #308 사고 이력).

## 위험 요소

- **응답 DTO 호환성** — `PetCatalogResponse`에 nullable 아닌 필드를 추가하면 기존 응답 스키마가 변한다. 클라이언트(iOS)가 카탈로그를 아직 미사용 상태이므로 backgrounds처럼 wire 호환성 영향은 사실상 없다(PR #308 commit body 명시). 그래도 OpenAPI 스펙은 갱신되므로 swagger 노출은 신규 필드로 잡힌다.
- **기존 DB 도큐먼트 마이그레이션** — `PetCatalogSeeder.seed()`는 `count() > 0L`이면 즉시 return하므로 시드 코드만 고치면 신규 환경에서만 색상이 채워진다. dev/prod의 기존 5 docs(`pet_catalog` 컬렉션)에는 색상 필드가 빠진 채로 남아 Kotlin 역직렬화 시 nullable이 아닌 필드는 예외를 던질 위험이 있다. **마이그레이션 mongosh 스크립트가 PR과 동시에 준비되어야 한다.** dev/prod 양쪽 모두 적용 필요.
- **역직렬화 실패 시 서비스 전면 다운** — 색상 필드를 nullable 아닌 `String`/value object로 추가하고 마이그레이션을 누락하면, 첫 카탈로그 조회에서 `MappingException`/`NullValueException`이 발생하여 `GET /v1/pets/catalog`, `POST /v1/pets`(랜덤 펫 추가) 등 카탈로그 의존 API가 전부 실패한다. 대안: (a) 코드에 default 값 부여(생성자 기본값), (b) 필드를 nullable로 추가 후 데이터 백필 완료 시 non-null 전환, (c) 마이그레이션을 코드 배포보다 먼저 수행. PR #308에서는 (c)로 처리한 것으로 보임.
- **`SnakeCaseFieldNamingStrategy` 키 표기 실수** — Kotlin 필드 `themeColor` ↔ MongoDB 필드 `theme_color`. mongosh 마이그레이션 시 `{ $set: { themeColor: "#FFCC00" } }`로 쓰면 DB에는 `themeColor`라는 별도 키가 생기고 Spring Data는 `theme_color`를 읽으려 해 null이 된다. MEMORY 항목(PR #308 사고)에서 동일 사고가 이미 발생했음.
- **`PetCatalogService.currentVersion()` 캐시(`60s TTL`) 무효화** — service.create/update에서 `invalidateVersionCache()`를 호출하므로 어드민 API 경로의 일관성은 보장된다. 다만 **mongosh로 직접 마이그레이션할 경우 캐시가 자동 무효화되지 않는다** — 최대 60초간 stale `version`을 응답하므로 마이그레이션 직후 클라이언트가 새 색상을 받아도 헤더 `X-Pet-Catalog-Version`이 갱신되지 않을 수 있다. 운영 영향은 미미하나 인지 필요.
- **어드민 요청 검증** — 색상이 hex 문자열이라면 단순 `@NotBlank`로는 잘못된 값(`"red"`, `"#GGG"`)을 막지 못한다. `@Pattern(regexp = "^#[0-9A-Fa-f]{6}$")` 등 hex 정규식 검증을 추가하는 것이 안전하다. 다중 컬러(객체)면 각 필드별로 동일 규칙 적용.
- **시드 기본값 정확성** — 펫 5종의 "어떤 색"이 정답인지(디자이너/PM 공급값) 입력에 없다. 시드 코드의 default는 어드민이 추후 수정한다는 전제로 임시값을 박을 수도 있지만, 그렇게 되면 신규 dev 환경 첫 부팅 시 사용자 노출 색상이 misleading해진다. 디자인 공급값 필요.

## 미해결 질문

1. **색상의 구조** — 단일 hex 1개(`themeColor: "#FFCC00"`)인가, primary/secondary 등 다중 컬러 nested object(`colors: { primary, secondary, background }`)인가, 컬러 팔레트 리스트(`colors: ["#FFCC00", "#AABBCC"]`)인가? — backgrounds처럼 `PetCatalogColors` value object를 만들지, 도메인 모델에 평탄한 단일 필드만 추가할지 결정에 직결.
2. **필드명 컨벤션** — `colorCode` / `color` / `themeColor` / `colors` / `palette` 중 무엇? 단일 hex면 `themeColor`가 자연스럽고, nested면 `colors`가 자연스럽다. (`backgrounds`가 복수형 nested인 것과 정합) PR #308의 `*Url` 접미사 컨벤션 통일 이력에 비추어, hex string은 접미사 없이 두는 게 가독성에 맞다.
3. **필수 vs 옵셔널** — 색상이 모든 펫에 강제되는 필수 메타데이터인지, 일부 펫만 색상이 정의되는 옵셔널인지? non-null 필드 추가는 마이그레이션 의무를 동반(기존 5 docs 백필 필수). nullable이면 코드는 안전하지만 클라이언트가 fallback UI를 가져야 한다.
4. **기존 DB 데이터 백필 정책** — PR #308과 동일하게 mongosh `$set`으로 별도 마이그레이션할 것인가? 그렇다면 펫 5종(CAT/HAMSTER/PENGUIN/DOG/MOLE)의 색상 정답값은 누가 공급(디자이너/PM)하는가? 디자인 토큰 확정 전이라면 임시값으로 박고 어드민 API로 추후 갱신할지?
5. **어드민 요청 검증 강도** — hex 정규식(`^#[0-9A-Fa-f]{6}$`) 적용? 3자리 단축형(`#FFF`) 허용? 알파 채널 8자리(`#FFCC0080`) 허용? `rgba(...)` 같은 CSS 표기 허용?
6. **클라이언트 사용 시점** — 요구사항 본문은 "클라이언트에 내려줘야 한다"만 명시. iOS가 이번 스프린트에 실사용하는지(즉시 노출), wire만 먼저 깔고 추후 사용인지(PR #308과 동일한 wire-first 패턴)? 사용 시점에 따라 default 값의 품질 기준이 달라진다.
