package com.livelink.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.livelink.data.model.Channel
import com.livelink.databinding.ItemCategoryBinding
import com.livelink.databinding.ItemChannelBinding
import coil.load

// Zum Anzeigen der Kategorien und Channel im Channels Fragment
// Wir übergeben eine Liste aus Strings, welche die Kategorie beinhalten und die Liste der
// Channel
class ChannelsAdapter(
    private val dataset: List<Pair<String, List<Channel>>>,
    private val onChannelItemClick: (String) -> Unit) :
    RecyclerView.Adapter<ChannelsAdapter.CategoryViewHolder>() {

        // ViewHolder für die Kategorien erstellen und das Layout inflaten
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    // Wir setzen die Kategorienamen und erstellen den zweiten Adapter für die Recyclerview in der Kategorie
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val (category, channels) = dataset[position]
        holder.binding.textViewCategory.text = category
        holder.binding.recyclerViewChannels.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = ChannelInnerAdapter(channels)
        }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    // Bindet die item_category.xml, welche die Kategorie-TextView und die RecyclerView für die Channels enthält
    inner class CategoryViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Wir erstellen den Adapter für die Channel
    inner class ChannelInnerAdapter(private val channels: List<Channel>) :
        RecyclerView.Adapter<ChannelInnerAdapter.ChannelViewHolder>() {

            // Inflaten das Layout für die Channel
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
            val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ChannelViewHolder(binding)
        }

        // Wir zeigen das Bild des Channels an (backgroundUrl) sowie den Channelnamen
        override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
            val item = channels[position]
            holder.binding.imageViewChannelImage.load(item.backgroundUrl)
            holder.binding.textViewChannelName.text = item.name

            holder.binding.root.setOnClickListener {
                //Log.d("Channels", "Du hast den Channel ${item.name} ausgewählt.")
                onChannelItemClick(item.name)
            }
        }

        override fun getItemCount(): Int {
            return channels.size
        }

        // Bindet die item_channel.xml für die Channel
        inner class ChannelViewHolder(val binding: ItemChannelBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}