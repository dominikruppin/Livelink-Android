package com.livelink.ui.channels

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.livelink.SharedViewModel
import com.livelink.adapter.MessageAdapter
import com.livelink.data.model.Message
import com.livelink.databinding.FragmentChannelBinding
import coil.load
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.livelink.R
import com.livelink.adapter.OnlineUserAdapter
import kotlinx.coroutines.delay


// Fragment welches die Nachrichten des aktuellen Channels anzeigt, außerdem ein Eingabefeld und Button um
// Nachrichten an den aktuellen Channel zu senden

class ChannelFragment : Fragment() {

    private lateinit var binding: FragmentChannelBinding
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var onlineUserAdapter: OnlineUserAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationViewOnlineUsers: NavigationView

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
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        navigationViewOnlineUsers = requireActivity().findViewById(R.id.nav_view_online_users)
        drawerLayout.setScrimColor(Color.TRANSPARENT)

        // Actionmenü im Fragment ausblenden
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Menü leeren, um sicherzustellen, dass keine Elemente vorhanden sind
                menu.clear()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Keine Menüelemente, nichts zutun (juhu)
                return false
            }
        }, viewLifecycleOwner)

        // Höhe der StatusBar und ActionBar ermitteln
        val statusBarHeight = getStatusBarHeight()
        val actionBarHeight = getActionBarHeight()

        // Gesamt marginTop berechnen
        val totalMarginTop = statusBarHeight + actionBarHeight

        // marginTop für das NavigationView setzen
        val layoutParams = navigationViewOnlineUsers.layoutParams as DrawerLayout.LayoutParams
        layoutParams.topMargin = totalMarginTop
        navigationViewOnlineUsers.layoutParams = layoutParams

        // Drawer Menü für die Darstellung der OnlineUser erstellen
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_channel, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_show_online_users -> {
                        drawerLayout.openDrawer(GravityCompat.END)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)


        // Wir binden hier bereits den Adapter, mit einer Funktion um Profile zu öffnen
        messageAdapter = MessageAdapter(emptyList()) { clickedUser ->
            viewModel.openProfile(clickedUser)
        }
        // Binden den Adapter für die OnlineUser, inklusive Funktion falls man einen
        // OnlineUser anklickt
        onlineUserAdapter = OnlineUserAdapter(emptyList()) { clickedUser ->
            viewModel.openProfile(clickedUser)
        }

        binding.recyclerViewMessages.adapter = messageAdapter
        val recyclerViewOnlineUsers = navigationViewOnlineUsers.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view_online_users)
        recyclerViewOnlineUsers.adapter = onlineUserAdapter

        // Aktuellen Channel abrufen
        viewModel.currentChannel.observe(viewLifecycleOwner) { channel ->
            Log.d("Channel", "Jointime: ${channel.timestamp}")
            // Wenn der aktuelle Channel nicht null ist...
            channel.channelID.let {
                // .. starten wir das abrufen der Nachrichten des Channel
                viewModel.fetchMessages(channel)
                viewModel.addOrUpdateOnlineUserData()
                sendOnlineStatus()
                val backgroundUrl = channel.backgroundURL
                if (backgroundUrl.isNotEmpty()) {
                    binding.imageViewChannelBackground.load(channel.backgroundURL)
                }
            }
        }

        // LiveData der Online User beobachten
        viewModel.onlineUsers.observe(viewLifecycleOwner) { onlineUsers ->
            // Neue Liste an den Adapter übergeben
            onlineUserAdapter.updateOnlineUsers(onlineUsers.sortedBy { it.joinTimestamp as Timestamp})
            Log.d("Channel", "OnlineUser geladen: $onlineUsers")
        }

        // Wir beobachten ob es neue Nachrichten gibt und updaten dann den Adapter
        // Außerdem scrollen wir immer nach unten, so läuft der Chatverlauf mit
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d("Chat", "Nachrichten: $messages")
            messageAdapter.updateMessages(messages)
            scrollToBottom()
        }

        // Beobachtet die LiveData in der die ChatBotantwort gespeichert wird,
        // welche von der Perplexity API geladen kommt
        viewModel.botMessage.observe(viewLifecycleOwner) { botMessage ->
            Log.d("Chat", "Botantwort: $botMessage")
            val currentChannel = viewModel.currentChannel.value
            if (botMessage != null && currentChannel != null) {
                // Die Nachricht vom Bot an den Server senden
                viewModel.sendMessage(botMessage)
                // Livedata zurücksetzen, damit beim nächsten Channeljoin
                // nicht noch mal gesendet wird
                viewModel.resetBotMessage()
            }
        }

        // Button zum senden der Nachricht an den Channel
        binding.buttonSend.setOnClickListener {
            // Eingegebene Nachricht
            val text = binding.editTextMessage.text

            // Wir prüfen ob Text leer ist oder null
            if (!text.isNullOrEmpty()) {
                // Wenn der Nutzer eine Nachricht(alias Command) eingibt, der mit /profil beginnt...
                if (text.startsWith("/")) {
                    viewModel.processCommand(text.toString())
                    // Eingabezeile leeren damit ready für neue Nachricht
                    binding.editTextMessage.text?.clear()
                } else {
                    if (text.toString().lowercase().startsWith("paul")) {
                        val textToBot = text.toString().replaceFirst("(?i)paul".toRegex(), "").trim()
                        Log.d("Chat", "Bot wurde angesprochen. Nachricht: ${text.toString()}")
                        viewModel.sendMessageToBot(textToBot)
                    }
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

    // Wenn das Fragment zerstört wird..
    override fun onDestroyView() {
        super.onDestroyView()
        // .. löschen wir noch die Daten aus der OnlineUsersListe, damit wir nicht
        // mehr als Online angezeigt werden
        viewModel.onChannelLeave()
    }

    // Immer zur neusten Nachricht scrollen
    private fun scrollToBottom() {
        binding.recyclerViewMessages.scrollToPosition(messageAdapter.itemCount - 1)
    }

    // Alle 5 Sekunden den Timestamp in Firestore updaten, so "weiß" der Server
    // wer noch aktiv (online) ist.
    private fun sendOnlineStatus() {
        lifecycleScope.launch {
            while (true) {
                Log.d("Channel", "Userstatus geupdatet")
                viewModel.updateOnlineUserTimestamp()
                delay(5000)
            }
        }
    }

    // Funktion um die Höhe der Statusbar zu erhalten
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    // Funktion um die Höhe der Actionbar zu erhalten
    private fun getActionBarHeight(): Int {
        val styledAttributes = requireActivity().theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = styledAttributes.getDimensionPixelSize(0, 0)
        styledAttributes.recycle()
        return actionBarHeight
    }
}