package com.leet.pepperapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.leet.pepperapp.adapter.RecyclerAdapter
import com.leet.pepperapp.databinding.ActivityMainBinding
import com.leet.pepperapp.model.ChatData
import com.leet.pepperapp.network.remote.ResultApi
import com.leet.pepperapp.utils.RecoderUtlils.Recorder
import com.leet.pepperapp.utils.RecoderUtlils.Recorder.Companion.TAG
import com.leet.pepperapp.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "Pepper-App"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), RobotLifecycleCallbacks {
    private val permissions: Array<String> = arrayOf(android.Manifest.permission.RECORD_AUDIO)

    private val chatAppViewModel: AppViewModel by viewModels()

    private lateinit var recordButton: Button
    private lateinit var messageContainer: LinearLayout



//    private lateinit var textView: TextView

    private lateinit var animation : LottieAnimationView




    private val recorder: Recorder = Recorder(this)


    private var startRecording: Boolean = false


    private lateinit var papperTalk : String

    private lateinit var outpoutFile : File

    @Volatile
    private var pepperSay: Boolean = false



    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val recyclerAdapter: RecyclerAdapter by lazy {
        RecyclerAdapter()
    }


    private lateinit var listChat : MutableList<ChatData>


    @SuppressLint("SetTextI18n", "MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(binding.root)


        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        hideSystemUI()

        listChat = mutableListOf(
            ChatData(
                message = "Hello, I am Pepper , how can I help you",
                type = "PEPPER"
            )
        )


        animation = findViewById(R.id.lottie_id)

//        messageContainer = findViewById(R.id.messageContainer)
//
        recordButton = findViewById(R.id.startButton)
        recordButton.setOnClickListener { onclick() }
//
//        textView = findViewById(R.id.my_text)
//        textView.movementMethod = ScrollingMovementMethod()


        outpoutFile = File( this.filesDir, "recording.webm")




        recorder.setFilePath(getFilePath(application))


        binding.recyclerview.apply {
            adapter = recyclerAdapter
            layoutManager = LinearLayoutManager(
                applicationContext,
                LinearLayoutManager.VERTICAL,
                false
            )
        }


        Log.i(com.leet.pepperapp.TAG, "onCreate: ${listChat.size}")

        recyclerAdapter.setData(listChat)
        binding.recyclerview.smoothScrollToPosition(listChat.size - 1)








        lifecycleScope.launch {
            chatAppViewModel.responseResult.collect { response ->
                when (response) {
                    is ResultApi.InitState -> {
                        Log.i("Hello Init", "Response********")
                    }

                    is ResultApi.Success -> {

                        Log.i("Hello Question:", response.data?.question.toString())
                        Log.i("Hello Success", response.data?.text.toString())

//                        textView.text = "You : ${response.data?.question.toString()} \n\n\n\n " +
//                                "Pepper : ${response.data?.text.toString()}"

//                        addField(response.data?.question.toString())

                        listChat.add(
                            ChatData(
                                message = response.data?.question.toString(),
                                type = "USER"
                            )
                        )

                        recyclerAdapter.setData(listChat)
                        recyclerAdapter.notifyDataSetChanged()

                        binding.recyclerview.smoothScrollToPosition(listChat.size - 1)




                        delay(3000L)

                        listChat.add(
                            ChatData(
                                message = response.data?.text.toString(),
                                type = "PEPPER"
                            )
                        )
                        recyclerAdapter.setData(listChat)
                        recyclerAdapter.notifyDataSetChanged()

                        binding.recyclerview.smoothScrollToPosition(listChat.size - 1)

//                        addField(response.data?.text.toString())



                        papperTalk = response.data?.text.toString()

                        pepperSay = true

                        recordButton.isEnabled = true

                    }

                    is ResultApi.Error -> {
                        response.error?.let { it1 ->
                            Log.i("Hello Error", it1)
                        }
                    }

                    is ResultApi.Listening -> TODO()
                    is ResultApi.Thinking -> {
                        Log.i("Hello Thinking", "Response********")
                        stopAnimation()
                    }
                }
            }
        }
    }





    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
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

        QiSDK.register(this, this)




        Log.i(TAG, "Audio record is permitted")
    }

    private fun showAnimation()  {
        animation.setAnimation("record.json")
        animation.visibility = View.VISIBLE
        animation.playAnimation()
    }



    val stopAnimation : () -> Unit = {
        animation.visibility = View.GONE
        animation.cancelAnimation()
    }





    private fun onclick() {
        showAnimation()

        recordButton.isEnabled = false

        recorder.start(chatAppViewModel)
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
        private const val RECORDING_FILE_WAV = "recording.wav"
    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        val text = "Hello, I am Pepper , how can I help you"


        // Create a new say action.
        val say: Say = SayBuilder.with(qiContext) // Create the builder with the context.
            .withText(text) // Set the text to say.
            .build() // Build the say action.

        say.run()


        while (true) {
            if (pepperSay) {
                val sayAnswer: Say = SayBuilder.with(qiContext)
                    .withText(papperTalk)
                    .build()

                sayAnswer.run()

                recordButton.isEnabled = false


                papperTalk = ""

                pepperSay = false
//                pepperListening = true
            }
        }





    }

    override fun onRobotFocusLost() {
        TODO("Not yet implemented")
    }

    override fun onRobotFocusRefused(reason: String?) {
        TODO("Not yet implemented")
    }

}