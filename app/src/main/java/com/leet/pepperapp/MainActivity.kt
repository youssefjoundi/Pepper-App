package com.leet.pepperapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.actuation.Animation
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.aldebaran.qi.sdk.`object`.human.Human
import com.aldebaran.qi.sdk.`object`.humanawareness.HumanAwareness
import com.leet.pepperapp.adapter.RecyclerAdapter
import com.leet.pepperapp.databinding.ActivityMainBinding
import com.leet.pepperapp.model.ChatData
import com.leet.pepperapp.network.remote.ResultApi
import com.leet.pepperapp.utils.RecoderUtlils.Recorder
import com.leet.pepperapp.utils.RecoderUtlils.Recorder.Companion.TAG
import com.leet.pepperapp.utils.resource
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


    private lateinit var recycleview : RecyclerView
    private lateinit var recordButton: Button
//    private lateinit var textView: TextView
    private lateinit var animation : LottieAnimationView




    private lateinit var dialog : Dialog
    private lateinit var inflater : LayoutInflater
    private lateinit var dialogLayout : View





    private val recorder: Recorder = Recorder(this)
    private lateinit var papperTalk : String

    @Volatile
    private var pepperSay: Boolean = false

    @Volatile
    private var pepperThink: Boolean = false

    @Volatile
    private var animationStarted: Boolean = false

    @Volatile
    private var sayError: Boolean = false


    @Volatile
    private var animationFile: Int = 0


    // Detect Human
    // Store the HumanAwareness service.
    private var humanAwareness: HumanAwareness? = null
    // The QiContext provided by the QiSDK.
    private var qiContext: QiContext? = null



    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val recyclerAdapter: RecyclerAdapter by lazy {
        RecyclerAdapter()
    }


    private lateinit var listChat : MutableList<ChatData>


    @SuppressLint("SetTextI18n", "MissingInflatedId")
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
        recordButton = findViewById(R.id.startButton)
        recordButton.setOnClickListener { onclick() }
        recorder.setFilePath(getFilePath(application))

        binding.recyclerview.apply {
            adapter = recyclerAdapter
            layoutManager = LinearLayoutManager(
                applicationContext,
                LinearLayoutManager.VERTICAL,
                false
            )
        }


        recyclerAdapter.setData(listChat)
        binding.recyclerview.smoothScrollToPosition(listChat.size - 1)

        binding.recyclerview.layoutManager



        lifecycleScope.launch {
            chatAppViewModel.responseResult.collect { response ->
                when (response) {
                    is ResultApi.InitState -> {
                        Log.i("Hello Init", "Response********")
                    }

                    is ResultApi.Success -> {
                        stopAnimation()

                        Log.i("Hello Question:", response.data?.question.toString())
                        Log.i("Hello Success", response.data?.text.toString())

//                        textView.text = "You : ${response.data?.question.toString()} \n\n\n\n " +
//                                "Pepper : ${response.data?.text.toString()}"


                        addMessage(
                            message = response.data?.question.toString(),
                            type = "USER"
                        )

                        delay(1000L)


                        addMessage(
                            message = response.data?.text.toString(),
                            type = "PEPPER"
                        )


                        papperTalk = response.data?.text.toString()

                        pepperSay = true


                    }

                    is ResultApi.Error -> {

                        delay(3000L)

//                        animationFile = R.raw.error_animation
//                        animationStarted = true

                        response.error?.let { it1 ->
                            Log.i("Hello Error", it1)
                        }


                        showAnimation("error.json")

                        papperTalk = resource.pepperErrorMsg
                        sayError = true
//                        pepperSay = true

                    }

                    is ResultApi.Listening -> {
                        animationFile = R.raw.listen_animation
                        animationStarted = true
                    }

                    is ResultApi.Thinking -> {
                        Log.i("Hello Thinking", "Response********")
                        stopAnimation()
                        papperTalk = "mmm"
                        pepperThink = true
                        animationFile = R.raw.thinking_animation
                        animationStarted = true
                    }

                    is ResultApi.Done -> {
                        Log.i("Hello Done", "Pepper Done Talking")
                        runOnUiThread {
                            hideSystemUI()
                            dialog.dismiss()
                            stopAnimation()
                            disableAndEnableUiWhenPepperTalking(true)

                        }

                    }
                }
            }
        }

        recycleview = findViewById(R.id.recyclerview)



        dialog = Dialog(this)
        inflater = layoutInflater
        dialogLayout = inflater.inflate(R.layout.pepper_talk, null)

        dialog.setContentView(dialogLayout)

    }


    private fun ShowDialogTalk(message : String) {


        val PepperTalk: TextView = dialogLayout.findViewById(R.id.pepper_talk)

        val scrollview : ScrollView = dialogLayout.findViewById(R.id.scrollView)

        PepperTalk.text = ""



        var time = 0L



        message.forEach { char ->

            Handler(Looper.getMainLooper()).postDelayed({
                PepperTalk.append(char.toString())

                scrollview.post {
                    scrollview.fullScroll(View.FOCUS_DOWN)
                }

            }, time)

            time += 20
        }

//
//        PepperTalk.setCharacterDelay(35)
//
//        PepperTalk.animateText("messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "messagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessagemessage" +
//                "")




        dialog.window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }








    @SuppressLint("NotifyDataSetChanged")
    private fun addMessage(message : String, type : String) {

        listChat.add(
            ChatData(
                message = message,
                type = type
            )
        )

        recyclerAdapter.setData(listChat)
        recyclerAdapter.notifyDataSetChanged()

        binding.recyclerview.smoothScrollToPosition(listChat.size - 1)

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

    private fun showAnimation(file : String)  {
        animation.setAnimation(file)
        animation.visibility = View.VISIBLE
        animation.playAnimation()
    }



    val stopAnimation : () -> Unit = {
        animation.visibility = View.GONE
        animation.cancelAnimation()
    }


    private fun disableAndEnableUiWhenPepperTalking(a : Boolean) {
        recordButton.isEnabled = a
        recordButton.isClickable = a
    }



    private fun onclick() {

//        ShowDialogTalk("sdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwer" +
//                "sdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwer" +
//                "sdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwer" +
//                "sdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwer" +
//                "sdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwer" +
//                "sdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwersdfs sdfs sdfs sdfs sdfs sdfs sdfs sdfs ewewrwerwerwerwer" +
//                "")

        showAnimation("record.json")

        chatAppViewModel.pepperState("listen")


        recordButton.isEnabled = false


        disableAndEnableUiWhenPepperTalking(false)


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


    private fun startAnimation(animationFile : Int, qiContext1: QiContext) {


        val animation: Animation = AnimationBuilder.with(qiContext1)
            .withResources(animationFile)
            .build()


        // Create an animate action.
        val animate = AnimateBuilder.with(qiContext1) // Create the builder with the context.
            .withAnimation(animation) // Set the animation.
            .build() // Build the animate action.


        animate.async().run()

        this.animationFile = 0

    }






    override fun onRobotFocusGained(qiContext: QiContext?) {


        // Store the provided QiContext.
        this.qiContext = qiContext

        // Get the HumanAwareness service from the QiContext.
        qiContext?.let {
            humanAwareness = it.humanAwareness
        }


        val human = findHumansAround()

        human?.andThenConsume { humansAround -> Log.i(TAG, "${humansAround.size} human(s) around.") }

        // TODO
        // continue when the humansAround is > 0



        val text = "Hello, I am Pepper , how can I help you"


        // Create a new say action.
        val say: Say = SayBuilder.with(qiContext) // Create the builder with the context.
            .withText(text) // Set the text to say.
            .build() // Build the say action.

        say.run()

        runOnUiThread {
//            recordButton.isEnabled = true
            disableAndEnableUiWhenPepperTalking(true)
        }


        while (true) {


            if (sayError) {
                startAnimation(R.raw.error_animation, qiContext!!)

                val sayAnswer: Say? = SayBuilder.with(qiContext)
                    .withText(papperTalk)
                    .build()
                sayAnswer?.run()
                papperTalk = ""
                pepperSay = false
                chatAppViewModel.pepperState("done")


                sayError = false
            }


            if (animationStarted) {
                startAnimation(animationFile, qiContext!!)
                animationStarted = false
            }


            if (pepperThink) {
                val sayAnswer: Say? = SayBuilder.with(qiContext)
                    .withText(papperTalk)
                    .build()
                sayAnswer?.async()?.run()
                papperTalk = ""
                pepperThink = false
            }

            if (pepperSay) {

                if (animationFile == 0) {
                    runOnUiThread {
                        ShowDialogTalk(papperTalk)
                    }
                }


                val sayAnswer: Say? = SayBuilder.with(qiContext)
                    .withText(papperTalk)
                    .build()
                sayAnswer?.run()
                papperTalk = ""
                pepperSay = false
                chatAppViewModel.pepperState("done")
            }
        }
    }



    override fun onRobotFocusLost() {
        this.qiContext = null

    }


    private fun findHumansAround(): Future<List<Human>>? {
        // Get the humans around the robot.
        val humansAroundFuture: Future<List<Human>>? = humanAwareness?.async()?.humansAround
        val num = humansAroundFuture


        return num
    }




    override fun onRobotFocusRefused(reason: String?) {
        TODO("Not yet implemented")
    }

}