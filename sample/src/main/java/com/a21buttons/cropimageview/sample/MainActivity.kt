package com.a21buttons.cropimageview.sample

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.a21buttons.cropimageview.CropImageView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cropImageView: CropImageView = findViewById(R.id.imageView)
        findViewById<View>(R.id.centerCrop).setOnClickListener {
            if (cropImageView.isCenteredCrop) {
                Toast.makeText(this, "isCenteredCrop", Toast.LENGTH_SHORT).show()
            }
            cropImageView.centerCrop()
        }

        findViewById<View>(R.id.centerInside).setOnClickListener {
            if (cropImageView.isCenteredInside) {
                Toast.makeText(this, "isCenteredInside", Toast.LENGTH_SHORT).show()
            }
            cropImageView.centerInside()
        }

        findViewById<View>(R.id.rotate).setOnClickListener {
            cropImageView.rotate(90f)
        }

        findViewById<View>(R.id.crop).setOnClickListener {
            val bitmap = cropImageView.getCroppedBitmap()()
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
