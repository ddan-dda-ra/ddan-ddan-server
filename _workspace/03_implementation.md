# 구현 결과

## 변경 파일

| 파일 | 동작 | 요약 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/model/petcatalog/PetCatalog.kt` | 수정 | `PetCatalogItem`에 `val colorCode: String` 1줄 추가 (backgrounds 직후, isActive 직전). |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/entity/PetCatalogEntity.kt` | 수정 | `PetCatalogEntity`에 `val colorCode: String` 추가 + `toDomain()` / `fromDomain()` 매퍼 양방향 1줄씩 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/seed/PetCatalogSeeder.kt` | 수정 | `PetSeed`에 `colorCode` 필드 추가, `DEFAULT_PETS` 5개 항목에 색상 채움(CAT `#FD85FF`, HAMSTER `#46F8A2`, PENGUIN `#4E95FF`, DOG `#9B6CFF`, MOLE `#D0DAE4`), `seed()` 내 entity 생성부에 `colorCode = pet.colorCode` 1줄 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/PetCatalogService.kt` | 수정 | `create(...)` / `update(...)` 시그니처에 `colorCode: String` 파라미터 추가, 본문의 `PetCatalogEntity(...)` 및 `existing.copy(...)`에 `colorCode = colorCode` 1줄씩 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/response/PetCatalogResponse.kt` | 수정 | `PetCatalogItemResponse`에 `colorCode: String` 필드 + `from()` 매핑 1줄 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/admin/PetCatalogAdminDto.kt` | 수정 | `import jakarta.validation.constraints.Pattern` 추가, top-level `private const val HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$"` 추가. `PetCatalogAdminCreateRequest` / `PetCatalogAdminUpdateRequest`에 `@field:NotBlank @field:Pattern(regexp = HEX_COLOR_REGEX, ...) val colorCode: String` 추가. `PetCatalogAdminItemResponse`에 `colorCode` 필드 + `from()` 매핑 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/admin/PetCatalogAdminController.kt` | 수정 | `create()` / `update()`의 `petCatalogService.create/update` 호출 인자에 `colorCode = request.colorCode` 1줄씩 추가. |

**무변경 파일(설계서 명시):**
- `presentation/controller/PetCatalogController.kt` — 응답 DTO `from(...)`이 자동 매핑하므로 무변경.
- `infrastructure/database/repository/PetCatalogRepository.kt` — 조회/정렬 키가 아니므로 무변경.
- `presentation/filter/PetCatalogVersionFilter.kt` — 헤더만 다루므로 무변경.

**생성 파일:** 없음 (단일 평탄 필드 추가이므로 신규 클래스 없음).

## 핵심 결정

- 설계 대비 변경 사항 **없음**. 설계서(`_workspace/02_design.md`)의 변경 파일 목록 7개 파일 + 변경 내역과 1:1 일치.
- 필드명 `colorCode` (camelCase), MongoDB는 `SnakeCaseFieldNamingStrategy`에 의해 `color_code`로 영속화. nested value object 신설 없이 평탄한 단일 `String` 필드.
- hex 정규식 검증은 어드민 요청 DTO(`PetCatalogAdminCreateRequest`, `PetCatalogAdminUpdateRequest`)에만 위치. 도메인 모델은 plain `String` 운반.

## 빌드 검증

- `./gradlew compileKotlin`: ✅ 성공 (JDK 17 사용).
- 초기에 시스템 기본 JDK 25로 실행 시 Gradle 8.8 + Kotlin 1.9.24 호환성 문제로 daemon 초기화 단계에서 `What went wrong: 25` 에러가 발생하여 `JAVA_HOME=/Users/.../azul-17.0.14`로 전환 후 정상 통과. 이는 환경 이슈이며 본 변경과는 무관.

```
> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :compileKotlin

BUILD SUCCESSFUL in 13s
1 actionable task: 1 executed
```

## test-engineer에게 전달할 메모

### 보강해야 할 기존 테스트 파일 (모두 main 변경에 의해 컴파일 깨짐 상태)

1. **`src/test/kotlin/.../application/service/PetCatalogServiceTest.kt`**
   - `entity(...)` / fixture 헬퍼에서 `PetCatalogEntity(...)` 생성자 호출 시 `colorCode = "#FFCC00"` 등 임의 hex 추가 필요.
   - `service.create(...)` / `service.update(...)` 호출부 4곳에 `colorCode = "#FFCC00"` 추가 필요.
   - 신규 단언 권장:
     - "create는 colorCode를 entity에 저장한다" — `saved.captured.colorCode shouldBe "#FFCC00"`.
     - "update는 colorCode를 갱신한다" — `existing.copy(...)` 결과의 `colorCode` 단언.

2. **`src/test/kotlin/.../presentation/controller/PetCatalogControllerIntegrationTest.kt`**
   - `PetCatalogItem(...)` fixture에 `colorCode = "#FFCC00"` 추가.
   - jsonPath 단언 추가: `jsonPath("$.pets[0].colorCode").value("#FFCC00")`.

3. **`src/test/kotlin/.../presentation/controller/admin/PetCatalogAdminControllerIntegrationTest.kt`**
   - `item(...)` fixture에 `colorCode = "#FFCC00"` 추가.
   - `PetCatalogAdminCreateRequest(...)` / `PetCatalogAdminUpdateRequest(...)` 빌더에 `colorCode = "#FFCC00"` 추가 (2곳).
   - 단언 추가: `jsonPath("$.pets[0].colorCode").value("#FFCC00")` (GET list), `jsonPath("$.colorCode").value("#FFCC00")` (POST/PUT 응답).
   - **신규 테스트 케이스 — hex 정규식 검증 실패**: `"red"`, `"#GGGGGG"`, `"#FFF"`, `"#FFCC0080"` 등 잘못된 colorCode로 POST 요청 시 400 응답 확인. `@field:Pattern`이 `WebExceptionHandler`로 어떻게 변환되는지 기존 backgrounds `@NotBlank` 실패 케이스와 동일 경로일 것.
   - `verify { petCatalogService.create(...) }` 시그니처에 `colorCode = any()` 또는 명시값 인자 추가 필요.

### Mocking / 경계 케이스 포인트

- **MockK relaxed mock 주의**: `PetCatalogService` 모킹 시 `create`/`update`가 추가 파라미터를 가져 기존 `every { ... }` 블록의 매처가 인자 개수 불일치로 실패할 수 있다. 모든 호출부에 `colorCode = any()` 명시 권장.
- **Jackson 역직렬화 실패**: `colorCode`가 non-null `String`이라 어드민 요청 body에 필드 누락 시 `MissingKotlinParameterException` → 400으로 빠진다. 이 케이스도 통합 테스트로 검증 가능 (선택).
- **시드 검증 (선택)**: `PetCatalogSeederTest`가 존재한다면 5종에 대해 `entity.colorCode`가 설계 표 5색과 일치하는지 단언 추가. (현재 파일 존재 미확인.)

### 외부 의존 / 마이그레이션 메모

- **mongosh 마이그레이션은 본 단계 구현 범위에 없음** (설계서 명시). PR 본문/배포 문서에 첨부할 운영 절차. 마이그레이션이 prod/dev 코드 배포보다 선행되지 않으면 기존 5개 도큐먼트의 `color_code` 누락으로 카탈로그 의존 API 전부가 다운된다 — 통합 테스트로는 잡히지 않는 위험.
- **`SnakeCaseFieldNamingStrategy`**: Kotlin `colorCode` ↔ DB `color_code`. mongosh 스크립트는 반드시 snake_case 키 사용 (MEMORY 항목 PR #308 사고 재발 방지).
