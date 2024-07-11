package com.livelink.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.livelink.R
import com.livelink.data.model.OnlineUser
import com.livelink.databinding.ItemOnlineuserBinding

// Zum Anzeigen der Onlineuser innerhalb eines Channels
// Wir übergeben die Onlineuser als Liste vom Typ Onlineuser und eine Funktion falls ein Nutzername
// angeklickt wird
class OnlineUserAdapter(
    private var dataset: List<OnlineUser>,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<OnlineUserAdapter.OnlineUserViewHolder>() {

    // Erstellen den ViewHolder
    inner class OnlineUserViewHolder(val binding: ItemOnlineuserBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Binden das Layout der item_onlineuser.xml
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnlineUserViewHolder {
        val binding = ItemOnlineuserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OnlineUserViewHolder(binding)
    }

    // Hier binden wir die Daten und holen uns das jeweilige OnlineUser Objekt an der
    // [position]
    // Außerdem binden wir die Daten an, wie Username, Alter, usw.
    override fun onBindViewHolder(holder: OnlineUserViewHolder, position: Int) {
        val onlineUser = dataset[position]
        if (onlineUser.profilePic.isNotEmpty()) {
            holder.binding.imageViewProfile.load(onlineUser.profilePic)
        } else {
            holder.binding.imageViewProfile.setImageResource(R.drawable.placeholder_profilepic)
        }
        holder.binding.textViewUsername.text = onlineUser.username
        if (onlineUser.age.isNotEmpty()) {
            holder.binding.textViewAge.text =
                holder.itemView.context.getString(R.string.age_onlineUser, onlineUser.age)
        }
        when (onlineUser.gender.lowercase()) {
            "männlich" -> holder.binding.imageViewGender.setImageResource(R.drawable.baseline_male_24)
            "weiblich" -> holder.binding.imageViewGender.setImageResource(R.drawable.baseline_female_24)
            "divers" -> holder.binding.imageViewGender.setImageResource(R.drawable.baseline_transgender_24)
        }

        // Wir legen die Farbe in der onlineUserListe fest für einen Usernamen
        var colorResId = 0
        when (onlineUser.status) {
            in 0..4 -> colorResId = R.color.white
            in 5..11 -> colorResId = R.color.red
        }
        val color = ContextCompat.getColor(holder.itemView.context, colorResId)
        holder.binding.textViewUsername.setTextColor(color)
        holder.binding.textViewAge.setTextColor(color)

        // Wenn ein Nutzername angeklickt wird..
        holder.binding.root.setOnClickListener {
            // .. öffnen wir das Profil des angeklickten Nutzers
            onUserClick(onlineUser.username)
        }
    }

    // Größe der Liste
    override fun getItemCount(): Int {
        return dataset.size
    }

    // Damit updaten wir die Nachrichten
    // Das verhindert, das immer bei einer neuen Nachricht ALLE Nachrichten neu geladen werden
    // So werden nur die neuen Nachrichten hinzugefügt.
    // Dafür werden die Unterschiede berechnet
    fun updateOnlineUsers(newOnlineUsers: List<OnlineUser>) {
        val diffResult = DiffUtil.calculateDiff(OnlineUserDiffCallback(dataset, newOnlineUsers))
        dataset = newOnlineUsers
        diffResult.dispatchUpdatesTo(this)
    }

    // Die Klasse vergleicht die alte und neue OnlineUserListe, um die Unterschiede zu ermitteln.
    private class OnlineUserDiffCallback(
        private val oldList: List<OnlineUser>,
        private val newList: List<OnlineUser>
    ) : DiffUtil.Callback() {

        // Gibt die größe der alten Liste zurück
        override fun getOldListSize(): Int {
            return oldList.size
        }

        // Gibt die Größe der neuen Liste zurück
        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Vergleicht, ob zwei User dieselben sind
            val oldUser = oldList[oldItemPosition]
            val newUser = newList[newItemPosition]
            return oldUser.username == newUser.username &&
                    oldUser.age == newUser.age &&
                    oldUser.gender == newUser.gender &&
                    oldUser.profilePic == newUser.profilePic &&
                    oldUser.joinTimestamp == newUser.joinTimestamp
        }

        // Prüft ob zwei Objekte gleich sind
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldUser = oldList[oldItemPosition]
            val newUser = newList[newItemPosition]
            return oldUser.username == newUser.username &&
                    oldUser.age == newUser.age &&
                    oldUser.gender == newUser.gender &&
                    oldUser.profilePic == newUser.profilePic &&
                    oldUser.joinTimestamp == newUser.joinTimestamp
        }
    }
}
