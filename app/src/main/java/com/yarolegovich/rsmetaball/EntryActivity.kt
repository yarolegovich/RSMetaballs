package com.yarolegovich.rsmetaball

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

/**
 * Created by yarolegovich on 2/11/18.
 */
class EntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)

        findViewById<View>(R.id.btn_canvas_metaballs).setOnClickListener {
            launch(MetaballCanvasTargetActivity::class.java)
        }
        findViewById<View>(R.id.btn_surface_metaballs).setOnClickListener {
            launch(MetaballSurfaceTargetActivity::class.java)
        }
    }

    private fun <T : Activity>launch(token: Class<T>) {
        startActivity(Intent(this, token))
    }
}