# 백엔드 8주차 워크북 작성용

이 문서는 작성 예시입니다. 직접 실행·촬영한 결과와 자동 테스트 결과를 구분하고, 본인의 확인 결과에 맞춰 작성하세요. 선택미션 Swagger는 구현하지 않았습니다.

## 핵심 키워드

- **트랜잭션:** 여러 DB 작업을 하나의 처리 단위로 묶는 것. 영화 기록에서는 평점과 즐겨찾기 저장을 묶는다.
- **commit:** 트랜잭션의 변경을 최종 반영한다. 여기서는 서비스가 정상 종료될 때 수행된다.
- **rollback:** 실패한 트랜잭션의 변경을 취소한다. Spring의 기본 @Transactional은 밖으로 전파된 RuntimeException/Error에 대해 롤백한다.
- **원자성(Atomicity):** 여러 작업이 모두 성공하거나 모두 취소되어 일부만 남지 않는 성질.
- **ACID:** 원자성, 일관성(제약을 지키는 상태 유지), 격리성(동시 트랜잭션 간 간섭 제어), 지속성(commit한 결과의 보존).
- **UNIQUE 제약:** DB에서 중복을 막는다. `(member_id, movie_id)`를 묶어 같은 회원의 같은 영화만 중복 금지한다. 다른 회원은 같은 영화를 등록할 수 있다.
- **낙관적 잠금:** 버전 값을 비교해 다른 트랜잭션의 수정과 충돌했는지 감지하는 방식. JPA의 @Version으로 적용할 수 있다. 이번 필수미션에서는 구현하지 않았으며, 중복 등록은 UNIQUE로 처리했다.

## 학습 후기 질문

**1. 트랜잭션 없이 평점만 저장되면 왜 문제인가요?**

사용자는 평점과 즐겨찾기를 함께 저장하도록 요청했는데 일부만 반영되어 의도와 실제 데이터가 달라진다. 하나의 트랜잭션으로 묶으면 중간 실패 시 먼저 저장한 평점도 취소할 수 있다.

**2. 사전 검사와 DB UNIQUE를 함께 두는 이유는 무엇인가요?**

사전 검사는 이미 등록한 경우 명확한 오류를 빠르게 알려 주기 위한 것이다. 두 동시 요청이 모두 사전 검사를 통과할 수 있으므로 DB UNIQUE도 필요하며, DB 충돌도 같은 FAVORITE409_1 코드로 처리한다.

**2줄 후기 예시**

즐겨찾기를 구현하면서 토큰으로 회원을 식별하고 회원별 데이터를 분리하는 흐름을 이해했다.
평점과 즐겨찾기를 하나의 트랜잭션으로 저장하며, 사전 중복 검사와 DB UNIQUE 제약이 각각 필요한 이유를 배웠다.

## 실습 체크리스트 기록

| 항목 | 구현 위치 | 직접 확인할 증거 |
| --- | --- | --- |
| Favorite가 BaseTimeEntity 상속 | entity/Favorite.java | 엔티티 코드 캡처 |
| 토큰의 회원 사용, memberId 요청 없음 | FavoriteController, MovieRecordController | RequestAttribute와 Postman URL/Body |
| 중복 409 FAVORITE409_1 | FavoriteService, GlobalExceptionHandler | Postman 08 응답 |
| 내 목록 최신순 | FavoriteRepository | Postman 21 응답 (550 → 238) |
| 등록 전후 false → true | FavoriteService.getStatus | Postman 06/09 응답 |
| 해제 200 공통 응답 | FavoriteController.delete | Postman 13, result null |
| 두 저장의 단일 트랜잭션 | MovieRecordService.create | @Transactional과 두 저장 코드, Postman 16 |

## 필수미션 기록 템플릿

- 기준 코드: back_spring의 week-7, 커밋 406ac51.
- 구현 내용: 즐겨찾기 등록·목록·여부·해제와 영화 기록 저장 API를 추가했다.
- 회원 식별: 검증된 JWT에서 꺼낸 회원 ID를 @RequestAttribute로 전달받으며 요청 DTO에는 memberId가 없다.
- 중복 방지: 사전 exists 검사와 DB의 (member_id, movie_id) UNIQUE 제약을 함께 사용했다.
- 트랜잭션: MovieRecordService.create의 @Transactional 안에서 평점과 즐겨찾기를 저장한다. addToFavorites가 false이면 평점만 저장하고 favorite는 null로 응답한다.
- 성공 응답 캡처: [Postman 07, 21, 06/09, 13, 16의 직접 촬영 화면]
- 중복 응답 캡처: [Postman 08의 HTTP 409 / FAVORITE409_1]
- 인증 실패 캡처: [Postman 23의 HTTP 401 / AUTH401_2]
- 핵심 코드 링크: [GitHub에 업로드된 week-8 코드 링크]
- 테스트: [자동 테스트 결과 보고서 / 본인 환경에서 실행했다면 환경과 결과 기재]

## 트랜잭션 검증 기록 예시

- 정상 흐름: 새 영화 238을 addToFavorites true로 저장 → 201, rating과 favorite가 함께 반환됨.
- 사전 중복 검사: 같은 요청 재전송 → 409 RATING409_1. 행이 추가되지 않음.
- 실제 중간 실패: 자동 테스트에서 사전 exists 결과만 제어하여 DB UNIQUE 충돌을 재현. rating INSERT 다음 favorite INSERT가 실패해 rating도 롤백되는지 확인.
- 구분: 중복 요청의 사전 거절과 INSERT 이후 rollback 검증은 서로 다른 시나리오다.

## 트러블슈팅 예시 1 — JWT 설정 경로

이 문제는 제공된 7주차 코드와 설정을 비교해 발견한 것이다. 본인 PC에서 겪은 오류라고 그대로 적지 말고, 수정·학습 기록으로 구분한다.

- 상황: JwtUtil은 jwt.secret / jwt.access-expiration을 읽는데 기존 YAML에는 spring 아래 jwt가 들어가 있었다.
- 원인: YAML 들여쓰기로 실제 설정 경로가 spring.jwt.*가 되어 코드의 @Value 경로와 달랐다.
- 수정: jwt를 spring과 같은 최상위 단계로 이동했다. 비밀값은 환경 변수 참조를 유지했다.
- 재검증: 기본 application.yml을 사용하는 서버 실행 및 로그인 토큰으로 신규 보호 API 호출을 자동 테스트로 검증한다. [본인 서버 실행·로그인 확인 결과 추가]

## 트러블슈팅 예시 2 — DB 중복 오류의 도메인 코드

- 상황: 사전 검사 이후 발생하는 DB UNIQUE 충돌은 기존 공통 처리에서 COMMON4091로 반환됐다.
- 원인: 기존 처리기가 모든 DataIntegrityViolationException에 같은 공통 코드를 사용했다.
- 수정: 알려진 UNIQUE 제약 이름을 확인하여 FAVORITE409_1 또는 RATING409_1로 변환했다. SQL 오류 원문은 응답에 포함하지 않는다.
- 재검증: DB 저장을 실제 수행하는 테스트에서 사전 검사 결과만 false로 제어하여 UNIQUE 위반을 발생시키고 HTTP 409/FAVORITE409_1과 rating rollback을 검증했다.
- 한계: 실제 두 요청을 동시에 보내는 부하 테스트를 수행했다는 뜻은 아니다.

## 본인이 겪은 문제를 추가할 칸

- 재현 순서:
- 요청 Method / URL / Body (토큰·비밀번호 제외):
- HTTP status / code:
- 예상 결과 / 실제 결과:
- 원인:
- 수정:
- 재검증:
