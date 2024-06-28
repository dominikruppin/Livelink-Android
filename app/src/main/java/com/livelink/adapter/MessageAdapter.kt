package com.livelink.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.livelink.data.model.Message
import com.livelink.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MessageAdapter(
    private val dataset: List<Message>
): RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = dataset[position]
        val date = Date(item.timestamp)
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(date)
        val username = holder.binding.textViewUsername

        username.text = item.senderId
        holder.binding.textViewMessage.text = item.content
        holder.binding.textViewTimestamp.text = formattedTime

        holder.binding.textViewUsername.setOnClickListener {
            Log.d("Profil", "Du hast das Profil von ${username.text} angeklickt.")
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}