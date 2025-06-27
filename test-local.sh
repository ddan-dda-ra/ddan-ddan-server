#!/bin/bash

echo "=== GitHub Actions 로컬 테스트 스크립트 ==="
echo ""

# act 설치 확인
if ! command -v act &> /dev/null; then
    echo "❌ act가 설치되지 않았습니다. 설치해주세요:"
    echo "brew install act"
    exit 1
fi

echo "✅ act 버전: $(act --version)"
echo ""

# 사용 가능한 워크플로우 목록 출력
echo "📋 사용 가능한 워크플로우:"
act -l
echo ""

# 메뉴 선택
echo "테스트할 워크플로우를 선택하세요:"
echo "1) PR 테스트 (pr-test.yml)"
echo "2) 로컬 테스트 (act-local-test.yml)"
echo "3) ktlint 검사 (ktlint-check.yml)"
echo "4) 모든 워크플로우 목록 보기"
echo ""

read -p "선택 (1-4): " choice

case $choice in
    1)
        echo "🚀 PR 테스트 워크플로우 실행중..."
        act pull_request -W .github/workflows/pr-test.yml
        ;;
    2)
        echo "🚀 로컬 테스트 워크플로우 실행중..."
        act workflow_dispatch -W .github/workflows/act-local-test.yml
        ;;
    3)
        echo "🚀 ktlint 검사 워크플로우 실행중..."
        act pull_request -W .github/workflows/ktlint-check.yml
        ;;
    4)
        echo "📝 모든 워크플로우:"
        act -l
        ;;
    *)
        echo "❌ 잘못된 선택입니다."
        exit 1
        ;;
esac

echo ""
echo "✨ 완료!"