package edu.cs371m.passtheaux.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.Utils
import edu.cs371m.passtheaux.databinding.ProfileRowBinding
import edu.cs371m.passtheaux.model.ProfileMeta


class FriendsAdapter(private val onClick: (ProfileMeta) -> Unit)
    : ListAdapter<ProfileMeta, FriendsAdapter.VH>(Diff()) {
    // This class allows the adapter to compute what has changed
    class Diff : DiffUtil.ItemCallback<ProfileMeta>() {
        override fun areItemsTheSame(oldItem: ProfileMeta, newItem: ProfileMeta): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: ProfileMeta, newItem: ProfileMeta): Boolean {

            return oldItem.uid == newItem.uid
                    && oldItem.name == newItem.name
                    && oldItem.email == newItem.email
                    && oldItem.bio == newItem.bio
                    && oldItem.topSpotifySong1 == newItem.topSpotifySong1
                    && oldItem.topSpotifySong2 == newItem.topSpotifySong2
                    && oldItem.topSpotifySong3 == newItem.topSpotifySong3
                    && oldItem.topLocalSong1 == newItem.topLocalSong1
                    && oldItem.topLocalSong2 == newItem.topLocalSong2
                    && oldItem.topLocalSong3 == newItem.topLocalSong3
                    && oldItem.lastPlayedSong == newItem.lastPlayedSong
                    && oldItem.localSongCount1 == newItem.localSongCount1
                    && oldItem.localSongCount2 == newItem.localSongCount2
                    && oldItem.localSongCount3 == newItem.localSongCount3
        }
    }

    inner class VH(private val rowBinding: ProfileRowBinding) :
        RecyclerView.ViewHolder(rowBinding.root) {


        fun bind(holder: VH, position: Int) {
            val profileMeta = getItem(position)
            Utils.loadProfileRow(rowBinding, profileMeta)
            // Note to future me: It might be fun to display the date

            rowBinding.root.setOnClickListener {
                onClick(profileMeta)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val rowBinding = ProfileRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return VH(rowBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(holder, position)
    }
}