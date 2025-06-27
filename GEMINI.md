# Gemini AI 어시스턴트를 위한 프로젝트 컨텍스트

## 프로젝트 개요

- **프로젝트명:** 딴딴 서버 (ddan-ddan-server)
- **설명:** 사용자의 활동(칼로리 소모)을 기반으로 펫을 육성하고 랭킹으로 경쟁하는 모바일 애플리케이션의 백엔드 서버입니다.
- **언어:** Kotlin
- **프레임워크:** Spring Boot
- **빌드 툴:** Gradle
- **데이터베이스:** MongoDB
- **인증:** JWT, OAuth 2.0 (카카오, 애플)

## 아키텍처

현재 클린 아키텍처를 적용하고 있습니다. 레이어는 다음과 같습니다.

- `presentation`: API 엔드포인트 (Controller, DTO)
- `application`: 비즈니스 로직 (Service, Processor)
- `domain`: 핵심 비즈니스 규칙 (Model, UseCase, Gateway 인터페이스)
- `infrastructure`: 외부 의존성 구현체 (DB Repository, 외부 API 클라이언트)

### 리팩토링 목표

현재 아키텍처는 과도하게 복잡하다고 판단되어 리팩토링이 필요합니다.

**핵심 목표:**
`domain` 레이어의 `UseCase`와 `Gateway` 인터페이스를 제거하고, `application` 레이어의 `Service`가 `infrastructure` 레이어의 `Gateway` 구현체(Repository 등)를 직접 호출하도록 구조를 단순화하는 것입니다.

이를 통해 불필요한 추상화 레이어를 줄이고 코드 추적 및 유지보수를 용이하게 만들 것으로 기대합니다.

## 주요 기능

- **사용자 인증:** OAuth (카카오, 애플) 로그인, JWT 토큰 발급/재발급
- **펫 관리:** 펫 추가, 먹이주기, 놀아주기 등
- **랭킹 시스템:** 기간별 랭킹 조회 및 보드 업데이트
- **사용자 정보:** 칼로리 업데이트, 메인 펫 설정
- **알림:** FCM을 이용한 푸시 알림

## 주요 디렉토리 구조

- **Controller, DTO:** `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation`
- **Service, Processor:** `src/main/kotlin/notbe/tmtm/ddanddanserver/application`
- **Model, UseCase, Gateway:** `src/main/kotlin/notbe/tmtm/ddanddanserver/domain`
- **Repository, 외부 API:** `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure`

## PR 생성 규칙

- PR 제목: `타입: 설명 (#이슈번호)` 형식
- PR 본문: `.github/pull_request_template.md` 템플릿 사용
- 작업 내용과 기타 사항을 명확하게 기재
