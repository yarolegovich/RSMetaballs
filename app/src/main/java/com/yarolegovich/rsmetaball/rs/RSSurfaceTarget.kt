package com.yarolegovich.rsmetaball.rs

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.view.Surface
import android.view.SurfaceHolder
import com.yarolegovich.rsmetaball.R

/**
 * Created by yarolegovich on 2/10/18.
 */
class RSSurfaceTarget : RSTarget<Unit?>, SurfaceHolder.Callback{

    private var rsContext: RenderScript? = null
    private var surface: Surface? = null
    private var surfaceAllocation: Allocation? = null
    private var renderThread: RSRenderThread<*, RSSurfaceTarget>? = null

    private var width = 0
    private var height = 0

    private val renderedFlagLock = Object()
    private var isFrameReady = false
    private var isFrameRequested = false

    override fun attachToContext(context: RenderScript) {
        rsContext = context
        renderThread = Thread.currentThread() as RSRenderThread<*, RSSurfaceTarget>
        reInitAllocation()
    }

    override fun detachFromContext() {
        destroyAllocation()
        rsContext = null
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            reInitAllocationOnRenderThread()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface
        reInitAllocationOnRenderThread()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        width = 0
        height = 0
        surface = null
        reInitAllocationOnRenderThread()
    }

    private fun reInitAllocationOnRenderThread() {
        if (Thread.currentThread() == renderThread) {
            reInitAllocation()
        } else {
            renderThread?.updateTarget(R.id.request_update_size) {
                reInitAllocation()
            }
        }
    }

    private fun reInitAllocation() {
        if (isAllocationOfTheSameSize(width, height)) {
            return
        }
        destroyAllocation()
        if (width > 0 && height > 0) {
            val rsContext = rsContext ?: return
            val surface = surface ?: return
            val surfaceType = Type.Builder(rsContext, Element.RGBA_8888(rsContext))
                    .setX(width).setY(height)
                    .create()
            val usage = Allocation.USAGE_IO_OUTPUT or Allocation.USAGE_SCRIPT
            val allocation = Allocation.createTyped(rsContext, surfaceType, usage)
            allocation.surface = surface
            surfaceAllocation = allocation
            isFrameReady = false
        }
    }

    private fun isAllocationOfTheSameSize(width: Int, height: Int) : Boolean {
        val alloc = surfaceAllocation ?: return false
        return alloc.type.x == width && alloc.type.y == height
    }

    private fun destroyAllocation() {
        surfaceAllocation?.destroy()
        surfaceAllocation = null
    }

    override fun isInitialized(): Boolean = surfaceAllocation != null

    override fun getTargetAllocation() = surfaceAllocation!!

    override fun onNextFrameRendered() {
        synchronized(renderedFlagLock) {
            if (isFrameRequested) {
                surfaceAllocation!!.ioSend()
                isFrameReady = false
                isFrameRequested = false
            } else {
                isFrameReady = true
            }
        }
    }

    override fun waitUntilNextFrameRequired() {
        val currentThread = Thread.currentThread()
        synchronized(renderedFlagLock) {
            while (!currentThread.isInterrupted && isFrameReady) {
                renderedFlagLock.wait()
            }
        }
    }

    override fun renderNextFrame(args: Unit?) {
        synchronized(renderedFlagLock) {
            surfaceAllocation?.let {
                if (isFrameReady) {
                    it.ioSend()
                    isFrameReady = false
                    isFrameRequested = false
                    renderedFlagLock.notify()
                } else {
                    isFrameRequested = true
                }
            }
        }
    }
}