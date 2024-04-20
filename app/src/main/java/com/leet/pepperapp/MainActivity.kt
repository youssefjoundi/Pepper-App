package com.leet.pepperapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.leet.pepperapp.utils.Recorder
import com.leet.pepperapp.utils.Recorder.Companion.TAG
import java.io.File

private const val TAG = "Pepper-App"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200


class MainActivity : AppCompatActivity() {
    private val permissions: Array<String> = arrayOf(android.Manifest.permission.RECORD_AUDIO)


    private lateinit var recordButton: Button
    private lateinit var textView: TextView


    private val recorder: Recorder = Recorder(this)

    private var startRecording: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)





        recordButton = findViewById(R.id.record_button)
        recordButton.setOnClickListener { onclick() }

        textView = findViewById(R.id.my_text)
        textView.movementMethod = ScrollingMovementMethod()




        recorder.setFilePath(getFilePath(this))


    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        if (!permissionToRecordAccepted) {
            Log.e(TAG, "Audio record is disallowed")
            finish()
        }

        Log.i(TAG, "Audio record is permitted")
    }


    private fun onclick() {
        if (!startRecording) {
            recorder.start()
        } else {
            recorder.stop()
        }
        startRecording = !startRecording

        Toast.makeText(this, "Record State $startRecording", Toast.LENGTH_LONG).show()

    }


    private fun getFilePath(context: Context): String? {
        val outfile = File(context.filesDir, RECORDING_FILE_WAV)
        if (!outfile.exists()) {
            Log.d(TAG, "File not found - " + outfile.absolutePath)
        }
        Log.d(TAG, "Returned asset path: " + outfile.absolutePath)
        return outfile.absolutePath
    }


    companion object {

        private const val MODEL_PATH = "whisper_tiny_english_14.tflite"
        private const val VOCAB_PATH = "filters_vocab_en.bin"
        private const val RECORDING_FILE = "recording.mp3"
        private const val RECORDING_FILE_WAV = "recording.wav"
    }


}