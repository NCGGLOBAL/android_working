package com.creator.pandayaand.live.filter

/**
 * 필터 체인에서 "원본 프레임"(뷰티 효과 적용 전, 업라이트 2D 텍스처)을 공유하기 위한 홀더.
 *
 * 흐름:
 *   [OriginalCaptureFilter] 가 매 프레임 onDraw 직후 자신의 출력 텍스처 ID를 여기에 기록한다.
 *   [FaceMaskBlendFilter]   가 같은 GL 스레드·같은 프레임에서 이 텍스처를 읽어
 *                           뷰티 적용본과 얼굴 마스크로 블렌딩한다.
 *
 * 동일 GL 스레드에서 순차 실행되므로( capture → 내장필터 → blend ) 별도 동기화 불필요.
 * 단, GL 스레드와 다른 스레드 간 가시성을 위해 @Volatile 로 둔다.
 */
class OriginalFrameHolder {
    @Volatile var textureId: Int = 0
    @Volatile var width: Int = 0
    @Volatile var height: Int = 0

    fun clear() {
        textureId = 0
        width = 0
        height = 0
    }
}
