package com.livelink.ui.channels

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.adapter.ChannelsAdapter
import com.livelink.adapter.MessageAdapter
import com.livelink.data.model.ChannelJoin
import com.livelink.data.model.Message
import com.livelink.databinding.FragmentChannelBinding

class ChannelFragment : Fragment() {

    private lateinit var binding: FragmentChannelBinding
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.currentChannel.observe(viewLifecycleOwner) { channel ->
            // Hier channelId verwenden, um Nachrichten abzurufen und anzuzeigen
            channel.channelID.let {
                viewModel.fetchMessages(ChannelJoin(it))
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) {
            Log.d("Chat", "Nachrichten: $it")
            val adapter = MessageAdapter(it)
            binding.recyclerViewMessages.adapter = adapter
        }

        binding.buttonSend.setOnClickListener {
            val text = binding.editTextMessage.text

            if (!text.isNullOrEmpty()) {
                viewModel.sendMessage(
                    viewModel.userData.value.let {
                            Message(
                                it!!.username,
                                binding.editTextMessage.text.toString()
                            )
                    }
                )
                binding.editTextMessage.text?.clear()
            }
        }
    }
}

/*
    private fun observeMessages() {
        viewModel.message.observe(viewLifecycleOwner, Observer { messages ->
            messages?.let {
                messageAdapter.submitList(it)
                binding.recyclerViewMessages.smoothScrollToPosition(0)
            }
        })
    }

    private fun sendMessage() {
        val messageContent = binding.editTextMessage.text.toString().trim()
        if (messageContent.isNotEmpty()) {
            val senderId = viewModel.currentUser.value?.uid ?: ""
            val message = Message(senderId, messageContent, System.currentTimeMillis())
            viewModel.sendMessage(message)
            binding.editTextMessage.text.clear()
        }
    }
}

    }

}*/