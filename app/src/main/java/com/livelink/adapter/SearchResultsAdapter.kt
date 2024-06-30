package com.livelink.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.livelink.data.UserData
import com.livelink.databinding.ItemUserSearchResultBinding

class SearchResultsAdapter(
    private val dataset: List<UserData>,
    private val onUserClicked: (UserData) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: ItemUserSearchResultBinding) :
            RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultsAdapter.UserViewHolder {
        val binding = ItemUserSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = dataset[position]
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}
