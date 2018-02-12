package com.yarolegovich.rsmetaball.rs

import android.graphics.*
import android.graphics.drawable.Drawable

/**
 * Created by yarolegovich on 2/10/18.
 */
class RSDrawable : Drawable() {

    private val renderPaint = Paint()
    private val renderArgs = RSCanvasTarget.RenderArgs()

    val renderTarget = RSCanvasTarget()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        renderTarget.setSize(bounds.width(), bounds.height())
    }

    override fun draw(canvas: Canvas) {
        renderArgs.init(canvas, renderPaint)
        renderTarget.renderNextFrame(renderArgs)
    }

    override fun setAlpha(alpha: Int) {
        renderPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        renderPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

}