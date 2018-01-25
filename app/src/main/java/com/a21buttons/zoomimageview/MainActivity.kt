package com.a21buttons.zoomimageview

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.button).setOnClickListener {
            val bitmap = findViewById<CropImageView>(R.id.imageView).getCoppedBitmap()()
            if (bitmap != null) {
                val imageView = ImageView(this)
                imageView.setImageBitmap(bitmap)
                AlertDialog.Builder(this)
                        .setView(imageView)
                        .show()
            }
        }
    }
}
