package com.yarolegovich.rsmetaball.rs

import android.graphics.Color
import android.renderscript.Allocation
import android.renderscript.Float3
import android.renderscript.RenderScript
import com.yarolegovich.rsmetaball.ScriptC_metaball
import com.yarolegovich.rsmetaball.ScriptField_BallData
import com.yarolegovich.rsmetaball.Metaball
import com.yarolegovich.rsmetaball.MetaballScene

/**
 * Created by yarolegovich on 2/10/18.
 */
class RSMetaballRenderer(private val scene: MetaballScene,
                         initBlock: RSMetaballRenderer.() -> Unit = {}) : RSRenderer {

    private var rsContext: RenderScript? = null
    private var script: ScriptC_metaball? = null
    private var ballData: ScriptField_BallData? = null

    var metaballs: List<Metaball> = emptyList()
        set(value) {
            val oldCount = field.size
            field = value
            updateBalls(oldCount)
        }

    var ballColor = Color.rgb(128, 56, 39)
        set(value) {
            field = value
            script?.apply { _color = value.toFloatVector() }
        }

    var isGlowEnabled = false

    override fun isInitialized() = script != null && ballData != null

    init {
        initBlock()
    }

    override fun updateFrameState() {
        scene.updateScene()
        bindBallDataToScript()
    }

    override fun renderFrameTo(allocation: Allocation) {
        val script = ensureScriptIsReady()
        if (isGlowEnabled) {
            script.forEach_drawGlowingMetaball(allocation, allocation)
        } else {
            script.forEach_drawMetaball(allocation, allocation)
        }
    }

    private fun ensureScriptIsReady(): ScriptC_metaball {
        return script ?: throw IllegalStateException()
    }

    private fun bindBallDataToScript() {
        ballData?.apply {
            for (i in 0 until metaballs.size) {
                set_x(i, metaballs[i].x, false)
                set_y(i, metaballs[i].y, false)
            }
            copyAll()
        }
    }

    private fun updateBalls(oldBallCount: Int) {
        resizeBallDataIfRequired(oldBallCount)
        script?.apply { _ballCount = metaballs.size }
        updateBallRadii()
    }

    fun updateBallRadii() {
        ballData?.apply {
            metaballs.map { it.radius }.map { it * it }.forEachIndexed { i, value ->
                set_radiusSquared(i, value, false)
            }
            copyAll()
        }
    }

    private fun resizeBallDataIfRequired(oldBallCount: Int) {
        val rsContext = rsContext ?: return
        if (metaballs.isNotEmpty() && (oldBallCount != metaballs.size || ballData == null)) {
            destroyBallData()
            ballData = ScriptField_BallData(rsContext, metaballs.size)
            script?.bind_ballDataArray(ballData)
        }
    }

    override fun attachToContext(context: RenderScript) {
        rsContext = context
        script = ScriptC_metaball(context)
        script?.apply {
            _ballCount = metaballs.size
            _color = ballColor.toFloatVector()
        }
        updateBalls(0)
    }

    override fun detachFromContext() {
        destroyBallData()
        script?.destroy()
        script = null
        rsContext = null
    }

    private fun destroyBallData() {
        ballData?.allocation?.destroy()
        ballData = null
    }

    private fun Int.toFloatVector() = Float3(
            Color.red(this) / 255f,
            Color.green(this) / 255f,
            Color.blue(this) / 255f)
}