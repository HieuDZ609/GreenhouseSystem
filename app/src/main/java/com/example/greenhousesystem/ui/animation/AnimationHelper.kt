package com.example.greenhousesystem.ui.animation

import android.animation.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.addListener
import com.google.android.material.card.MaterialCardView

// ═══════════════════════════════════════════════════════════
//  AnimationHelper — Tập hợp các hàm animation tái sử dụng
//  Áp dụng cho toàn bộ UI "Smart Oasis" Dashboard.
//  Tất cả hàm đều extension trên View để gọi gọn hơn.
// ═══════════════════════════════════════════════════════════

object AnimationHelper {

    // ─────────────────────────────────────────────────────────
    //  STAGGERED ENTRANCE ANIMATION
    //  Các view lần lượt slide-up + fade-in với delay tăng dần.
    //  Dùng DecelerateInterpolator để có cảm giác vật lý tự nhiên.
    //
    //  [views]: danh sách view cần animate (theo thứ tự xuất hiện)
    //  [baseDelay]: delay giữa mỗi item (mặc định 80ms)
    //  [duration]: thời gian mỗi animation (mặc định 400ms)
    // ─────────────────────────────────────────────────────────
    fun staggerEntrance(
        views: List<View>,
        baseDelay: Long = 80L,
        duration: Long = 400L
    ) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 40f

            val delay = index * baseDelay + 100L  // delay tăng theo index

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HERO SLIDE-UP (cho Login page)
    //  Chữ + logo trượt nhẹ từ dưới lên với easing mượt mà.
    //  [view]: view cần animate
    //  [delay]: delay trước khi bắt đầu (ms)
    // ─────────────────────────────────────────────────────────
    fun heroSlideUp(view: View, delay: Long = 0L) {
        view.alpha = 0f
        view.translationY = 30f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600L)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator(2.5f))
            .start()
    }

    // ─────────────────────────────────────────────────────────
    //  FORM CARD SLIDE-UP (cho Login form)
    //  Card login trượt lên từ dưới sau khi hero load xong.
    // ─────────────────────────────────────────────────────────
    fun formCardEntrance(view: View) {
        view.alpha = 0f
        view.translationY = 60f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700L)
            .setStartDelay(300L)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
    }

    // ─────────────────────────────────────────────────────────
    //  PULSE ANIMATION (cho Device Status dot)
    //  Vòng tròn mở rộng + mờ dần + lặp lại (2 vòng xen kẽ).
    //  [pulseOuter], [pulseMiddle]: 2 view vòng pulse
    //  [colorRes]: màu pulse (lime green khi online)
    // ─────────────────────────────────────────────────────────
    fun startPulse(pulseOuter: View, pulseMiddle: View) {
        fun makePulseAnimator(view: View, delay: Long): AnimatorSet {
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1.8f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1.8f)
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.8f, 0f)
            return AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 1800L
                startDelay = delay
                interpolator = DecelerateInterpolator()
                addListener(onEnd = { this.start() })  // lặp vô hạn
            }
        }

        makePulseAnimator(pulseOuter, 0L).start()
        makePulseAnimator(pulseMiddle, 600L).start()
    }

    fun stopPulse(pulseOuter: View, pulseMiddle: View) {
        pulseOuter.animate().cancel()
        pulseMiddle.animate().cancel()
        pulseOuter.alpha = 0f
        pulseMiddle.alpha = 0f
    }

    // ─────────────────────────────────────────────────────────
    //  SPRING SCALE (cho nút bấm — cảm giác "bounce")
    //  Scale xuống khi nhấn, spring lên 1.0 khi nhả.
    //  Dùng OvershootInterpolator để tạo cảm giác lò xo.
    //  [view]: view (Button, Card, …) cần hiệu ứng
    // ─────────────────────────────────────────────────────────
    fun View.setupSpringPress() {
        setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.93f)
                        .scaleY(0.93f)
                        .setDuration(120L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300L)
                        .setInterpolator(OvershootInterpolator(4f))
                        .start()
                    v.performClick()   // vẫn trigger onClick
                }
            }
            true
        }
    }

    // ─────────────────────────────────────────────────────────
    //  PRESET MODE SELECT ANIMATION
    //  Khi chọn 1 preset card:
    //  1. Scale lên 1.04 (spring)
    //  2. Stroke sáng lên
    //  3. Các card còn lại scale về 1.0
    //
    //  [selectedCard]: card vừa được chọn
    //  [allCards]: tất cả preset cards
    //  [accentColor]: màu stroke khi selected
    // ─────────────────────────────────────────────────────────
    fun selectPresetCard(
        selectedCard: MaterialCardView,
        allCards: List<MaterialCardView>,
        accentColor: Int,
        defaultStrokeColor: Int
    ) {
        allCards.forEach { card ->
            if (card == selectedCard) {
                // Animate scale lên + stroke sáng
                card.animate()
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(250L)
                    .setInterpolator(OvershootInterpolator(3f))
                    .start()
                card.strokeColor = accentColor
                card.strokeWidth = 3
            } else {
                // Reset các card khác
                card.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200L)
                    .start()
                card.strokeColor = defaultStrokeColor
                card.strokeWidth = 1
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  MANUAL CARD REVEAL (cho cardSlider trong Control)
    //  Card xuất hiện với scale + fade in.
    //  [show]: true = hiện, false = ẩn
    // ─────────────────────────────────────────────────────────
    fun toggleManualCard(card: View, show: Boolean) {
        if (show) {
            card.visibility = View.VISIBLE
            card.alpha = 0f
            card.scaleX = 0.9f
            card.scaleY = 0.9f
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350L)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
        } else {
            card.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(200L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { card.visibility = View.GONE }
                .start()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  ALERT BANNER SHOW (frosted glass, shake + slide down)
    //  [alertView]: banner view
    //  [shakeTarget]: icon cảnh báo để shake
    // ─────────────────────────────────────────────────────────
    fun showAlert(alertView: View, shakeTarget: View? = null) {
        alertView.visibility = View.VISIBLE
        alertView.alpha = 0f
        alertView.translationY = -16f
        alertView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400L)
            .setInterpolator(DecelerateInterpolator(2f))
            .withEndAction {
                // Shake icon sau khi banner hiện
                shakeTarget?.let { shakeView(it) }
            }
            .start()
    }

    fun hideAlert(alertView: View) {
        alertView.animate()
            .alpha(0f)
            .translationY(-16f)
            .setDuration(300L)
            .withEndAction { alertView.visibility = View.GONE }
            .start()
    }

    /**
     * Shake animation — rung nhẹ sang trái/phải.
     * Dùng cho icon cảnh báo khi alert xuất hiện.
     */
    fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(
            view, "translationX",
            0f, -8f, 8f, -6f, 6f, -4f, 4f, -2f, 2f, 0f
        ).apply {
            duration = 500L
            interpolator = DecelerateInterpolator()
        }
        shake.start()
    }

    // ─────────────────────────────────────────────────────────
    //  OFFLINE GRAYSCALE FILTER
    //  Khi thiết bị offline, toàn bộ dashboard bị desaturate.
    //  Dùng ColorMatrix để giảm saturation về 0.
    //  [rootView]: view gốc của dashboard
    //  [isOffline]: true → apply grayscale, false → remove
    // ─────────────────────────────────────────────────────────
    fun applyOfflineFilter(rootView: View, isOffline: Boolean) {
        val targetSaturation = if (isOffline) 0f else 1f
        val currentSaturation = if (isOffline) 1f else 0f

        ValueAnimator.ofFloat(currentSaturation, targetSaturation).apply {
            duration = 600L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val sat = anim.animatedValue as Float
                val cm = android.graphics.ColorMatrix().apply { setSaturation(sat) }
                rootView.setLayerType(View.LAYER_TYPE_HARDWARE, android.graphics.Paint().apply {
                    colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                })
            }
        }.start()
    }

    // ─────────────────────────────────────────────────────────
    //  BACKGROUND TINT CROSS-FADE (cho Control preset modes)
    //  Khi chọn preset, nền màn hình cross-fade sang tint màu đó.
    //  [rootView]: view gốc của Control Fragment
    //  [targetColor]: màu tint mới (với alpha thấp ~20%)
    // ─────────────────────────────────────────────────────────
    fun crossFadeBackground(rootView: View, targetColor: Int) {
        val currentColor = (rootView.background as? ColorDrawable)?.color
            ?: android.graphics.Color.parseColor("#0A1A0A")

        val transitionDrawable = TransitionDrawable(
            arrayOf(
                ColorDrawable(currentColor),
                ColorDrawable(targetColor)
            )
        )
        rootView.background = transitionDrawable
        transitionDrawable.startTransition(400)  // cross-fade 400ms
    }
    fun View.postWithAnimation(delay: Long = 500L, action: () -> Unit) {
        this.postDelayed({
            action()
        }, delay)
    }

    // ─────────────────────────────────────────────────────────
    //  COUNTER ANIMATION (cho số liệu sensor)
    //  Số tăng dần từ 0 đến giá trị mục tiêu.
    //  [textView]: TextView hiển thị số
    //  [targetValue]: giá trị đích
    //  [suffix]: đơn vị (°C, %, …)
    // ─────────────────────────────────────────────────────────
    fun animateCounter(
        textView: android.widget.TextView,
        targetValue: Float,
        suffix: String
    ) {
        ValueAnimator.ofFloat(0f, targetValue).apply {
            duration = 1000L
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                textView.text = String.format("%.1f%s", v, suffix)
            }
        }.start()
    }
}