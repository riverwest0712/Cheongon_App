# 소셜 로그인 설정

`local.properties`에 아래 값을 추가합니다. 이 파일은 Git에 커밋하지 않습니다.

```properties
GOOGLE_WEB_CLIENT_ID=웹_클라이언트_ID.apps.googleusercontent.com
SOCIAL_LOGIN_MOCK=false
```

키 없이 개발할 때는 `SOCIAL_LOGIN_MOCK=true`로 설정합니다. 값이 없을 때도 기본값은
`true`입니다. 이 모드의 Google·네이버·카카오 버튼은 실제 제공자나 서버에 연결하지 않고
로컬 테스트 사용자로 로그인합니다. 실제 배포 또는 인증 테스트 전에는 반드시 `false`로
변경하고 Google 웹 클라이언트 ID를 설정해야 합니다.

## 제공자 콘솔

- Google Cloud: Android OAuth 클라이언트에 패키지 `com.example.cheongon`과 앱 서명 SHA-1을 등록하고, 같은 프로젝트의 **웹 클라이언트 ID**를 위 값에 넣습니다.

## 백엔드 계약

앱은 `POST /api/auth/social`에 아래 중 하나를 보냅니다.

```json
{ "provider": "google", "id_token": "..." }
```

서버는 토큰을 해당 제공자의 서버에서 검증해야 합니다. 검증된 제공자 사용자 ID로 기존 회원을 찾고, 없으면 신규 회원을 만든 뒤 다음 형식으로 응답합니다.

```json
{
  "user": {
    "id": 1,
    "user_id": "표시 이름",
    "email": "user@example.com"
  }
}
```

클라이언트가 전달한 이메일이나 사용자 ID를 토큰 검증 없이 신뢰하면 안 됩니다.
