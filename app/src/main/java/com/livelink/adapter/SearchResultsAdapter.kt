package com.livelink.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.livelink.data.model.UserData
import com.livelink.databinding.ItemUserSearchResultBinding

// Adapter für die Username-Suche, zur Darstellung der Suchergebnisse
class SearchResultsAdapter(
    private val context: Context,
    private val dataset: List<UserData>,
    private val onUserClicked: (UserData) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: ItemUserSearchResultBinding) :
            RecyclerView.ViewHolder(binding.root)


    // Layout anbinden
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultsAdapter.UserViewHolder {
        val binding = ItemUserSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    // Daten anbinden (Username) und das Profil des Users öffnen, wenn man
    // ihn anklickt
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = dataset[position]
        holder.binding.usernameTextView.text = item.username
        holder.itemView.setOnClickListener {
            onUserClicked(item)
        }
    }

    // Größe der Liste
    override fun getItemCount(): Int {
        return dataset.size
    }
}