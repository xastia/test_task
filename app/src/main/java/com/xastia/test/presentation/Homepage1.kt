package com.xastia.test.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xastia.test.R
import com.xastia.test.databinding.ActivityHomepage1Binding

class Homepage1 : AppCompatActivity() {


    companion object {
        private const val PERMISSION_CODE = 16
        private val PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    private fun permissionGranted() = PERMISSION.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private lateinit var binding: ActivityHomepage1Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepage1Binding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.heartButton.setOnClickListener {
            if(permissionGranted()) {
                val intent = Intent(this, Homepage2::class.java)
                startActivity(intent)
                finish()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    PERMISSION,
                    PERMISSION_CODE
                )
            }

        }

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, History::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_CODE) {
            if(permissionGranted()) {
                val intent = Intent(this, Homepage2::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Надайте дозвіл на використання камери щоб продовжити", Toast.LENGTH_SHORT).show()
            }
        }
    }
}