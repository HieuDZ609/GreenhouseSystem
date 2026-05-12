package com.example.greenhousesystem.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.greenhousesystem.R
import kotlin.math.*

// ═══════════════════════════════════════════════════════════
//  LiquidGaugeView — Hiển thị cảm biến dạng "mực nước"
//  Vẽ một hình tròn với sóng nước dâng lên theo % giá trị.
//  Thuộc tính XML custom: gaugeColor, gaugeWaveColor, gaugeMin, gaugeMax
// ═══════════════════════════════════════════════════════════
class LiquidGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Thuộc tính giao diện ──────────────────────────────────
    private var gaugeColor = Color.parseColor("#E53935")       // màu sóng chính
    private var gaugeWaveColor = Color.parseColor("#B71C1C")   // màu sóng thứ hai
    private var gaugeMin = 0f                                   // giá trị tối thiểu
    private var gaugeMax = 100f                                 // giá trị tối đa
    private var currentValue = 0f                              // giá trị hiện tại

    // ── Đối tượng vẽ ─────────────────────────────────────────
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()

    // ── Trạng thái animation sóng ────────────────────────────
    private var wavePhase = 0f          // pha sóng chính (0..2π)
    private var wave2Phase = PI.toFloat() / 2  // pha sóng phụ (lệch 90°)

    // ── Animator sóng liên tục ───────────────────────────────
    private val waveAnimator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
        duration = 2000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { animator ->
            wavePhase = animator.animatedValue as Float
            wave2Phase = wavePhase + PI.toFloat() / 2
            invalidate()   // yêu cầu vẽ lại
        }
    }

    // ── Animator mực nước dâng lên khi setValue ──────────────
    private var animatedFillPercent = 0f

    init {
        // Đọc thuộc tính XML custom
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.LiquidGaugeView)
            gaugeColor = ta.getColor(R.styleable.LiquidGaugeView_gaugeColor, gaugeColor)
            gaugeWaveColor = ta.getColor(R.styleable.LiquidGaugeView_gaugeWaveColor, gaugeWaveColor)
            gaugeMin = ta.getFloat(R.styleable.LiquidGaugeView_gaugeMin, gaugeMin)
            gaugeMax = ta.getFloat(R.styleable.LiquidGaugeView_gaugeMax, gaugeMax)
            ta.recycle()
        }

        // Setup Paint objects
        wavePaint.apply {
            color = gaugeColor
            style = Paint.Style.FILL
            alpha = 200
        }
        wave2Paint.apply {
            color = gaugeWaveColor
            style = Paint.Style.FILL
            alpha = 160
        }
        circlePaint.apply {
            color = Color.parseColor("#0A0A0A")  // nền tối bên trong vòng tròn
            style = Paint.Style.FILL
        }
        borderPaint.apply {
            color = gaugeColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 100
        }
        textPaint.apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        waveAnimator.start()    // bắt đầu animation sóng khi view gắn vào window
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator.cancel()   // dừng animation khi view bị remove (tránh memory leak)
    }

    /**
     * Set giá trị mới với animation mực nước dâng lên.
     * [value]: giá trị mới (ví dụ: 35.5 độ C)
     */
    fun setValue(value: Float) {
        val targetPercent = ((value - gaugeMin) / (gaugeMax - gaugeMin)).coerceIn(0f, 1f)
        // Animate từ % hiện tại → % mới trong 1000ms
        ValueAnimator.ofFloat(animatedFillPercent, targetPercent).apply {
            duration = 1200L
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                animatedFillPercent = anim.animatedValue as Float
                invalidate()
            }
        }.start()
        currentValue = value
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 6f

        // ── 1. Clip tất cả nội dung vào trong hình tròn ──────
        clipPath.reset()
        clipPath.addCircle(cx, cy, radius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        // ── 2. Vẽ nền tối ────────────────────────────────────
        canvas.drawCircle(cx, cy, radius, circlePaint)

        // ── 3. Tính y mực nước dựa trên % ───────────────────
        //    Khi percent=1.0: mực nước ở đỉnh (cy - radius)
        //    Khi percent=0.0: mực nước ở đáy (cy + radius)
        val waterTop = cy + radius - (2 * radius * animatedFillPercent)
        val waveAmplitude = radius * 0.08f  // biên độ sóng = 8% bán kính
        val waveLength = width.toFloat()    // một chu kỳ sóng = chiều rộng view

        // ── 4. Vẽ sóng phụ (phía sau) ────────────────────────
        val wavePath2 = Path()
        wavePath2.moveTo(0f, cy + radius)
        for (x in 0..width) {
            val y = waterTop + waveAmplitude *
                    sin(2 * PI * x / waveLength + wave2Phase + PI / 3).toFloat()
            wavePath2.lineTo(x.toFloat(), y)
        }
        wavePath2.lineTo(width.toFloat(), cy + radius)
        wavePath2.lineTo(0f, cy + radius)
        wavePath2.close()
        canvas.drawPath(wavePath2, wave2Paint)

        // ── 5. Vẽ sóng chính (phía trước) ───────────────────
        val wavePath = Path()
        wavePath.moveTo(0f, cy + radius)
        for (x in 0..width) {
            val y = waterTop + waveAmplitude *
                    sin(2 * PI * x / waveLength + wavePhase).toFloat()
            wavePath.lineTo(x.toFloat(), y)
        }
        wavePath.lineTo(width.toFloat(), cy + radius)
        wavePath.lineTo(0f, cy + radius)
        wavePath.close()
        canvas.drawPath(wavePath, wavePaint)

        // ── 6. Vẽ border vòng tròn (sau khi clip) ────────────
        // Cần reset clip để vẽ border ngoài vòng tròn
        canvas.save()
        canvas.clipPath(Path().apply { addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW) })
        canvas.drawCircle(cx, cy, radius, borderPaint)
        canvas.restore()
    }
}


// ═══════════════════════════════════════════════════════════
//  ColorOrbitWheelView — Vòng tròn màu HSV cho chọn màu LED
//  Người dùng chạm vào vòng ngoài → chọn Hue.
//  Kéo vào tâm → thay đổi Saturation + Value.
//  Indicator chạy theo vị trí ngón tay.
// ═══════════════════════════════════════════════════════════
class ColorOrbitWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Listener gọi lại khi màu thay đổi
    var onColorChanged: ((r: Int, g: Int, b: Int) -> Unit)? = null

    // ── Trạng thái màu hiện tại (HSV) ────────────────────────
    private val hsv = floatArrayOf(0f, 1f, 1f)  // Hue, Saturation, Value

    // ── Paint objects ─────────────────────────────────────────
    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000")
        style = Paint.Style.FILL
    }

    // ── Kích thước vòng tròn ──────────────────────────────────
    private var outerRadius = 0f
    private var innerRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    // ── Vị trí indicator (ngón tay) ──────────────────────────
    private var indicatorX = 0f
    private var indicatorY = 0f
    private var isDragging = false

    // ── SweepGradient để tạo vòng màu HSV ───────────────────
    private val hsvColors = IntArray(361) { i ->
        Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        outerRadius = min(centerX, centerY) - 4f
        innerRadius = outerRadius * 0.60f   // tâm vòng tròn chiếm 60% bán kính

        // Tạo SweepGradient với đầy đủ màu HSV
        val sweepShader = SweepGradient(
            centerX, centerY,
            hsvColors.map { it }.toIntArray(),
            null
        )
        wheelPaint.shader = sweepShader

        // Vị trí indicator ban đầu (phía phải, góc 0°)
        updateIndicatorPosition()
    }

    /**
     * Cập nhật vị trí indicator dựa trên hue hiện tại.
     * Indicator nằm ở giữa vòng wheel (tại (outerRadius+innerRadius)/2).
     */
    private fun updateIndicatorPosition() {
        val angleRad = Math.toRadians(hsv[0].toDouble()).toFloat()
        val r = (outerRadius + innerRadius) / 2f
        indicatorX = centerX + r * cos(angleRad)
        indicatorY = centerY + r * sin(angleRad)
    }

    override fun onDraw(canvas: Canvas) {
        // ── 1. Vẽ vòng wheel màu ─────────────────────────────
        canvas.drawCircle(centerX, centerY, outerRadius, wheelPaint)

        // ── 2. Vẽ vòng tối ở giữa (tạo hình donut) ─────────
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F1A0E")  // match background dark
        }
        canvas.drawCircle(centerX, centerY, innerRadius, holePaint)

        // ── 3. Vẽ màu hiện tại ở tâm ─────────────────────────
        centerPaint.color = Color.HSVToColor(hsv)
        canvas.drawCircle(centerX, centerY, innerRadius * 0.75f, centerPaint)

        // ── 4. Vẽ shadow mờ tại tâm ──────────────────────────
        canvas.drawCircle(centerX, centerY, innerRadius, innerShadowPaint)

        // ── 5. Vẽ indicator (vòng tròn trắng nhỏ) ────────────
        if (outerRadius > 0) {
            canvas.drawCircle(indicatorX, indicatorY, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.HSVToColor(hsv)
            })
            canvas.drawCircle(indicatorX, indicatorY, 14f, indicatorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y
        val dx = touchX - centerX
        val dy = touchY - centerY
        val dist = sqrt(dx * dx + dy * dy)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Chỉ xử lý nếu chạm vào vùng wheel (giữa innerRadius và outerRadius)
                if (dist in innerRadius..outerRadius) {
                    isDragging = true
                    // Tính góc hue từ vị trí chạm
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    hsv[0] = angle
                    updateIndicatorPosition()
                    notifyColorChanged()
                    invalidate()
                    return true
                }
                // Nếu chạm vào tâm → thay đổi saturation bằng khoảng cách
                if (dist < innerRadius) {
                    hsv[1] = (dist / innerRadius).coerceIn(0f, 1f)
                    notifyColorChanged()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> isDragging = false
        }
        return super.onTouchEvent(event)
    }

    /**
     * Thông báo màu mới ra ngoài qua lambda callback.
     * Chuyển đổi HSV → RGB trước khi gọi.
     */
    private fun notifyColorChanged() {
        val rgb = Color.HSVToColor(hsv)
        onColorChanged?.invoke(
            Color.red(rgb),
            Color.green(rgb),
            Color.blue(rgb)
        )
    }

    /**
     * Set màu từ bên ngoài (ví dụ khi sync từ Firebase).
     * [r], [g], [b]: giá trị 0-255
     */
    fun setColor(r: Int, g: Int, b: Int) {
        Color.RGBToHSV(r, g, b, hsv)
        updateIndicatorPosition()
        invalidate()
    }
}