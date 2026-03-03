# 입찰 원자성 제어 완화 분석

## 1. 목적
- 본 문서는 현재 코드 기준으로 "입찰 동시성/원자성 완화가 어디에 있는지"를 식별하고, 원자성 보장이 빠진 지점을 명확히 정리한다.
- 범위는 `WebSocket 입찰 경로`와 `프론트 입찰 UX 경로`로 한정한다.

## 2. 결론 요약
- 현재 코드에는 **세션/방 관리 수준의 동시성 완화**는 존재한다.
- 그러나 **입찰 금액 갱신의 원자성(atomic compare-and-set)** 은 구현되어 있지 않다.
- 따라서 경합 상황에서 `read-check-write` 경쟁 조건이 발생할 수 있으며, 테스트로 재현된다.

## 3. 현재 존재하는 완화 장치(As-Is)

| 구분 | 코드 위치 | 동작 | 한계 |
| --- | --- | --- | --- |
| WebSocket 방 자료구조 동시성 완화 | `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:66`, `:72`, `:73` | `ConcurrentHashMap` + `computeIfAbsent` + `CopyOnWriteArraySet`으로 접속 세션 집합을 스레드 안전하게 관리 | 입찰 가격 검증/저장 원자성과는 무관 |
| 트랜잭션 경계 | `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:79` | `handleTextMessage`를 트랜잭션으로 묶어 DB 반영 단위를 보장 | 동시 요청 간 직렬화/잠금/CAS는 아님 |
| 서버 유효성 검증 | `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:123`, `:130`, `:151`, `:157` | 종료 경매 차단, 본인 경매 차단, 최고입찰자 재입찰 차단, 현재가 초과 검증 | 각각의 요청 시점 기준 검증이라 경합 시 동시 통과 가능 |
| 알림 중복 완화 | `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:179`, `:203`, `:257` | 중복 알림을 soft-delete 후 재생성 | 알림 중복 완화일 뿐 입찰 원자성 보장 아님 |
| 프론트 UX 완화 | `frontend/src/pages/auctions/AuctionDetailPage.jsx:303`, `:306`, `:379` | 자동 입찰가 상향, 로컬 검증, 버튼 제출 | 클라이언트 로컬 상태 기반이라 다중 사용자 동시 제출을 막지 못함 |

## 4. 원자성 취약 지점(핵심 경쟁 구간)

아래 구간이 현재 입찰 경합의 핵심이다.

1. 경매 스냅샷 조회: `auctionRepository.findByIdWithImages(...)`
   - `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:116`
2. 최고 입찰 조회: `redisBidRepository.findByAuctionIdOrderByBidAmountDesc(...)`
   - `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:139`
3. 현재가 비교 검증: `bidAmount <= auction.getCurrentPrice()`
   - `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:157`
4. 입찰 저장: `redisBidRepository.save(newBid)`
   - `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:171`
5. 현재가 갱신 + 저장: `auction.updateCurrentPrice(...)` + `auctionRepository.save(...)`
   - `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:174`, `:175`

정리하면, 현재 구조는 `조회 -> 검증 -> 저장` 순서이며, 이 전체가 "경매 단위 단일 원자 연산"으로 보호되지 않는다.

## 5. 재현 테스트 근거

| 시나리오 | 테스트 위치 | 확인 내용 |
| --- | --- | --- |
| 서로 다른 사용자 동시 입찰 | `backend/src/test/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandlerConcurrencyCurrentBehaviorTest.java:33` | 저장 순서가 뒤집히면 낮은 금액이 최종 `currentPrice`를 덮어쓸 수 있음을 재현 |
| 같은 사용자 동시 입찰 | `backend/src/test/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandlerConcurrencyCurrentBehaviorTest.java:117` | 동일 사용자 요청 2건이 모두 승인/저장될 수 있음을 재현 |
| 개선 후 기대 동작(현재 비활성) | `backend/src/test/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandlerConcurrencyExpectedBehaviorTest.java:31` | 원자성/중복제어 적용 후 통과해야 하는 목표 테스트를 정의 |

## 6. 원자성 제어를 적용해야 할 정확한 지점(설계 관점)

### 6.1 서버 입찰 진입점
- 대상: `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:128` 이후 `bid` 분기
- 이유: 이 분기에서 가격 검증/저장/현재가 갱신이 모두 수행되므로, 원자성 게이트를 가장 먼저 걸어야 한다.

### 6.2 원자 갱신 지점
- 대상: 현재가 비교 및 갱신 구간
  - 비교: `:157`
  - 갱신: `:174`, `:175`
- 요구: "현재가가 내가 본 값과 같거나 더 낮을 때만 갱신"되는 CAS 성질 필요

### 6.3 중복 제출 제어 지점
- 대상: 동일 사용자/동일 경매의 짧은 시간 중복 제출
  - 현재 제출 진입: `frontend/src/pages/auctions/AuctionDetailPage.jsx:379`
  - 서버 저장: `backend/src/main/java/com/inhatc/auction/domain/bid/websocket/WebSocketHandler.java:171`
- 요구: 서버 기준 idempotency key(또는 user+auction+requestId dedup)로 1회성 처리

## 7. 참고
- 본 문서는 As-Is 분석 문서이며, 코드 수정은 포함하지 않는다.

## 8. 필요한 개선점(권장)

### 8.1 우선순위별 개선 항목

| 우선순위 | 개선 항목 | 적용 위치 | 구현 방향 | 기대 효과 |
| --- | --- | --- | --- | --- |
| P0 | 경매 단위 원자 게이트 도입 | `WebSocketHandler`의 `bid` 분기 시작부 | 경매 ID 기준 단일 진입 보장(예: 분산락/DB락/CAS) | 동시 입찰 경합 시 이중 승인 차단 |
| P0 | 현재가 CAS(조건부 갱신) 적용 | `auction.updateCurrentPrice` + `auctionRepository.save` 구간 | `현재가 < 입찰가` 조건에서만 갱신 성공 처리, 실패 시 충돌 응답 | `last-write-wins`로 인한 가격 역전 방지 |
| P0 | 동일 요청 중복 처리(idempotency) | WebSocket 요청 DTO + 서버 저장 전 단계 | `requestId` 기반 dedup 키(user+auction+requestId) 저장/검사 | 같은 사용자 동시/재전송 중복 승인 방지 |
| P0 | 저장 순서 재정렬 | `redisBidRepository.save` 호출 시점 | 원자 게이트 통과/가격 갱신 성공 후 입찰 이력 저장 | 부분 반영(이력만 저장, 현재가 실패) 불일치 감소 |
| P1 | bid vs buy-now 경쟁 상태 정리 | `bid`/`buy-now` 분기 공통 | `ACTIVE -> ENDED` 상태 전이 조건부 업데이트(1회만 성공) | 종료 직전 동시 요청의 상태 찢김 방지 |
| P1 | 충돌 응답 규약 강화 | WebSocket 응답 DTO | `409(CONFLICT)` + 최신 `currentPrice`, `auctionLeftTime` 반환 | 클라이언트 즉시 재동기화 가능 |
| P1 | 프론트 in-flight 제출 잠금 | `AuctionDetailPage`의 `handleBid` | 요청 전송 후 응답 수신 전까지 버튼 잠금/중복 submit 차단 | 동일 사용자의 순간 다중 클릭 완화 |
| P1 | 원자성 관측 지표 추가 | Actuator/Prometheus | 입찰 충돌 횟수, dedup hit, lock wait/timeout 메트릭 수집 | 운영 중 병목/오류 조기 탐지 |
| P2 | 경합 부하 테스트 상시화 | `k6-test` + JUnit 동시성 테스트 | CI에서 취약점 재현/기대 동작 테스트 분리 실행 | 회귀 방지 |

### 8.2 최소 구현 순서(현실적 도입안)

1. 서버 `bid` 경로에 CAS 기반 조건부 갱신을 먼저 도입한다.
2. 갱신 성공 시에만 입찰 이력 저장 및 브로드캐스트를 수행하도록 순서를 조정한다.
3. WebSocket 요청에 `requestId`를 추가하고 dedup 저장소(예: Redis NX)를 붙인다.
4. `ExpectedBehavior` 테스트를 활성화하여 CI 필수 게이트로 설정한다.
5. 프론트 `입찰하기` 버튼에 in-flight 잠금을 추가한다.

### 8.3 완료 기준(DoD)

- 동시 입찰 시 최종 `currentPrice`가 항상 최고 승인 금액으로 유지된다.
- 동일 사용자 동시 요청에서 1건만 승인된다.
- 충돌/중복 거절 응답이 클라이언트에서 즉시 최신가로 반영된다.
- `WebSocketHandlerConcurrencyExpectedBehaviorTest`가 활성화 상태로 안정 통과한다.
