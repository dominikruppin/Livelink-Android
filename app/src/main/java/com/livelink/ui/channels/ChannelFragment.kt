package com.livelink.ui.channels

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.livelink.R
import com.livelink.SharedViewModel
import com.livelink.adapter.ChannelsAdapter
import com.livelink.adapter.MessageAdapter
import com.livelink.data.UserData
import com.livelink.data.model.ChannelJoin
import com.livelink.data.model.Message
import com.livelink.databinding.FragmentChannelBinding
import com.livelink.databinding.PopupProfileBinding

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
            channel.channelID.let {
                viewModel.fetchMessages(ChannelJoin(it))
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) {
            Log.d("Chat", "Nachrichten: $it")
            val adapter = MessageAdapter(it) {
                viewModel.openProfile(it)
            }
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

        viewModel.profileUserData.observe(viewLifecycleOwner) { userData ->
            Log.d("Profile", "$userData")
            userData?.let { showProfilePopup(it) }
        }
    }

    private fun showProfilePopup(userData: UserData) {
        val popupBinding = PopupProfileBinding.inflate(layoutInflater)
        val popupView = popupBinding.root



        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)

        popupBinding.btnClose.setOnClickListener {
            popupWindow.dismiss()
        }

        popupBinding.textViewUsername.text = userData.username
        if (userData.profilePicURL.isNotEmpty()) {
            popupBinding.imageViewProfilePic.load(userData.profilePicURL)
        }




    }



    private fun getStatusText(status: Int): String {
        return when (status) {
            0 -> "Mitglied"
            6 -> "Admin"
            11 -> "Sysadmin"
            else -> "Unbekannter Status"
        }
    }
}
