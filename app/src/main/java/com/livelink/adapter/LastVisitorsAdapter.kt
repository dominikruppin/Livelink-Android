package com.livelink.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.livelink.data.model.Channel
import com.livelink.data.model.ProfileVisitor
import com.livelink.databinding.ItemRecentVisitorBinding

// Zum Anzeigen der Kategorien und Channel im Channels Fragment
// Wir übergeben eine Liste aus Channels und eine Funktion die aufgerufen wird, wenn man einen Channel anklickt
class LastVisitorsAdapter(
    private val dataset: List<ProfileVisitor>,
    private val onUserItemClick: (ProfileVisitor) -> Unit
) : RecyclerView.Adapter<LastVisitorsAdapter.VisitorViewHolder>() {

    // Erstellen des ViewHolder
    inner class VisitorViewHolder(val binding: ItemRecentVisitorBinding) : // BINDING ANPASSEN
        RecyclerView.ViewHolder(binding.root)

    // Binden des Layouts der item_recent_visitor.xml
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitorViewHolder {
        val binding = ItemRecentVisitorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisitorViewHolder(binding)
    }

    // Daten anbinden
    override fun onBindViewHolder(holder: VisitorViewHolder, position: Int) {
        val item = dataset[position]
        // Anbinden der Daten
        holder.binding.profileImageView.load(item.profilePicURL)
        holder.binding.userNameTextView.text = item.username

        // Wenn ein Channel angeklickt wird..
        holder.binding.root.setOnClickListener {
            Log.d("Channels", "Du hast den Channel ${item.username} ausgewählt.")
            // ..rufen wir die onChannelItemClick Funktion auf
            onUserItemClick(item)
        }
    }

    // Gibt die Größe des Datasets zurück
    override fun getItemCount(): Int {
        return dataset.size
    }
}
