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

        val imageView: CropImageView = findViewById(R.id.imageView)
        findViewById<View>(R.id.rotate).setOnClickListener {
            imageView.rotate(90f)
        }

        findViewById<View>(R.id.crop).setOnClickListener {
            val bitmap = imageView.getCoppedBitmap()()
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
