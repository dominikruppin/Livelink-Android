package com.livelink.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.livelink.data.model.UserData
import com.livelink.databinding.ItemUserSearchResultBinding

class SearchResultsAdapter(
    private val context: Context,
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
        // Wenn es das erste Element ist..
        /*val cardRadius = context.resources.getDimensionPixelSize(
            when {
                dataset.size == 1 -> R.dimen.card_corner_radius_all
                position == 0 -> R.dimen.card_corner_radius_top
                position == dataset.size - 1 -> R.dimen.card_corner_radius_bottom
                else -> R.dimen.card_corner_radius_none
            }
        )
        holder.binding.cardView.radius = context.resources.getDimension(cardRadius).toFloat()
        */
        holder.binding.usernameTextView.text = item.username

        holder.itemView.setOnClickListener { onUserClicked(item) }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}
