package com.yarolegovich.rsmetaball

import android.view.View
import android.widget.SeekBar
import com.yarolegovich.rsmetaball.rs.*

/**
 * Created by yarolegovich on 2/10/18.
 */
class MetaballCanvasTargetActivity : MetaballBaseActivity<RSCanvasTarget>() {

    private val qualitySeekBar by lazy { findViewById<SeekBar>(R.id.seek_bar_quality) }
    private val metaballView by lazy { findViewById<View>(R.id.metaball_view) }
    private val drawable = RSDrawable()

    private var surfaceScaleFactor = 1f / drawable.renderTarget.surfaceDownscaleFactor

    override fun getLayoutRes() = R.layout.activity_canvas

    override fun createRSTarget(): RSCanvasTarget {
        metaballView.background = drawable
        return drawable.renderTarget
    }

    override fun invalidateRSTarget() {
        drawable.invalidateSelf()
    }

    override fun getDimensionTransform() = { coord: Float -> coord * surfaceScaleFactor }

    private fun setDownscaleFactor(newFactor: Float) {
        surfaceScaleFactor = 1f / newFactor
        renderThread.updateTarget(R.id.request_update_quality) {
            surfaceDownscaleFactor = newFactor
        }
        renderThread.updateRenderer(R.id.request_update_ball_radii) {
            updateBallRadii()
        }
    }

    override fun handleProgressChange(id: Int, progress: Int) {
        if (id == R.id.seek_bar_quality) {
            setDownscaleFactor(qualityProgressToDownscaleFactor(progress))
        }
    }

    private fun qualityProgressToDownscaleFactor(progress: Int): Float {
        return ((qualitySeekBar.max - progress) + 1).toFloat()
    }

}