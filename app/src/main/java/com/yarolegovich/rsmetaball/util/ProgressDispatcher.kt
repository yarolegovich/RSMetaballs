package com.yarolegovich.rsmetaball.util

import android.app.Activity
import android.support.annotation.IdRes
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar

/**
 * Created by yarolegovich on 2/10/18.
 */
class ProgressDispatcher(private val action: (Payload) -> Unit) : SeekBar.OnSeekBarChangeListener {

    fun attachToAllSeekBarsIn(activity: Activity) {
        val content = activity.findViewById<View>(android.R.id.content)
        attachToSeekBarsRecursive(content)
    }

    private fun attachToSeekBarsRecursive(view: View) {
        if (view is ViewGroup) {
            (0 until view.childCount)
                    .map { index -> view.getChildAt(index) }
                    .forEach { attachToSeekBarsRecursive(it) }
        } else if (view is SeekBar) {
            view.setOnSeekBarChangeListener(this)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        action(Payload(seekBar.id, seekBar.progress))
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    class Payload(@IdRes val id: Int, val progress: Int)
}