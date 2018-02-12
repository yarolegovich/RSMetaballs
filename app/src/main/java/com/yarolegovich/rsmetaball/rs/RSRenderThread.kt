package com.yarolegovich.rsmetaball.rs

import android.content.Context
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Log
import android.util.SparseArray

/**
 * Created by yarolegovich on 2/10/18.
 */
const val MAX_FRAME_WAIT_TIME_MS = 30L

interface RSObject {
    fun attachToContext(context: RenderScript)
    fun isInitialized(): Boolean
    fun detachFromContext()
}

interface RSRenderer : RSObject {
    fun renderFrameTo(allocation: Allocation)
    fun updateFrameState()
}

interface RSTarget<in RenderArgs> : RSObject {
    fun getTargetAllocation(): Allocation
    fun waitUntilNextFrameRequired()
    fun onNextFrameRendered()

    fun renderNextFrame(args: RenderArgs)
}

class RSRenderThread<out Renderer : RSRenderer, out Target : RSTarget<*>>
(private val context: Context,
 private val renderer: Renderer,
 private val target: Target) : Thread() {

    private val requestQueueLock = Object()
    private val requestQueue = SparseArray<Request<*>>()

    private val pauseFlagLock = Object()
    var isPaused = false
        private set

    init {
        name = "RSRenderThread"
    }

    override fun run() {
        val rsContext = RenderScript.create(context)
        try {
            renderer.attachToContext(rsContext)
            target.attachToContext(rsContext)
            renderLoop()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            Log.d("tag", "thread finished")
            target.detachFromContext()
            renderer.detachFromContext()
            rsContext.destroy()
        }
    }

    private fun renderLoop() {
        while (!isInterrupted) {
            waitUntilResumed()
            processRequests()
            if (isNotReady()) {
                waitForRequests()
                continue
            }
            target.waitUntilNextFrameRequired()
            renderer.updateFrameState()
            renderer.renderFrameTo(target.getTargetAllocation())
            target.onNextFrameRendered()
        }
    }

    private fun waitUntilResumed() {
        synchronized(pauseFlagLock) {
            while (!isInterrupted && isPaused) {
                pauseFlagLock.wait()
            }
        }
    }

    private fun processRequests() {
        synchronized(requestQueueLock) {
            for (i in 0 until requestQueue.size()) {
                requestQueue.valueAt(i).execute()
            }
            requestQueue.clear()
        }
    }

    private fun isNotReady() = !(renderer.isInitialized() && target.isInitialized())

    private fun waitForRequests() {
        synchronized(requestQueueLock) {
            while (!isInterrupted && requestQueue.size() == 0) {
                requestQueueLock.wait()
            }
        }
    }

    fun resumeRendering() {
        synchronized(pauseFlagLock) {
            isPaused = false
            pauseFlagLock.notify()
        }
    }

    fun pauseRendering() {
        synchronized(pauseFlagLock) {
            isPaused = true
        }
    }

    fun updateTarget(id: Int, action: Target.() -> Unit) {
        synchronized(requestQueueLock) {
            requestQueue.put(id, Request(target, action))
            requestQueueLock.notify()
        }
    }

    fun updateRenderer(id: Int, action: Renderer.() -> Unit) {
        synchronized(requestQueueLock) {
            requestQueue.put(id, Request(renderer, action))
            requestQueueLock.notify()
        }
    }

    class Request<T>(private val receiver: T, private val action: T.() -> Unit) {
        fun execute() {
            receiver.action()
        }
    }
}
