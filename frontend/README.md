# Frontend

React 18과 Vite 기반 경매 클라이언트입니다.

## Requirements

- Node.js 22
- npm

## Environment

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=ws://localhost:8080
```

운영 환경에서는 HTTPS API와 WSS WebSocket 주소를 사용해야 합니다.

## Commands

```bash
npm ci
npm start       # Vite development server on port 80
npm test        # Vitest regression tests
npm run build   # production output in build/
npm run preview
```

Refresh Token은 백엔드가 발급하는 HttpOnly 쿠키에만 저장됩니다. Access Token은 Redux 메모리 상태에만 유지되며, 상태 변경 요청은 먼저 `/api/auth/csrf`에서 CSRF 쿠키를 초기화합니다.
