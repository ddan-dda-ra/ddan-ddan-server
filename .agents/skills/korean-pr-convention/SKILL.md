---
name: korean-pr-convention
description: ddan-ddan-server의 한국어 이슈/브랜치/PR 컨벤션. 이슈 제목 형식 [작업타입] 설명, 브랜치명 타입/#번호-설명, PR 제목 타입 설명, PR 본문 템플릿(작업 내용/기타/close 이슈) 패턴을 다룬다. 이슈 생성, 브랜치 생성, PR 생성 시 반드시 참조 — 팀 컨벤션 위반은 PR이 즉시 반려되는 사유다.
---

# Korean PR Convention (ddan-ddan-server)

## 작업 타입

| 타입 | 용도 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `refactor` | 동작 변경 없는 코드 정리 |
| `fix` | 버그 수정 |
| `bug` | 버그 수정 (`fix`와 혼용 — 최근 커밋 기준) |
| `docs` | 문서/주석 수정 |
| `chore` | 빌드, 의존성, CI/CD, 설정 |

## 이슈

### 제목

```
[작업타입] 간단한 한국어 설명
```

- 예: `[feat] 응원하기 API 추가`
- 예: `[refactor] Ranking 기능 레이어드 아키텍처로 변경`
- 예: `[fix] 초대수락자 이름이 뜨도록 수정`

### 본문

작업 목적과 세부사항을 한국어로 기재. 길이 자유.

### 명령

```bash
gh issue create --title "[chore] Codex 하네스 구성" --body "..."
```

## 브랜치

### 명명 규칙

```
{타입}/#{이슈번호}-{간단한-영어-설명}
```

- 예: `feature/#190-ranking-refactor-to-layered`
- 예: `fix/#261-invite-acceptor-name`
- 예: `chore/#194-Codex-harness-setup`

**주의:** 디렉토리 깊이 표현으로 `/`가 들어간다. `git checkout -b feature/#NNN-...`로 만들 수 있다 (zsh 기준 따옴표 불필요하지만, `#`는 코멘트 문자이므로 인용 권장).

```bash
git checkout -b "chore/#194-Codex-harness-setup"
```

### 타입 → 브랜치 prefix 매핑

| 작업 타입 | 브랜치 prefix |
|---|---|
| feat | `feature/` |
| refactor | `refactor/` |
| fix / bug | `fix/` |
| docs | `docs/` |
| chore | `chore/` |

> 브랜치 prefix와 PR 타입이 살짝 다름에 유의 (`feat` ↔ `feature/`).

## PR

### 타겟 브랜치

항상 `develop`. main/master 직접 PR 금지.

### 제목

```
{타입}: {한국어 설명}
```

- 예: `feat: 초대코드로 초대자 정보 조회 API 추가`
- 예: `refactor: 응원하기 API 성능 개선`
- 예: `chore: Codex 하네스 구성`

**주의:** 제목 끝에 이슈 번호 (#N) 붙이지 않는다. 이슈 연결은 본문의 `close #N`으로 처리.

### 본문 템플릿

`.github/pull_request_template.md`를 따른다:

```markdown
## 🔎 작업 내용
- 항목 1
- 항목 2

## ➕ 기타
- 추가 컨텍스트, 스크린샷, 리뷰어가 알아야 할 점

close #이슈번호
```

### 명령

```bash
gh pr create --title "chore: Codex 하네스 구성" --body "$(cat <<'EOF'
## 🔎 작업 내용
- ...

## ➕ 기타
- ...

close #194
EOF
)"
```

## 표준 워크플로우

```bash
# 1. 이슈 생성
gh issue create --title "[chore] Codex 하네스 구성" --body "..."
# → 이슈 번호 N 확인

# 2. 브랜치 생성 (develop 최신화 후)
git checkout develop
git pull
git checkout -b "chore/#N-Codex-harness-setup"

# 3. 작업 + 커밋
git add ...
git commit -m "chore: Codex 하네스 구성"

# 4. push
git push -u origin "chore/#N-Codex-harness-setup"

# 5. PR
gh pr create --title "chore: Codex 하네스 구성" --body "..."
```

## 자주 하는 실수

| 실수 | 올바른 형태 |
|---|---|
| PR 제목 끝에 `(#194)` | 본문에 `close #194` |
| 브랜치를 `feat/...`로 시작 | `feature/...` |
| 이슈 제목에 `feat:` (콜론) | `[feat]` (대괄호) |
| PR을 main으로 | develop으로 |
| 영어 본문 | 한국어 본문 |
