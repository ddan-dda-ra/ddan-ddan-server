package notbe.tmtm.ddanddanserver.application.processor

/**
 * Mock OAuth processor 분류용 마커 인터페이스.
 *
 * - dev/local profile 등 `mock-oauth.enabled=true` 환경에서만 빈으로 등록되는 processor가 구현.
 * - `OAuthProcessorFactory`는 이 마커를 기준으로 real/mock 풀을 분리한다.
 * - 별도 메서드 없이 분류 목적에만 사용 (메서드 추가 시 `OAuthProcessor` 인터페이스를 오염시키지 않기 위한 SOLID 분리).
 */
interface MockOAuthProcessor : OAuthProcessor
