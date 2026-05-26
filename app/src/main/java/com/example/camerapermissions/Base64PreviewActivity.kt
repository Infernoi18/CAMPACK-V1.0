package com.example.camerapermissions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class Base64PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base64_preview)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val base64Text: TextInputEditText = findViewById(R.id.base64_text)
        val copyButton: MaterialButton = findViewById(R.id.copy_button)
        val progressBar: ProgressBar = findViewById(R.id.loading_progress)

        progressBar.visibility = View.VISIBLE
        
        // Use a thread to avoid blocking the UI while loading potentially large string
        Thread {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val base64 = prefs.getString(KEY_LAST_BASE64, null)

            runOnUiThread {
                progressBar.visibility = View.GONE
                if (base64 != null) {
                    base64Text.setText(base64)

                    copyButton.setOnClickListener {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Base64 Photo", base64)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    base64Text.setText("No Base64 data available")
                    copyButton.isEnabled = false
                }
            }
        }.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val PREFS_NAME = "base64_store"
        const val KEY_LAST_BASE64 = "last_base64"
    }
}
