package com.creator.pandayaand.live.filter

import android.opengl.GLES20
import android.util.Log
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilter
import com.ksyun.media.streamer.framework.ImgTexFormat
import com.ksyun.media.streamer.util.gles.GLRender

/**
 * 역광·빛번짐 보정(톤매핑) 필터.
 *
 * 필터 체인의 **맨 뒤**(blend 다음)에 위치해 최종 합성 프레임에 적용된다.
 *
 * [1단계] 전체 프레임 균일 보정
 *   - 하이라이트(밝게 날아가는 영역)를 knee 위쪽만 부드럽게 눌러 창문 역광/빛번짐 완화
 *   - 섀도우(어두운 영역)를 살짝 들어 역광 속 얼굴 디테일 복원
 *   - 감마로 전체 톤 미세조정
 *
 * [2단계] 얼굴/배경 차등 보정 (facePriority > 0 일 때)
 *   - 기존 얼굴 마스크([MaskTextureHolder]) 재사용 — 별도 세그멘테이션 모델 불필요
 *   - 배경: 위 보정값 그대로(하이라이트 강하게 압축 → 창문 역광 죽임)
 *   - 얼굴: 밝기를 올리고 하이라이트는 약하게만 압축 → 역광에서도 얼굴 선명
 *   - facePriority == 0 또는 마스크 없음 → 1단계와 동일하게 동작(하위 호환)
 *
 * 진짜 멀티프레임 HDR 이 아니라 단일 SDR 프레임 톤매핑이므로 실시간 송출에 적합하다.
 * 모든 파라미터는 @Volatile var 로 런타임 튜닝 가능(매 프레임 uniform 으로 주입).
 */
class ToneMapFilter(
    glRender: GLRender,
    private val maskHolder: MaskTextureHolder? = null
) : ImgTexFilter(glRender) {

    companion object {
        // sTexture 는 베이스가 자동 선언(2D) 하므로 여기서 선언 금지.
        private const val FRAGMENT_SHADER_BODY = """
precision mediump float;
varying vec2 vTextureCoord;
uniform float uHighlight;     // 0~1, 낮을수록 하이라이트 강하게 압축 (1.0 = 끔)
uniform float uShadow;        // 1~2, 높을수록 섀도우 강하게 들어올림 (1.0 = 끔)
uniform float uKnee;          // 0~1, 하이라이트 압축 시작 밝기
uniform float uGamma;         // 감마 (1.0 = 끔)
uniform float uStrength;      // 0~1, 전체 효과 강도 (0 = 원본)
uniform sampler2D uMaskTex;   // 얼굴 소프트 마스크 (얼굴=1, 배경=0)
uniform float uFacePriority;  // 0~1, 얼굴/배경 차등 보정 정도 (0 = 균일)
uniform float uFaceBrighten;  // 얼굴 밝기 배수 (1.0 = 끔)

vec3 toneBackground(vec3 c, float l) {
    float hl = smoothstep(uKnee, 1.0, l);
    c *= mix(1.0, uHighlight, hl);              // 하이라이트 압축
    c += (uShadow - 1.0) * (1.0 - l) * c;       // 섀도우 리프트
    return pow(clamp(c, 0.0, 1.0), vec3(uGamma));
}

vec3 toneFace(vec3 c, float l) {
    c *= uFaceBrighten;                          // 얼굴 밝기 업
    float hl = smoothstep(uKnee, 1.0, l);
    c *= mix(1.0, mix(1.0, uHighlight, 0.4), hl);// 얼굴은 약하게만 압축
    return clamp(c, 0.0, 1.0);
}

void main() {
    vec3 src = texture2D(sTexture, vTextureCoord).rgb;
    float l = dot(src, vec3(0.299, 0.587, 0.114));

    vec3 mapped = toneBackground(src, l);
    if (uFacePriority > 0.0) {
        float m = uFacePriority * texture2D(uMaskTex, vTextureCoord).r;
        mapped = mix(mapped, toneFace(src, l), m);
    }

    gl_FragColor = vec4(mix(src, mapped, uStrength), 1.0);
}
"""
    }

    private val TAG = "FACE_FILTER"

    // ── 런타임 튜닝 파라미터 (기본값: 은은한 역광 보정) ──────────────────────
    @Volatile var highlight    = 0.75f   // 하이라이트 75%로 압축
    @Volatile var shadow       = 1.15f   // 섀도우 15% 리프트
    @Volatile var knee         = 0.65f   // 밝기 0.65 이상부터 압축
    @Volatile var gamma        = 1.0f
    @Volatile var strength     = 1.0f    // 전체 강도
    @Volatile var facePriority = 0.0f    // 0 = 균일, >0 = 얼굴/배경 차등
    @Volatile var faceBrighten = 1.1f    // 얼굴 밝기 배수

    private var uHighlightLoc    = -1
    private var uShadowLoc       = -1
    private var uKneeLoc         = -1
    private var uGammaLoc        = -1
    private var uStrengthLoc     = -1
    private var uMaskLoc         = -1
    private var uFacePriorityLoc = -1
    private var uFaceBrightenLoc = -1

    init {
        mFragmentShaderBody = FRAGMENT_SHADER_BODY
    }

    override fun onInitialized() {
        super.onInitialized()
        uHighlightLoc    = getUniformLocation("uHighlight")
        uShadowLoc       = getUniformLocation("uShadow")
        uKneeLoc         = getUniformLocation("uKnee")
        uGammaLoc        = getUniformLocation("uGamma")
        uStrengthLoc     = getUniformLocation("uStrength")
        uMaskLoc         = getUniformLocation("uMaskTex")
        uFacePriorityLoc = getUniformLocation("uFacePriority")
        uFaceBrightenLoc = getUniformLocation("uFaceBrighten")
        Log.d(TAG, "[ToneMap] onInitialized: prog=$mProgramId hl=$uHighlightLoc mask=$uMaskLoc")
    }

    override fun onFormatChanged(format: ImgTexFormat) {
        super.onFormatChanged(format)
    }

    override fun onDrawArraysPre() {
        GLES20.glUniform1f(uHighlightLoc, highlight)
        GLES20.glUniform1f(uShadowLoc, shadow)
        GLES20.glUniform1f(uKneeLoc, knee)
        GLES20.glUniform1f(uGammaLoc, gamma)
        GLES20.glUniform1f(uStrengthLoc, strength)
        GLES20.glUniform1f(uFaceBrightenLoc, faceBrighten)

        // 마스크가 준비된 경우에만 차등 보정 활성 (없으면 0 → 균일 보정)
        val maskTex = maskHolder?.textureId ?: 0
        val effPriority = if (maskTex != 0) facePriority else 0f
        GLES20.glUniform1f(uFacePriorityLoc, effPriority)
        if (maskTex != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTex)
            GLES20.glUniform1i(uMaskLoc, 3)
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)  // 부모 sTexture 용 복원
    }
}
