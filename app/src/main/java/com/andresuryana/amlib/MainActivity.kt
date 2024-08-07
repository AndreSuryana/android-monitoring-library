package com.andresuryana.amlib

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.andresuryana.amlib.databinding.ActivityMainBinding
import com.andresuryana.amlib.logging.AppLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupButton()
    }

    private fun setupButton() {
        // Setup button click listeners for logging different messages
        binding.btnLogDebug.setOnClickListener {
            AppLogger.d(TAG, "This is a DEBUG message")
        }

        binding.btnLogInfo.setOnClickListener {
            AppLogger.i(TAG, "This is an INFO message")
        }

        binding.btnLogWarn.setOnClickListener {
            AppLogger.w(TAG, "This is a WARNING message")
        }

        binding.btnLogError.setOnClickListener {
            AppLogger.e(TAG, "This is an ERROR message")
        }

        binding.btnLogWtf.setOnClickListener {
            AppLogger.wtf(TAG, "This is a WTF message")
        }

        binding.btnLogErrorWithException.setOnClickListener {
            AppLogger.e(TAG, "This is an ERROR message with exception", Exception("Sample Exception"))
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}