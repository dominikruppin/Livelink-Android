package com.livelink.ui.channels

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.livelink.SharedViewModel
import com.livelink.adapter.MessageAdapter
import com.livelink.data.model.ChannelJoin
import com.livelink.data.model.Message
import com.livelink.databinding.FragmentChannelBinding

// Fragment welches die Nachrichten des aktuellen Channels anzeigt, außerdem ein Eingabefeld und Button um
// Nachrichten an den aktuellen Channel zu senden

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

        // Aktuellen Channel abrufen
        viewModel.currentChannel.observe(viewLifecycleOwner) { channel ->
            // Wenn der aktuelle Channel nicht null ist...
            channel.channelID.let {
                // .. starten wir das abrufen der Nachrichten des Channels
                viewModel.fetchMessages(ChannelJoin(it))
            }
        }

        // Die Nachrichten des Channels die wir laden landen in der LiveData die wir hier beobachten
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d("Chat", "Nachrichten: $messages")
            // Wir laden die empfangenen in den Adapter und definieren eine Funktion
            // falls jemand den Nutzernamen eines Users anklickt
            val adapter = MessageAdapter(messages) { clickedUser ->
                // Wurde ein Nutzername angeklickt, öffnen wir das Profil des angeklickten Users
                viewModel.openProfile(clickedUser)
            }
            // Nachrichten an die Recyclerview binden, ergo Nachrichten anzeigen
            binding.recyclerViewMessages.adapter = adapter
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
}