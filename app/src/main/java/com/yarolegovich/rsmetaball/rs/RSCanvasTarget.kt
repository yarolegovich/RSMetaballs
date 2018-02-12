package com.yarolegovich.rsmetaball.rs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.yarolegovich.rsmetaball.R
import kotlin.properties.Delegates

/**
 * Created by yarolegovich on 2/10/18.
 */
class RSCanvasTarget : RSTarget<RSCanvasTarget.RenderArgs> {

    private var rsContext: RenderScript? = null
    private var renderThread: RSRenderThread<*, RSCanvasTarget>? = null

    private val buffer1 = PixelBuffer()
    private val buffer2 = PixelBuffer()
    private var backBuffer = buffer2
    private var frameBuffer = buffer1
    private var hasRenderedFrame = false
    private val bufferLock = Object()

    private var width = 0
    private var height = 0

    var surfaceDownscaleFactor: Float = 2f
        set(value) {
            field = value
            updateBufferSizeOnRenderThread()
        }

    fun setSize(width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            updateBufferSizeOnRenderThread()
        }
    }

    override fun attachToContext(context: RenderScript) {
        rsContext = context
        renderThread = Thread.currentThread() as RSRenderThread<*, RSCanvasTarget>
        updateBufferSize()
    }

    override fun detachFromContext() {
        buffer1.destroy()
        buffer2.destroy()
        renderThread = null
        hasRenderedFrame = false
    }

    override fun isInitialized() = backBuffer.allocation != null

    override fun getTargetAllocation() = backBuffer.allocation!!

    override fun onNextFrameRendered() {
        backBuffer.allocation?.copyTo(backBuffer.bitmap)
        synchronized(bufferLock) {
            backBuffer.isFool = true
            if (areBuffersReadyToSwap()) {
                swapBuffersLocked()
            }
            bufferLock.notify()
        }
    }

    override fun waitUntilNextFrameRequired() {
        val currentThread = Thread.currentThread()
        synchronized(bufferLock) {
            while (!currentThread.isInterrupted && backBuffer.isFool) {
                bufferLock.wait()
            }
        }
    }

    override fun renderNextFrame(args: RenderArgs) {
        synchronized(bufferLock) {
            if (areBuffersReadyToSwap()) {
                swapBuffersIfBackReadyLocked()
            }
            if (frameBuffer.isFool) {
                renderFrameBufferLocked(args)
            }
        }
    }

    private fun swapBuffersIfBackReadyLocked() {
        if (!backBuffer.isFool) {
            bufferLock.wait(MAX_FRAME_WAIT_TIME_MS)
        }
        if (backBuffer.isFool) {
            swapBuffersLocked()
            bufferLock.notify()
        }
    }

    private fun renderFrameBufferLocked(renderArgs: RenderArgs) {
        frameBuffer.bitmap?.let { frame ->
            val canvas = renderArgs.canvas
            canvas.save()
            canvas.scale(surfaceDownscaleFactor, surfaceDownscaleFactor)
            canvas.drawBitmap(frame, 0f, 0f, renderArgs.paint)
            canvas.restore()

            hasRenderedFrame = true
        }
    }

    private fun areBuffersReadyToSwap() = hasRenderedFrame || !frameBuffer.isFool

    private fun swapBuffersLocked() {
        frameBuffer.isFool = false
        hasRenderedFrame = false
        if (frameBuffer == buffer1) {
            frameBuffer = buffer2
            backBuffer = buffer1
        } else {
            frameBuffer = buffer1
            backBuffer = buffer2
        }
    }

    private fun updateBufferSizeOnRenderThread() {
        if (Thread.currentThread() == renderThread) {
            updateBufferSize()
        } else {
            renderThread?.updateTarget(R.id.request_update_size) {
                updateBufferSize()
            }
        }
    }

    private fun updateBufferSize() {
        buffer1.ensureSizeIs(width, height, surfaceDownscaleFactor)
        buffer2.ensureSizeIs(width, height, surfaceDownscaleFactor)
    }

    private inner class PixelBuffer {

        var bitmap: Bitmap? = null
        var allocation: Allocation? = null
        var isFool: Boolean = false

        fun ensureSizeIs(width: Int, height: Int, downscale: Float) {
            destroy()
            val w = (width / downscale).toInt()
            val h = (height / downscale).toInt()
            if (rsContext != null && w > 0 && h > 0) {
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                allocation = Allocation.createFromBitmap(rsContext, bitmap)
            }
        }

        fun destroy() {
            bitmap?.recycle()
            bitmap = null
            allocation?.destroy()
            allocation = null
        }
    }

    class RenderArgs {
        var canvas by Delegates.notNull<Canvas>()
        var paint: Paint? = null
        fun init(canvas: Canvas, paint: Paint?) {
            this.canvas = canvas
            this.paint = paint
        }
    }
}