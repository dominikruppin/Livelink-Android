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

// Zum Anzeigen der Nachrichten innerhalb eines Channels
// Wir übergeben die Nachrichten als Liste vom Typ Message und eine Funktion falls ein Nutzername
// angeklickt wird (Wenn ein Nutzer eine Nachricht sendet, wird sein Nutzername mit dargestellt,
// dieser ist dann anklickbar)
class MessageAdapter(
    private val dataset: List<Message>,
    private val onUserClick: (String) -> Unit) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

        // Erstellen den ViewHolder
    inner class MessageViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Binden das Layout der item_message.xml
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    // Hier binden wir die Daten und holen uns das jeweilige Message Objekt an der [position]
    // Den gespeicherten Timestamp formatieren wir in ein lesbares Uhrzeitformat und weisen es zu
    // Außerdem setzen wir im item den jeweiligen Nutzernamens und seine dazugehörige Nachricht
    // die er gesendet hat.
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = dataset[position]
        val date = Date(item.timestamp)
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(date)
        val username = holder.binding.textViewUsername

        username.text = item.senderId
        holder.binding.textViewMessage.text = item.content
        holder.binding.textViewTimestamp.text = formattedTime

        // Wenn ein Nutzername angeklickt wird..
        holder.binding.textViewUsername.setOnClickListener {
            // .. öffnen wir das Profil des angeklickten Nutzers
            onUserClick(username.text.toString())
            Log.d("Profil", "Du hast das Profil von ${username.text} angeklickt.")
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}