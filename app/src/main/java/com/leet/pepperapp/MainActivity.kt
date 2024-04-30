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
import com.airbnb.lottie.LottieAnimationView
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import com.aldebaran.qi.sdk.builder.GoToBuilder
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder
import com.aldebaran.qi.sdk.builder.LocalizeBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.builder.TransformBuilder
import com.aldebaran.qi.sdk.`object`.actuation.Actuation
import com.aldebaran.qi.sdk.`object`.actuation.Animation
import com.aldebaran.qi.sdk.`object`.actuation.ExplorationMap
import com.aldebaran.qi.sdk.`object`.actuation.Frame
import com.aldebaran.qi.sdk.`object`.actuation.FreeFrame
import com.aldebaran.qi.sdk.`object`.actuation.GoTo
import com.aldebaran.qi.sdk.`object`.actuation.LocalizationStatus
import com.aldebaran.qi.sdk.`object`.actuation.Localize
import com.aldebaran.qi.sdk.`object`.actuation.LocalizeAndMap
import com.aldebaran.qi.sdk.`object`.actuation.Mapping
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.aldebaran.qi.sdk.`object`.geometry.Transform
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

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), RobotLifecycleCallbacks {
    private val permissions: Array<String> = arrayOf(android.Manifest.permission.RECORD_AUDIO)

    private val chatAppViewModel: AppViewModel by viewModels()


    private lateinit var recordButton: Button
//    private lateinit var textView: TextView
    private lateinit var animation : LottieAnimationView




    private lateinit var dialog : Dialog
    private lateinit var inflater : LayoutInflater
    private lateinit var dialogLayout : View





    private val recorder: Recorder = Recorder(this)
    private var papperTalk : String = ""

    @Volatile
    private var pepperSay: Boolean = false

    // Store the GoTo action.
    private var goTo: GoTo? = null

    @Volatile
    private var pepperThink: Boolean = false

    @Volatile
    private var animationStarted: Boolean = false

    @Volatile
    private var sayError: Boolean = false

    @Volatile
    private var startDetectpeople: Boolean = false

    @Volatile
    private var movePepper: Boolean = false


    private var previousSize : Int = 0


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


    @SuppressLint("SetTextI18n", "MissingInflatedId", "InflateParams")
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
//                        animationFile = R.raw.listen_animation_2
//                        animationStarted = true
                    }

                    is ResultApi.Thinking -> {
                        Log.i("Hello Thinking", "Response********")
                        stopAnimation()
                        showAnimation("think_animation.json")
                        papperTalk = "mmm"
                        pepperThink = true
                        animationFile = R.raw.thinking_animation
                        animationStarted = true
                    }

                    is ResultApi.Done -> {
                        Log.i("Hello Done", "Pepper Done Talking")
                        hideSystemUI()
                        dialog.dismiss()
                        stopAnimation()
                        disableAndEnableUiWhenPepperTalking(true)
                        if (papperTalk.isNotEmpty()) {
                            addMessage(
                                message = papperTalk,
                                type = "PEPPER"
                            )
                        }
                        papperTalk = ""
                        delay(2000L)
                        startDetectpeople = false

                    }

                    is ResultApi.Move -> {
                        movePepper = true
                        delay(4000L)
                        movePepper = false
                    }
                }
            }
        }

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

        startDetectpeople = true


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


    private fun moveArround(qiContext: QiContext?) {
        // Get the Actuation service from the QiContext.
        val actuation: Actuation? = qiContext?.actuation

        // Get the robot frame.
        val robotFrame: Frame? = actuation?.robotFrame()

        // Create a transform corresponding to a 1 meter forward translation.
        val transform: Transform = TransformBuilder.create().from2DTransform(1.0, 1.0, 2.0)
        // Get the Mapping service from the QiContext.
        val mapping: Mapping? = qiContext?.mapping

        // Create a FreeFrame with the Mapping service.
        val targetFrame: FreeFrame? = mapping?.makeFreeFrame()

        // Update the target location relatively to Pepper's current location.
        if (targetFrame != null) {
            targetFrame.update(robotFrame, transform, 0L)
        }

        if (targetFrame != null) {
            goTo = GoToBuilder.with(qiContext) // Create the builder with the QiContext.
                .withFrame(targetFrame.frame()) // Set the target frame.
                .build()
        } // Build the GoTo action.

        goTo?.addOnStartedListener { Log.i(TAG, "GoTo action started.") }
        // Execute the GoTo action asynchronously.
//        val goToFuture: Future<Void>? = goTo?.async()?.run()
        val goToFuture: Future<Void>? = goTo?.async()?.run()


        // Add a lambda to the action execution.
        goToFuture?.thenConsume { future ->
            if (future.isSuccess) {
                Log.i(TAG, "GoTo action finished with success.")
            } else if (future.hasError()) {
                Log.e(TAG, "GoTo action finished with error.", future.error)
            }
        }

    }





    override fun onRobotFocusGained(qiContext: QiContext?) {



        this.qiContext = qiContext

        qiContext?.let {
            humanAwareness = it.humanAwareness
        }

        if (qiContext != null) {
            startMapping(qiContext)
        }

        // Get the robot frame.


        var humansAround: MutableList<Human>? = humanAwareness?.humansAround

        humansAround?.let {
            Log.i(TAG, "Humans Around ${it.size}")
            previousSize = it.size
        }




        val text = "Hello, I am Pepper , how can I help you"

        val say: Say = SayBuilder.with(qiContext)
            .withText(text)
            .build()

        say.run()

        runOnUiThread {
            disableAndEnableUiWhenPepperTalking(true)
        }


        while (true) {


            if (!startDetectpeople)
            {
                humansAround = humanAwareness?.humansAround
                humansAround?.let {
//                Log.i(TAG, "Humans arounds : ${it.size} , previous : ${previousSize}")
                    if (it.size > 0 && previousSize == 0) {
//                        // Remove on status changed listeners from the LocalizeAndMap action.
//                        localizeAndMap.removeAllOnStatusChangedListeners()
//
//                        // Remove on status changed listeners from the Localize action.
//                        localize.removeAllOnStatusChangedListeners()



                        goTo?.removeAllOnStartedListeners()

                        val sayAnswer: Say? = SayBuilder.with(qiContext)
                            .withText("Hello I am pepper how can I help you")
                            .build()
                        sayAnswer?.run()
                        chatAppViewModel.pepperState("done")
                    }
                    if (it.size == 0) {

//                        if (qiContext != null) {
//                            startMapping(qiContext)
//                        }

                        runOnUiThread {
                            disableAndEnableUiWhenPepperTalking(false)
                        }
                        chatAppViewModel.pepperState("move")
                        if (movePepper)
                            moveArround(qiContext)
                    }
                    previousSize = it.size
                }
            }

            if (sayError) {
                startAnimation(R.raw.error_animation, qiContext!!)

                val sayAnswer: Say? = SayBuilder.with(qiContext)
                    .withText(papperTalk)
                    .build()
                sayAnswer?.run()
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
//                    .withBodyLanguageOption(BodyLanguageOption.DISABLED)
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
                pepperSay = false
                chatAppViewModel.pepperState("done")
            }
        }
    }



    override fun onRobotFocusLost() {
        this.qiContext = null
        // Remove on started listeners from the GoTo action.
        goTo?.removeAllOnStartedListeners()

        // Remove on status changed listeners from the LocalizeAndMap action.
        localizeAndMap.removeAllOnStatusChangedListeners()

        // Remove on status changed listeners from the Localize action.
        localize.removeAllOnStatusChangedListeners()

    }

    // Store the LocalizeAndMap action.
    private lateinit var localizeAndMap: LocalizeAndMap
    // Store the map.
    private var explorationMap: ExplorationMap? = null
    // Store the LocalizeAndMap execution.
    private lateinit var localizationAndMapping: Future<Void>
    // Store the Localize action.
    private lateinit var localize: Localize

    private fun startMapping(qiContext: QiContext) {
        // Create a LocalizeAndMap action.
        localizeAndMap = LocalizeAndMapBuilder.with(qiContext).build()

        // Add an on status changed listener on the LocalizeAndMap action for the robot to say when he is localized.
        localizeAndMap.addOnStatusChangedListener {
            if (it == LocalizationStatus.LOCALIZED) {
                // Dump the ExplorationMap.
                explorationMap = localizeAndMap.dumpMap()

                val message = "Robot has mapped his environment."
                Log.i(TAG, message)

                // Cancel the LocalizeAndMap action.
                localizationAndMapping.requestCancellation()
            }
        }

        val message = "Mapping..."
        Log.i(TAG, message)

        // Execute the LocalizeAndMap action asynchronously.
        localizationAndMapping = localizeAndMap.async().run()

        // Add a lambda to the action execution.
        localizationAndMapping.thenConsume {
            if (it.hasError()) {
                val errorMessage = "LocalizeAndMap action finished with error."
                Log.e(TAG, errorMessage, it.error)
            } else if (it.isCancelled) {
                startLocalizing(qiContext)
            }
        }
    }

    private fun startLocalizing(qiContext: QiContext) {
        // Create a Localize action.
        localize = LocalizeBuilder.with(qiContext)
            .withMap(explorationMap)
            .build()

        // Add an on status changed listener on the Localize action for the robot to say when he is localized.
        localize.addOnStatusChangedListener {
            if (it == LocalizationStatus.LOCALIZED) {
                val message = "Robot is localized."
                Log.i(TAG, message)

            }
        }

        val message = "Localizing..."
        Log.i(TAG, message)

        // Execute the Localize action asynchronously.
        val localization = localize.async().run()

        // Add a lambda to the action execution.
        localization?.thenConsume {
            if (it.hasError()) {
                val errorMessage = "Localize action finished with error."
                Log.e(TAG, errorMessage, it.error)
            }
        }
    }


    override fun onRobotFocusRefused(reason: String?) {


    }

}