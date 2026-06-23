---
name: ddan-ddan-feature-dev
description: ddan-ddan-server의 기능 개발/버그 수정/리팩토링 워크플로우 오케스트레이터. 이슈 분석 → 설계 → 구현 → 테스트·API 문서 → 리뷰 → PR 생성을 6개 전문 에이전트(issue-analyzer, backend-architect, kotlin-spring-implementer, test-engineer, api-documenter, code-reviewer)가 협업하여 자동화한다. 다음 표현이 등장하면 반드시 트리거할 것 - "기능 추가", "API 만들어줘", "이슈 #N 작업", "이슈 따서 작업", "버그 수정", "fix해줘", "리팩토링", "PR 만들어줘", "PR 올려줘", "다시 실행", "재실행", "리뷰만 다시", "보완". 단, 한 줄 수정이나 단순 코드 질문은 직접 처리.
---

# ddan-ddan-feature-dev — 오케스트레이터

이슈 → 설계 → 구현 → 테스트·API 문서 → 리뷰 → PR을 자동화하는 워크플로우. 6개 에이전트가 파일 기반 핸드오프(`_workspace/`)로 협업한다.

## 실행 모드

**서브 에이전트 + 부분 검증 팀 (하이브리드).**

- Phase 1~5는 서브 에이전트 — 각 에이전트를 `Agent` 도구로 호출, `model: "opus"` 명시.
- Phase 4의 `test-engineer`와 `api-documenter`는 Phase 3 완료 후 서로 독립적으로 병렬 실행한다.
- Phase 6(PR 생성)은 오케스트레이터가 직접 수행 (사용자 확인 후).
- 모든 Agent 호출에 **`model: "opus"`**를 명시한다.

## Phase 0 — 컨텍스트 확인

워크플로우 진입 시 가장 먼저 실행 모드를 결정한다.

1. `_workspace/` 디렉토리 존재 여부 확인:
   - **미존재** → 초기 실행. `_workspace/`를 생성하고 Phase 1부터 진행.
   - **존재** + 사용자가 새 이슈/요구사항 제시 → 새 실행. 기존 `_workspace/`를 `_workspace_prev/`로 이동 후 Phase 1부터.
   - **존재** + 사용자가 부분 수정 요청 ("리뷰만 다시", "테스트만 보완", "구현 다시") → 부분 재실행. 해당 Phase만 호출.
2. 사용자에게 결정 사항을 한 줄로 보고한 뒤 진행.

```
_workspace/
├── 01_issue_analysis.md
├── 02_design.md
├── 03_implementation.md
├── 04_test_summary.md
├── 04_api_docs.md
└── 05_review.md
```

## Phase 1 — 이슈 분석

**에이전트:** `issue-analyzer`
**입력:** 사용자가 제시한 이슈 번호 또는 요구사항 텍스트
**출력:** `_workspace/01_issue_analysis.md`

```
Agent(
  subagent_type: "issue-analyzer",
  model: "opus",
  description: "이슈 분석",
  prompt: "다음 이슈/요구사항을 분석하라:\n\n{사용자 입력}\n\n_workspace/01_issue_analysis.md에 결과를 작성하라."
)
```

**진행 조건:** 분석 결과의 "미해결 질문"이 비어있을 것. 비어있지 않으면 사용자에게 질문하고 응답 후 재실행.

## Phase 2 — 설계

**에이전트:** `backend-architect`
**입력:** `_workspace/01_issue_analysis.md`
**출력:** `_workspace/02_design.md`

```
Agent(
  subagent_type: "backend-architect",
  model: "opus",
  description: "Layered 아키텍처 설계",
  prompt: "_workspace/01_issue_analysis.md를 읽고 layered-architecture-guide 스킬을 따라 설계서를 작성하라. 결과는 _workspace/02_design.md에."
)
```

**사용자 확인:** 설계서 핵심(API 명세, 변경 파일 목록)을 사용자에게 요약 보고 후, 진행 동의를 받는다. 동의 없이 Phase 3로 넘어가지 않는다.

## Phase 3 — 구현

**에이전트:** `kotlin-spring-implementer`
**입력:** `_workspace/02_design.md`
**출력:** 코드 변경 + `_workspace/03_implementation.md`

```
Agent(
  subagent_type: "kotlin-spring-implementer",
  model: "opus",
  description: "Kotlin Spring 구현",
  prompt: "_workspace/02_design.md의 설계대로 코드를 구현하라. layered-architecture-guide와 kotlin-spring-conventions 스킬을 반드시 따른다. 완료 후 ./gradlew compileKotlin으로 검증하고 _workspace/03_implementation.md를 작성하라."
)
```

**진행 조건:** `compileKotlin` ✅. 실패 시 1회 재시도, 재실패 시 사용자에게 보고하고 중단.

## Phase 4 — 테스트 + API 문서 (병렬)

Phase 3의 구현과 `compileKotlin` 검증이 끝나면 아래 두 에이전트를 동시에 호출한다.

### Phase 4-A — 테스트

**에이전트:** `test-engineer`
**입력:** `_workspace/03_implementation.md` + 변경된 코드
**출력:** 테스트 코드 + `_workspace/04_test_summary.md`

```
Agent(
  subagent_type: "test-engineer",
  model: "opus",
  description: "Kotest 테스트 작성",
  prompt: "_workspace/03_implementation.md를 읽고 새로 추가/수정된 service, controller, 도메인 모델에 대한 Kotest FunSpec + MockK 테스트를 작성하라. kotest-testing-patterns 스킬을 따른다. ./gradlew test로 검증하고 _workspace/04_test_summary.md를 작성하라."
)
```

**진행 조건:** 추가한 테스트가 모두 통과. 실패 시 원인이 production 코드면 implementer에게 메시지(03_implementation.md의 "재실행 변경 이력" 섹션에 기록 후 implementer 재호출), 테스트 자체 결함이면 test-engineer가 직접 수정.

### Phase 4-B — API 문서

**에이전트:** `api-documenter`
**입력:** `_workspace/02_design.md`, `_workspace/03_implementation.md`, `git diff develop...HEAD`, 변경된 controller·DTO·보안·예외 코드
**출력:** API 문서 변경 + `_workspace/04_api_docs.md`

```
Agent(
  subagent_type: "api-documenter",
  model: "opus",
  description: "API 계약 문서 갱신",
  prompt: "_workspace/02_design.md와 03_implementation.md, develop과의 diff를 읽고 API 계약 변경을 판별하라. 변경이 있으면 SpringDoc OpenAPI 스냅샷과 관련 docs/api 도메인 문서를 갱신하고, 없으면 문서를 수정하지 말고 근거를 남겨라. 결과는 _workspace/04_api_docs.md에 작성하라."
)
```

**진행 조건:**
- API 변경 있음 → `docs/api/openapi.yaml`과 관련 도메인 문서가 실제 코드와 일치하고 검증이 통과할 것.
- API 변경 없음 → `_workspace/04_api_docs.md`에 “문서 변경 불필요”와 controller·DTO·인증·예외 계약이 바뀌지 않았다는 근거가 있을 것.
- 문서 검증 실패 또는 API 변경 누락 시 1회 재호출. 재실패 시 사용자에게 보고하고 중단.

## Phase 5 — 리뷰

**에이전트:** `code-reviewer`
**입력:** `_workspace/02_design.md`, `03_implementation.md`, `04_test_summary.md`, `04_api_docs.md` + `git diff develop...HEAD`
**출력:** `_workspace/05_review.md`

```
Agent(
  subagent_type: "code-reviewer",
  model: "opus",
  description: "코드 리뷰",
  prompt: "develop과의 diff와 _workspace/02_design.md, 03_implementation.md, 04_test_summary.md, 04_api_docs.md를 검토하라. 코드·테스트 체크리스트와 함께 API 변경이 OpenAPI 및 관련 도메인 문서에 반영됐는지 검사한다. 결과는 _workspace/05_review.md."
)
```

**분기:**
- 🛑 Critical → implementer에게 수정 요청 (재호출). 1회 재시도까지. 재시도 후에도 Critical 잔존 시 사용자에게 보고하고 중단.
- ⚠️ Major → 사용자에게 보고하고 진행 여부 확인.
- ✅ 승인 → Phase 6 진행.

## Phase 6 — PR 생성

**실행 주체:** 오케스트레이터 (직접)
**참조 스킬:** `korean-pr-convention`

순서:
1. **사용자에게 PR 생성 동의 확인**.
2. 이슈가 아직 없으면 `gh issue create`로 생성.
3. 새 브랜치 생성: `git checkout -b "{prefix}/#{N}-{설명}"`. (prefix: feat→feature/, refactor→refactor/, fix→fix/, chore→chore/, docs→docs/)
4. 변경 파일 stage + commit. 커밋 메시지는 `{타입}: {설명}` (한국어).
5. push: `git push -u origin "{브랜치}"`.
6. `gh pr create --title "{타입}: {설명}"` + 본문(`pull_request_template.md` 형식, `close #N` 포함).
7. 생성된 PR URL을 사용자에게 보고.

## 데이터 전달 프로토콜

- **파일 기반** (주): `_workspace/{phase번호}_{agent}_{artifact}.md`. 각 에이전트는 자기 출력만 쓰고, 이전 단계 출력을 읽는다.
- **메시지** (보조): 에이전트가 이전 산출물 보강이 필요할 때, 오케스트레이터를 통해 해당 에이전트를 재호출. 직접 호출 금지.

## 에러 핸들링

| 상황 | 대응 |
|---|---|
| 에이전트가 산출물 파일을 안 만듦 | 1회 재시도. 재실패 시 사용자에게 보고 + 중단. |
| compileKotlin 실패 | implementer가 1회 자가 수정. 재실패 시 보고 + 중단. |
| 테스트 실패 (production 결함) | implementer 재호출. 1회 재시도. |
| 테스트 실패 (테스트 결함) | test-engineer가 직접 수정. |
| API 문서 산출물 누락/검증 실패 | api-documenter 1회 재호출. 재실패 시 보고 + 중단. |
| API 변경인데 문서 미갱신 | api-documenter 재호출 후 reviewer 재검토. |
| 리뷰에서 Critical 잔존 | implementer 재호출. 1회 재시도. 그래도 잔존 시 사용자 결정. |
| 사용자가 Phase 도중 중단 요청 | 즉시 중단. `_workspace/`는 보존 (재개 가능). |

## 테스트 시나리오

### 시나리오 1: 정상 흐름

> "이슈 #200 따서 작업해줘"

1. Phase 0 — `_workspace/` 미존재 → 신규 생성
2. Phase 1 — issue-analyzer가 #200 분석, 미해결 질문 없음
3. Phase 2 — backend-architect 설계서 작성, 사용자 OK
4. Phase 3 — implementer 구현, compileKotlin ✅
5. Phase 4 — test-engineer 테스트 작성 + api-documenter 문서 갱신을 병렬 실행
6. Phase 5 — reviewer ✅ 승인
7. Phase 6 — 사용자 OK 후 브랜치/커밋/PR 생성 → URL 반환

### 시나리오 2: Critical 발견 → 재시도

> Phase 5에서 reviewer가 "domain이 infrastructure를 import" Critical 지적

1. 오케스트레이터가 implementer를 재호출, 5_review.md를 입력으로 전달
2. implementer가 위반 코드 수정 + 03_implementation.md 갱신
3. test-engineer와 api-documenter 재호출 (각각 영향받는 범위만)
4. reviewer 재호출 — Critical 사라지면 ✅ → Phase 6
5. 1회 재시도 후에도 잔존하면 중단 + 사용자 보고

### 시나리오 3: 부분 재실행

> "리뷰만 다시 해줘"

1. Phase 0 — `_workspace/` 존재, 사용자 의도 = 부분 재실행
2. Phase 5만 실행 (reviewer 단독 호출)
3. 결과에 따라 Phase 6로 진행하거나 사용자에게 보고

### 시나리오 4: API 변경 문서 누락

> DTO 필드가 추가됐지만 OpenAPI 스냅샷과 도메인 문서가 바뀌지 않음

1. Phase 4-B에서 api-documenter가 DTO diff를 API 계약 변경으로 판정
2. `docs/api/openapi.yaml`과 관련 도메인 문서를 갱신하고 `04_api_docs.md`에 기록
3. Phase 5 reviewer가 코드 DTO와 OpenAPI 필드, 인증, 상태 코드, Markdown 업무 규칙을 대조
4. 문서가 여전히 누락됐으면 ⚠️ Major 이상으로 차단하고 api-documenter를 재호출

### 시나리오 5: 내부 구현만 변경

> repository 조회 방식만 변경되고 controller·DTO·인증·예외 계약은 동일

1. Phase 4-B에서 API 변경 없음으로 판정
2. 문서 파일은 수정하지 않음
3. `04_api_docs.md`에 “문서 변경 불필요”와 판단 근거 기록
4. reviewer가 실제 diff와 판정이 일치하는지 확인

## 트리거 (description 보강)

이 스킬은 다음 상황에서 반드시 트리거한다:

- 기능 추가 요청: "기능 추가", "API 만들어줘", "엔드포인트 추가"
- 이슈 작업: "이슈 #N", "이슈 따서", "이슈 작업"
- 버그/수정: "버그 수정", "fix해줘", "수정해줘" (단, 1~2줄 수정은 직접)
- 리팩토링: "리팩토링", "정리해줘"
- PR: "PR 만들어줘", "PR 올려줘"
- 후속: "다시 실행", "재실행", "Phase {N}만 다시", "리뷰만 다시", "보완"

**트리거하지 않을 것:**
- 단순 질문 ("이 코드는 뭐 하는 거야?", "Spring Boot 버전 알려줘")
- 한 줄짜리 명백한 수정 ("typo 고쳐줘")
- 비-기능 작업 (의존 업데이트, 단순 설정 변경) — 이건 직접 처리 + 필요 시 PR만

## 테스트 시나리오 외 — 후속 작업 키워드

description의 후속 작업 키워드(다시 실행, 재실행, 리뷰만 다시, 보완)는 Phase 0의 부분 재실행 분기로 진입한다. 사용자에게 "어떤 Phase를 다시 실행할까요?"를 물을 필요는 없다 — 표현에서 추론한다 ("리뷰만" → Phase 5, "테스트 보완" → Phase 4, "다시 실행" → 전체 새로 시작).
