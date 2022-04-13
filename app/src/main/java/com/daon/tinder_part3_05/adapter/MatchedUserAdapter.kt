package com.daon.tinder_part3_05.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daon.tinder_part3_05.R
import com.daon.tinder_part3_05.model.CardItem

class MatchedUserAdapter : ListAdapter<CardItem, MatchedUserAdapter.MatchedUserViewHolder>(diffUtil){

    inner class MatchedUserViewHolder(
        private val view : View
    ) : RecyclerView.ViewHolder(view) {

        fun bind (cardItem: CardItem) {
            view.findViewById<TextView>(R.id.userNameTextView).text = cardItem.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchedUserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MatchedUserViewHolder(inflater.inflate(R.layout.item_matched_user, parent, false))
    }

    override fun onBindViewHolder(holder: MatchedUserViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<CardItem>() {
            override fun areItemsTheSame(oldItem: CardItem, newItem: CardItem) = oldItem.userId == newItem.userId
            override fun areContentsTheSame(oldItem: CardItem, newItem: CardItem) = oldItem == newItem
        }
    }
}