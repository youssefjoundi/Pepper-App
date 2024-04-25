package com.leet.pepperapp.adapter

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.leet.pepperapp.databinding.PepperItemBinding
import com.leet.pepperapp.databinding.UserItemBinding
import com.leet.pepperapp.model.ChatData
import com.leet.pepperapp.viewmodel.AppViewModel

class RecyclerAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val listOfChat = mutableListOf<ChatData>()


    companion object {
        const val VIEW_TYPE_PEPPER = 0
        const val VIEW_TYPE_USER = 1
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PEPPER -> PepperViewHolder(
                PepperItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            VIEW_TYPE_USER -> UserViewHolder(
                UserItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = listOfChat.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (listOfChat[position].type == "USER")
            (holder as UserViewHolder).bind(listOfChat[position])
        else
            (holder as PepperViewHolder).bind(listOfChat[position], position)
    }

    override fun getItemViewType(position: Int): Int {
//        return super.getItemViewType(position)
        return if (listOfChat[position].type == "USER") VIEW_TYPE_USER else VIEW_TYPE_PEPPER
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<ChatData>) {
        listOfChat.clear()
        listOfChat.addAll(list)
        notifyDataSetChanged()
    }



    inner class PepperViewHolder(private val binding: PepperItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatData, position: Int) {

            binding.apply {
                chat.also {
                    pepperText.text = ""

//                    if (position == getData().size - 1 && isInsert)
//                    {
//
//                        Log.i("Hello from check", "check : ${pepperText.maxWidth}")
//                        Log.i("Hello from check", "check : ${pepperText.paint.measureText(pepperText.text.toString())}")
//
//
//                        var time = 0L
//
//
//
//
//                        chat.message.forEach { char ->
//
//
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                pepperText.append(char.toString())
//                                tmp += char.toString()
//                                val size = pepperText.paint.measureText(tmp + 30)
//                                if (size > pepperText.maxWidth) {
//                                    tmp = ""
//                                    // The text exceeds the maximum width
//                                    Log.i("Hello from check", "check")
//
//                                    chatAppviewModel?.pepperState("listen")
//
//                                }
//                            }, time)
//
//                            time += 30
//                        }
//
//
//                        isInsert = false
//                    }
//                    else {
                        pepperText.text = chat.message
//                    }

                }
            }
        }
    }


    inner class UserViewHolder(private val binding: UserItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatData) {
            binding.apply {
                chat.also {
                    textGchatMessageMe.text = chat.message
                }
            }
        }
    }
}