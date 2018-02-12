package com.yarolegovich.rsmetaball

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.yarolegovich.rsmetaball.rs.*
import com.yarolegovich.rsmetaball.util.ProgressDispatcher
import java.util.*

/**
 * Created by yarolegovich on 2/10/18.
 */
abstract class MetaballBaseActivity<Target : RSTarget<*>> : AppCompatActivity(), MetaballScene {

    private val redComponentSeekBar by lazy { findViewById<SeekBar>(R.id.seek_bar_red_component) }
    private val greenComponentSeekBar by lazy { findViewById<SeekBar>(R.id.seek_bar_green_component) }
    private val blueComponentSeekBar by lazy { findViewById<SeekBar>(R.id.seek_bar_blue_component) }
    private val glowToggle by lazy { findViewById<SwitchCompat>(R.id.switch_glow) }
    private val ballCountSeekBar by lazy { findViewById<SeekBar>(R.id.seek_bar_ball_count) }
    private val animationStateView by lazy { findViewById<TextView>(R.id.tv_animation_state) }
    private val optionsToggleArrow by lazy { findViewById<View>(R.id.iv_options_toggle_arrow) }
    private val optionsPanel by lazy { findViewById<View>(R.id.options_panel) }
    private val optionsContainer by lazy { findViewById<View>(R.id.options_container) }
    private val metaballView by lazy { findViewById<View>(R.id.metaball_view) }
    private val optionsInterpolator = FastOutSlowInInterpolator()
    private var isOptionsPanelVisible = true

    protected lateinit var renderThread: RSRenderThread<RSMetaballRenderer, Target>
    protected lateinit var renderer: RSMetaballRenderer
    protected lateinit var currentBalls: List<Metaball>

    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutRes())

        ProgressDispatcher {
            when (it.id) {
                R.id.seek_bar_red_component -> onSelectedColorChanged()
                R.id.seek_bar_green_component -> onSelectedColorChanged()
                R.id.seek_bar_blue_component -> onSelectedColorChanged()
                else -> handleProgressChange(it.id, it.progress)
            }
        }.attachToAllSeekBarsIn(this)

        findViewById<View>(R.id.btn_options_toggle).setOnClickListener {
            toggleOptionsPanel()
        }
        findViewById<View>(R.id.btn_glow_toggle).setOnClickListener {
            toggleGlowMode()
        }
        findViewById<View>(R.id.btn_generate_balls).setOnClickListener {
            onGenerateNewBallsRequest()
        }
        metaballView.setOnClickListener {
            toggleAnimation()
        }

        currentBalls = generateRandomBalls()
        renderer = RSMetaballRenderer(this) {
            ballColor = getSelectedColor()
            isGlowEnabled = isGlowModeOn()
            metaballs = currentBalls
        }
        renderThread = RSRenderThread(this, renderer, createRSTarget())
        renderThread.start()

        mainThreadHandler.post(RefreshTask())
    }

    override fun onDestroy() {
        super.onDestroy()
        renderThread.interrupt()
    }

    private fun toggleOptionsPanel() {
        val animDuration = 300L
        isOptionsPanelVisible = !isOptionsPanelVisible
        val targetTranslation = if (isOptionsPanelVisible) 0 else optionsPanel.height
        val targetRotation = if (isOptionsPanelVisible) 0f else 180f
        optionsContainer.animate()
                .translationY(targetTranslation.toFloat())
                .setInterpolator(optionsInterpolator)
                .setDuration(animDuration)
                .start()
        optionsToggleArrow.animate()
                .rotation(targetRotation)
                .setDuration(animDuration)
                .start()
    }

    override fun onBackPressed() {
        if (isOptionsPanelVisible) {
            toggleOptionsPanel()
        } else {
            super.onBackPressed()
        }
    }

    private fun toggleAnimation() {
        if (renderThread.isPaused) {
            renderThread.resumeRendering()
        } else {
            renderThread.pauseRendering()
        }
        animationStateView.setText(if (renderThread.isPaused) {
            R.string.anim_state_paused
        } else {
            R.string.anim_state_running
        })
    }

    private fun toggleGlowMode() {
        glowToggle.toggle()
        onGlowModeToggled()
    }

    private fun onGenerateNewBallsRequest() {
        currentBalls = generateRandomBalls()
        renderThread.updateRenderer(R.id.request_update_balls) {
            metaballs = currentBalls
        }
    }

    private fun onGlowModeToggled() {
        val isGlowOn = isGlowModeOn()
        renderThread.updateRenderer(R.id.request_update_glow_mode) {
            isGlowEnabled = isGlowOn
        }
    }

    private fun onSelectedColorChanged() {
        val selectedColor = getSelectedColor()
        renderThread.updateRenderer(R.id.request_update_color) {
            ballColor = selectedColor
        }
    }

    private fun generateRandomBalls(): List<Metaball> {
        val dm = resources.displayMetrics
        val maxRadius = Math.min(dm.widthPixels, dm.heightPixels) * 0.20f
        val minRadius = maxRadius * 0.5f
        return (0..ballCountSeekBar.progress)
                .map { random.nextFloat(minRadius, maxRadius) }
                .map { radius -> createMetaballOf(radius) }
                .onEach { ball -> initWithRandomVelocity(ball) }
                .onEach { it.transform = getDimensionTransform() }
                .toList()
    }

    private fun createMetaballOf(radius: Float): Metaball {
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        return Metaball(
                random.nextFloat(radius, width - radius),
                random.nextFloat(radius, height - radius),
                radius)
    }

    private fun initWithRandomVelocity(ball: Metaball) {
        val maxVelocity = resources.displayMetrics.density * 2
        val velocityX = random.nextFloat(-maxVelocity, maxVelocity)
        val velocityY = random.nextFloat(-maxVelocity, maxVelocity)
        ball.setVelocity(velocityX, velocityY)
    }

    override fun updateScene() {
        for (i in 0 until currentBalls.size) {
            val ball = currentBalls[i]
            if (!ball.isInHorizontalBounds(0, metaballView.width)) {
                ball.scaleVelocity(-1, 1)
            }
            if (!ball.isInVerticalBounds(0, metaballView.height)) {
                ball.scaleVelocity(1, -1)
            }
            ball.move()
        }
    }

    private fun isGlowModeOn() = glowToggle.isChecked

    private fun getSelectedColor() = Color.argb(255,
            redComponentSeekBar.progress,
            greenComponentSeekBar.progress,
            blueComponentSeekBar.progress)

    protected abstract fun createRSTarget(): Target

    protected abstract fun invalidateRSTarget()

    protected abstract fun getLayoutRes(): Int

    open protected fun handleProgressChange(id: Int, progress: Int) {

    }

    open protected fun getDimensionTransform() = IDENTITY

    private inner class RefreshTask : Runnable {
        override fun run() {
            invalidateRSTarget()
            mainThreadHandler.postDelayed(this, 30L)
        }
    }

    private fun Random.nextFloat(min: Float, max: Float): Float {
        return min + ((max - min) * nextDouble()).toFloat()
    }
}
