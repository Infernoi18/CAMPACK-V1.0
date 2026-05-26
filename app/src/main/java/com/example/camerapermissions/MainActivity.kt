package com.example.camerapermissions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.camerapermissions.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var imageView: ImageView
    private lateinit var imageUri: Uri
    private var lastImageFile: File? = null
    private lateinit var showBase64Button: Button

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                statusText.text = "Permission Status: Granted"
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                statusText.text = "Permission Status: Denied"
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                imageView.setImageURI(imageUri)
                Toast.makeText(this, "Image Saved Successfully", Toast.LENGTH_SHORT).show()
                showBase64Button.visibility = View.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        imageView = findViewById(R.id.image_view)
        showBase64Button = findViewById(R.id.show_base64_btn)

        val requestBtn: Button = findViewById(R.id.request_btn)
        val captureBtn: Button = findViewById(R.id.capture_btn)

        updatePermissionStatus()

        requestBtn.setOnClickListener {
            requestPermission()
        }

        captureBtn.setOnClickListener {
            if (hasPermission()) {
                showBase64Button.visibility = View.GONE
                openCamera()
            } else {
                Toast.makeText(this, "Please grant permission first", Toast.LENGTH_SHORT).show()
            }
        }

        showBase64Button.setOnClickListener {
            openBase64Preview()
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePermissionStatus() {
        if (hasPermission()) {
            statusText.text = "Permission Status: Granted"
        } else {
            statusText.text = "Permission Status: Not Granted"
        }
    }

    private fun requestPermission() {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val file = createImageFile()
        lastImageFile = file
        imageUri = FileProvider.getUriForFile(
            this,
            "com.example.camerapermissions.provider",
            file
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        cameraLauncher.launch(intent)
    }

    private fun openBase64Preview() {
        val imageFile = lastImageFile
        if (imageFile == null || !imageFile.exists()) {
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            return
        }

        showBase64Button.isEnabled = false
        statusText.text = "Status: Converting to Base64..."

        Thread {
            try {
                // Read bytes
                val bytes = imageFile.readBytes()
                
                // Encode to Base64
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                
                // Save to SharedPreferences
                val prefs = getSharedPreferences(Base64PreviewActivity.PREFS_NAME, MODE_PRIVATE)
                prefs.edit().putString(Base64PreviewActivity.KEY_LAST_BASE64, base64).apply()
                
                val intent = Intent(this, Base64PreviewActivity::class.java)
                runOnUiThread {
                    showBase64Button.isEnabled = true
                    statusText.text = "Status: Ready"
                    startActivity(intent)
                }
            } catch (e: OutOfMemoryError) {
                runOnUiThread {
                    showBase64Button.isEnabled = true
                    statusText.text = "Status: Error (Image too large)"
                    Toast.makeText(this, "Image is too large for Base64 conversion. Try a smaller photo.", Toast.LENGTH_LONG).show()
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    showBase64Button.isEnabled = true
                    statusText.text = "Status: Error"
                    Toast.makeText(this, "Failed to process image: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val storageDir = getExternalFilesDir(null)
        return File(storageDir, fileName)
    }
}
