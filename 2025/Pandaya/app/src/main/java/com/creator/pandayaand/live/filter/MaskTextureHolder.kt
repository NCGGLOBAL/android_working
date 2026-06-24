package com.creator.pandayaand.live.filter

/**
 * 얼굴 소프트 마스크 텍스처를 필터 간 공유하기 위한 홀더. (2단계 차등 톤매핑용)
 *
 * 흐름:
 *   [FaceMaskBlendFilter] 가 매 프레임 onDraw 에서 생성한 마스크 텍스처 ID를 여기에 기록한다.
 *   [ToneMapFilter]       가 같은 GL 스레드·같은 프레임(체인상 blend 다음)에서 이 텍스처를 읽어
 *                         얼굴/배경에 서로 다른 톤매핑을 적용한다.
 *
 * 동일 GL 스레드에서 순차 실행되므로(blend → toneMap) 별도 동기화 불필요.
 * textureId == 0 이면 마스크 없음(차등 보정 비활성).
 */
class MaskTextureHolder {
    @Volatile var textureId: Int = 0
}
