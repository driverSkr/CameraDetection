package com.ethan.base.progress

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.ethan.base.R
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin

class LoadingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    init {
        init(context, attributeSet)
    }

    private var animator: ValueAnimator? = null
    private var linePaint: Paint? = null
    private var value = 0
    private var radius1 = 0
    private var radius2 = 0

    private fun init(context: Context, attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.LoadingView)
        val loadingColor = ta.getColor(R.styleable.LoadingView_color, resources.getColor(R.color.white))
        val lineWidth = ta.getDimension(R.styleable.LoadingView_strokeWidth, 8f)
        ta.recycle()

        linePaint = Paint().apply {
            color = loadingColor
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = lineWidth
        }
        setupAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        radius1 = measuredWidth / 2 - 8
        radius2 = radius1 / 3 * 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0..7) {
            linePaint!!.alpha = (255 / 8 * (i + value)) % 255
            val d = 0.25 * i * PI
            val startX = (radius1 + 8 + radius2 * cos(d)).toFloat()
            val startY = (radius1 + 8 - radius2 * sin(d)).toFloat()
            val stopX = (radius1 + 8 + radius1 * cos(d)).toFloat()
            val stopY = (radius1 + 8 - radius1 * sin(d)).toFloat()
            canvas.drawLine(startX, startY, stopX, stopY, linePaint!!)
        }
    }

    private fun setupAnimation() {
        animator = ValueAnimator.ofInt(0, 7).apply {
            duration = 800
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                value = animation.animatedValue as Int
                invalidate()
            }
        }
    }

    fun start() {
        animator?.start()
    }

    fun stop() {
        animator?.cancel()
    }

}