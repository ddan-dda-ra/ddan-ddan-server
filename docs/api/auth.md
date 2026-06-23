# 인증 API

## 엔드포인트

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `POST` | `/v1/auth/login` | 불필요 | Kakao 또는 Apple OAuth 토큰으로 로그인합니다. |
| `POST` | `/v1/auth/reissue` | 불필요 | refresh token으로 토큰을 재발급합니다. |

## 로그인

```json
{
  "token": "oauth-provider-token",
  "tokenType": "KAKAO",
  "deviceToken": "fcm-device-token"
}
```

- `tokenType`은 `KAKAO` 또는 `APPLE`입니다.
- `deviceToken`은 nullable입니다.
- 로컬·개발 환경에서만 `X-Mock-OAuth: true`를 사용할 수 있습니다. 지원하지 않는 환경에서는 `AC006`이 반환됩니다.
- 응답에는 `accessToken`, `refreshToken`, `user`, `isOnboardingComplete`가 포함됩니다.

## 토큰 재발급

```json
{
  "refreshToken": "refresh-token"
}
```

- 성공 시 access token과 refresh token을 모두 교체해 저장합니다.
- `AC004`는 refresh token 만료, `AC005`는 토큰 누락을 의미합니다.

## 클라이언트 처리

1. 로그인 또는 재발급 응답의 두 토큰을 함께 저장합니다.
2. 일반 API에는 access token을 Bearer 토큰으로 전송합니다.
3. `AC003` 수신 시 한 번만 재발급한 뒤 원 요청을 재시도합니다.
4. 재발급도 실패하면 저장된 인증 정보를 폐기하고 로그인 화면으로 이동합니다.
