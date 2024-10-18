package com.example.emptyviewsapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.emptyviewsapplication.data.entity.SymptomsOfUser

class SymptomsOfUserListAdapter: ListAdapter<SymptomsOfUser, SymptomsOfUserListAdapter.SymptomsOfUserViewHolder>(SymptomsOfUserComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomsOfUserViewHolder {
        return SymptomsOfUserViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: SymptomsOfUserViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.id.toString())
    }

    class SymptomsOfUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val symptomsOfUserItemView: TextView = itemView.findViewById(R.id.textView)

        fun bind(text: String?) {
            symptomsOfUserItemView.text = text
        }

        companion object {
            fun create(parent: ViewGroup): SymptomsOfUserViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)
                return SymptomsOfUserViewHolder(view)
            }
        }
    }

    class SymptomsOfUserComparator : DiffUtil.ItemCallback<SymptomsOfUser>() {
        override fun areItemsTheSame(oldItem: SymptomsOfUser, newItem: SymptomsOfUser): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: SymptomsOfUser, newItem: SymptomsOfUser): Boolean {
            return oldItem.id == newItem.id
        }
    }
}
