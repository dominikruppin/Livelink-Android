package com.livelink.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.livelink.data.model.Channel
import com.livelink.databinding.ItemChannelBinding
import coil.load
import com.livelink.databinding.ItemRecentChannelBinding

// Zum Anzeigen der Kategorien und Channel im Channels Fragment
// Wir übergeben eine Liste aus Channels und eine Funktion die aufgerufen wird, wenn man einen Channel anklickt
class LastChannelsAdapter(
    private val dataset: List<Channel>,
    private val onChannelItemClick: (Channel) -> Unit
) : RecyclerView.Adapter<LastChannelsAdapter.ChannelViewHolder>() {

    // Erstellen des ViewHolder
    inner class ChannelViewHolder(val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Binden des Layouts der item_channel.xml
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    // Daten anbinden
    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val item = dataset[position]

        holder.binding.textViewChannelName.text = item.name
        holder.binding.imageViewChannelImage.load(item.backgroundUrl)

        // Wenn ein Channel angeklickt wird..
        holder.binding.root.setOnClickListener {
            Log.d("Channels", "Du hast den Channel ${item.name} ausgewählt.")
            // ..rufen wir die onChannelItemClick Funktion auf
            onChannelItemClick(item)
        }
    }

    // Gibt die Größe des Datasets zurück
    override fun getItemCount(): Int {
        return dataset.size
    }
}
