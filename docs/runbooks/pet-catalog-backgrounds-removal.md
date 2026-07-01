# 펫 카탈로그 backgrounds 제거 운영 Runbook

이 작업은 애플리케이션 배포와 MongoDB 수동 마이그레이션을 분리해 수행합니다. 구버전 애플리케이션은 `backgrounds`가 없는 문서를 읽지 못할 수 있으므로 아래 순서를 변경하지 않습니다.

## 1. 실행 전 현황 기록

대상 환경의 `pet_catalog`에서 다음 값을 기록합니다.

```javascript
db.pet_catalog.countDocuments({})
db.pet_catalog.countDocuments({ backgrounds: { $exists: true } })
db.pet_catalog.find({}, { updated_at: 1 }).sort({ updated_at: -1 }).limit(1)
```

기록한 전체 문서 수와 `backgrounds` 대상 문서 수는 migration 안전장치 입력값으로 사용합니다.

## 2. 신버전 전체 배포 확인

1. `backgrounds` 필드가 제거된 신버전을 모든 인스턴스에 배포합니다.
2. 로드밸런서 뒤의 모든 인스턴스가 신버전인지 배포 플랫폼의 이미지 태그 또는 커밋 SHA로 확인합니다.
3. 구버전 인스턴스가 하나라도 남아 있으면 migration을 실행하지 않습니다.

## 3. API smoke

사용자 JWT로 다음 API를 호출합니다.

- `GET /v1/pets/catalog`

응답에서 다음을 확인합니다.

- `pets[].backgrounds`가 존재하지 않음
- `colorCode`와 `levels`가 존재함
- 일반 응답 body의 `revision`이 활성 카탈로그의 max `updated_at`과 일치함
- 기본 카탈로그 8종과 레벨 SVG/Lottie URL이 유지됨

## 4. 백업

migration 직전에 `pet_catalog` 컬렉션을 별도 컬렉션 또는 export 파일로 백업합니다. 별도 관리 서버에서 등록한 커스텀 데이터가 있을 수 있으므로 기본 시더로 복구할 수 있다고 가정하지 않습니다.

백업의 문서 수가 1단계에서 기록한 전체 문서 수와 같은지 확인합니다.

## 5. 수동 migration 실행

대상 환경의 MongoDB 접속 환경변수를 명시하고, 1단계에서 확인한 값을 입력합니다.

```bash
export PET_CATALOG_BACKGROUNDS_MIGRATION_CONFIRM=NEW_VERSION_DEPLOYED_API_SMOKED_BACKUP_COMPLETED
export PET_CATALOG_MIGRATION_EXPECTED_TOTAL=<전체 문서 수>
export PET_CATALOG_MIGRATION_EXPECTED_TARGET=<backgrounds 존재 문서 수>

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
SPRING_PROFILES_ACTIVE=<dev|prod> \
./gradlew test \
  --rerun-tasks \
  --tests notbe.tmtm.ddanddanserver.migration.PetCatalogBackgroundsRemovalMigration.migrate
```

확인 문구가 정확하지 않으면 테스트는 실행되지 않습니다. 실제 전체/대상 문서 수가 입력값과 다르면 update 전에 실패합니다.

## 6. DB 및 버전 검증

migration 로그의 `matchedCount`, `modifiedCount`, `remaining`, `totalAfter`, `migrationStartedAt`을 기록하고 다음을 확인합니다.

```javascript
db.pet_catalog.countDocuments({})
db.pet_catalog.countDocuments({ backgrounds: { $exists: true } })
db.pet_catalog.find({}, { updated_at: 1 }).sort({ updated_at: -1 }).limit(1)
```

성공 조건:

- 전체 문서 수가 실행 전과 동일함
- `backgrounds` 존재 문서 수가 `0`
- `matchedCount == modifiedCount == 실행 전 대상 문서 수`
- max `updated_at`이 migration 로그의 `migrationStartedAt`으로 변경됨

migration 완료 후 다시 `GET /v1/pets/catalog`를 호출합니다.

- 응답 body의 `revision`이 활성 카탈로그의 새 max `updated_at`과 동일함
- 일반 응답에 `backgrounds`가 없음

필요하면 같은 확인값으로 migration을 다시 실행해 대상 0건인 멱등 상태를 확인합니다. 이때 `PET_CATALOG_MIGRATION_EXPECTED_TARGET=0`으로 변경합니다.

## 롤백 주의사항

- DB migration 전에는 애플리케이션 이미지만 구버전으로 롤백할 수 있습니다.
- DB migration 후 구버전 이미지를 먼저 배포하지 않습니다. 구버전의 non-null `backgrounds` 역직렬화가 실패할 수 있습니다.
- DB migration 후 구버전 롤백이 필요하면 먼저 백업에서 각 문서의 `backgrounds`와 기존 `updated_at`을 복구한 뒤 구버전 애플리케이션을 배포합니다.
- 기존 CDN/R2 배경 객체는 이 작업에서 삭제하지 않습니다.
