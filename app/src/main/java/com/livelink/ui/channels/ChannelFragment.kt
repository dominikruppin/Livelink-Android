package com.livelink.ui.channels

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.livelink.SharedViewModel
import com.livelink.adapter.MessageAdapter
import com.livelink.data.model.ChannelJoin
import com.livelink.data.model.Message
import com.livelink.databinding.FragmentChannelBinding
import coil.load
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch


// Fragment welches die Nachrichten des aktuellen Channels anzeigt, außerdem ein Eingabefeld und Button um
// Nachrichten an den aktuellen Channel zu senden

class ChannelFragment : Fragment() {

    private lateinit var binding: FragmentChannelBinding
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: MessageAdapter

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

        // Wir binden hier bereits den Adapter, mit einer Funktion um Profile zu öffnen
        adapter = MessageAdapter(emptyList()) { clickedUser ->
            viewModel.openProfile(clickedUser)
        }
        binding.recyclerViewMessages.adapter = adapter

        // Aktuellen Channel abrufen
        viewModel.currentChannel.observe(viewLifecycleOwner) { channel ->
            Log.d("Channel", "Jointime: ${channel.timestamp}")
            // Wenn der aktuelle Channel nicht null ist...
            channel.channelID.let {
                // .. starten wir das abrufen der Nachrichten des Channels
                viewModel.fetchMessages(channel)
                val backgroundUrl = channel.backgroundURL
                if (backgroundUrl.isNotEmpty()) {
                    binding.imageViewChannelBackground.load(channel.backgroundURL)
                }
            }
        }

        // Wir beobachten ob es neue Nachrichten gibt und updaten dann den Adapter
        // Außerdem scrollen wir immer nach unten, so läuft der Chatverlauf mit
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d("Chat", "Nachrichten: $messages")
            adapter.updateMessages(messages)
            scrollToBottom()
        }

        // Button zum senden der Nachricht an den Channel
        binding.buttonSend.setOnClickListener {
            // Eingegebene Nachricht
            val text = binding.editTextMessage.text

            // Wir prüfen ob Text leer ist oder null
            if (!text.isNullOrEmpty()) {
                // Wenn der Nutzer eine Nachricht(alias Command) eingibt, der mit /profil beginnt...
                if (text.startsWith("/profil")) {
                    // .. holen wir uns nach dem Leerzeichen den Usernamen
                    val username = text.split(" ").getOrNull(1)
                    // Wenn der username nicht leer ist..
                    if (!username.isNullOrEmpty()) {
                        // .. öffnen wir das Profil des eingegebenen Users
                        viewModel.openProfile(username.lowercase())
                    }
                    // Eingabezeile leeren damit ready für neue Nachricht
                    binding.editTextMessage.text?.clear()
                } else {
                    // Wir senden die Nachricht, dazu erstellen wir ein Message-Objekt mit den benötigten Infos (username, nachricht)
                    viewModel.sendMessage(
                        // Wir holen uns hier noch fix über die userData den Usernamen des sendenen Nutzers und aus der Eingabezeile den Text
                        viewModel.userData.value.let {
                            Message(
                                it!!.username,
                                binding.editTextMessage.text.toString()
                            )
                        }
                    )
                }
                // Wir leeren wieder die Eingabezeile
                binding.editTextMessage.text?.clear()
            }
        }
    }

    private fun scrollToBottom() {
        binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)
    }
}