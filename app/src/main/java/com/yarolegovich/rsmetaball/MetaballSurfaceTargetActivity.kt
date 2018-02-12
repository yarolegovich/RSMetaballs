package com.yarolegovich.rsmetaball

import android.view.SurfaceView
import com.yarolegovich.rsmetaball.rs.RSSurfaceTarget

/**
 * Created by yarolegovich on 2/10/18.
 */
class MetaballSurfaceTargetActivity : MetaballBaseActivity<RSSurfaceTarget>() {

    private val surfaceView by lazy { findViewById<SurfaceView>(R.id.metaball_view) }
    private val surfaceTarget = RSSurfaceTarget()

    override fun getLayoutRes() = R.layout.activity_surface

    override fun createRSTarget(): RSSurfaceTarget {
        surfaceView.holder.addCallback(surfaceTarget)
        return surfaceTarget
    }

    override fun invalidateRSTarget() {
        surfaceTarget.renderNextFrame(null)
    }
}