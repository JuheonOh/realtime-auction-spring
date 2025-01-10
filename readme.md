# 🔨 실시간 경매 시스템

## 📖 프로젝트 소개

실시간 입찰과 알림 기능을 제공하는 온라인 경매 플랫폼입니다. WebSocket을 활용한 실시간 입찰 시스템과 SSE를 통한 실시간 알림으로 사용자들에게 역동적인 경매 경험을 제공합니다.

## ⏰ 개발 기간

- 23.12.01일 - 24.03.01일

## ⚙ 개발 환경

### Frontend

- **Framework**: `React 18.2.0`
- **상태관리**: `Redux Toolkit 2.0.1`
- **스타일링**: `TailwindCSS 3.4.0`
- **차트**: `Chart.js 4.4.1`
- **HTTP 클라이언트**: `Axios 1.6.2`

### Backend

- **Framework**: `Spring Boot 3.2.0`
- **보안**: `Spring Security 6.2.0`
- **ORM**: `Spring Data JPA 3.2.0`
- **실시간 통신**: `WebSocket`, `SSE`
- **캐시**: `Redis 7.2`
- **데이터베이스**: `MariaDB 10.11`

## 📋 시스템 아키텍처

<p align="center">
  <img src="path/to/architecture/image.png" width="80%">
</p>

```
Frontend (React) <--> API Gateway
                     |
                     |--> Auth Service
                     |--> Auction Service
                     |--> Notification Service
                     |
                     |--> Database (MariaDB)
                     |--> Cache (Redis)
                     |--> Message Queue
```

## ⌨ E-R 다이어그램

<p align="center">
  <img src="path/to/er-diagram.png" width="80%">
</p>

## 🔍 주요 기능

### 실시간 경매

- WebSocket을 활용한 실시간 입찰 시스템
- 입찰 현황 실시간 차트 시각화
- 자동 입찰 기능
- 즉시 구매 옵션
- 경매 시간 자동 연장 시스템

### 실시간 알림

- SSE를 활용한 실시간 알림 시스템
- 입찰, 낙찰, 경매 종료 알림
- 새로운 경매 등록 알림
- 관심 상품 가격 변동 알림

### 사용자 기능

- JWT 기반 인증/인가
- OAuth2.0 소셜 로그인
- 관심 경매 등록/관리
- 입찰 내역 조회
- 낙찰 내역 관리

### 관리자 기능

- 경매 상품 관리
- 사용자 관리
- 입찰 현황 모니터링
- 통계 대시보드

## 🖥 스크린샷

### 메인 페이지

<img src="path/to/main-page.png" alt="메인 페이지">

### 경매 상세

<img src="path/to/auction-detail.png" alt="경매 상세">

### 실시간 입찰

<img src="path/to/real-time-bidding.png" alt="실시간 입찰">

### 관리자 대시보드

<img src="path/to/admin-dashboard.png" alt="관리자 대시보드">

## 🔄 API 명세

### 인증 API

```
POST /api/auth/login     - 로그인
POST /api/auth/signup    - 회원가입
POST /api/auth/refresh   - 토큰 갱신
GET  /api/auth/logout    - 로그아웃
```

### 경매 API

```
GET    /api/auctions          - 경매 목록 조회
POST   /api/auctions          - 경매 등록
GET    /api/auctions/{id}     - 경매 상세 조회
POST   /api/auctions/{id}/bid - 입찰
DELETE /api/auctions/{id}     - 경매 삭제
```

### 알림 API

```
GET    /api/notifications          - 알림 목록 조회
PATCH  /api/notifications/{id}     - 알림 읽음 처리
DELETE /api/notifications/{id}     - 알림 삭제
```
