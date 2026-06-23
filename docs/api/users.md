# 사용자 API

모든 엔드포인트는 일반 사용자 access token이 필요합니다.

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/v1/users/{userId}` | 사용자 프로필과 친구·응원 상태를 조회합니다. |
| `GET` | `/v1/users/me` | 내 사용자 정보를 조회합니다. |
| `PUT` | `/v1/users/me` | 이름과 목표 칼로리를 수정합니다. |
| `DELETE` | `/v1/users/me` | 회원 탈퇴를 처리합니다. |
| `PATCH` | `/v1/users/me/daily-calorie` | 오늘 칼로리를 갱신하고 보상을 받습니다. |
| `POST` | `/v1/users/me/main-pet` | 메인 펫을 설정합니다. |
| `GET` | `/v1/users/me/main-pet` | 메인 펫을 조회합니다. |
| `PATCH` | `/v1/users/me/settings` | 앱 푸시 설정을 수정합니다. |

## 사용자 정보 수정

```json
{
  "name": "딴딴한 유저",
  "purposeCalorie": 500
}
```

- `name`은 빈 문자열일 수 없습니다.
- `purposeCalorie`는 `100..1000` 범위입니다.
- 오늘의 일일 기록이 있으면 표시 이름도 함께 갱신됩니다.

## 일일 칼로리

```json
{
  "calorie": 430
}
```

응답에는 갱신된 `user`, `dailyInfo`, 이번 요청으로 지급된 `rewardedFoodQuantity`, `rewardedToyQuantity`가 포함됩니다. 보상 수량은 클라이언트가 이전 재고와 비교해 계산하지 말고 응답 값을 사용합니다.

## 메인 펫

```json
{
  "petId": "507f1f77bcf86cd799439011"
}
```

- 사용자가 소유한 펫만 메인 펫으로 설정할 수 있습니다.
- 메인 펫이 없는 경우 조회 응답의 `mainPet`은 `null`일 수 있습니다.

## 주요 에러

- `UE001`: 사용자를 찾을 수 없음
- `PE001`: 펫을 찾을 수 없음
- `PE002`: 펫 소유자 불일치
- `DE0003`: 요청 값 오류
