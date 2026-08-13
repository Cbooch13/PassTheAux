package edu.cs371m.passtheaux.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.R
import edu.cs371m.passtheaux.Utils
import edu.cs371m.passtheaux.databinding.SongRowBinding
import edu.cs371m.passtheaux.model.SongMeta

// Pass in a function called clickListener that takes a view and a songName
// as parameters.  Call clickListener when the row is clicked.
class RVDiffAdapter(private val viewModel: MainViewModel,
                    private val clickListener: (songIndex : Int)->Unit)
// https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter
// Slick adapter that provides submitList, so you don't worry about how to update
// the list, you just submit a new one when you want to change the list and the
// Diff class computes the smallest set of changes that need to happen.
// NB: Both the old and new lists must both be in memory at the same time.
// So you can copy the old list, change it into a new list, then submit the new list.
    : ListAdapter<SongMeta,
        RVDiffAdapter.ViewHolder>(Diff())
{
    companion object {
        val TAG = "RVDiffAdapter"
    }

    // ViewHolder pattern holds row binding
    inner class ViewHolder(val songRowBinding : SongRowBinding)
        : RecyclerView.ViewHolder(songRowBinding.root) {
        init {
            //XXX Write me.
            songRowBinding.root.setOnClickListener{
                clickListener(this.adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //XXX Write me.
        val rowBinding = SongRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ViewHolder(rowBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //XXX Write me.
        val item = getItem(position)
        val binding = holder.songRowBinding
        Utils.loadLocalSongRow(binding, item, count=viewModel.getLocalCount(item))
        if (viewModel.currentIndex == position) {
            binding.root.setBackgroundResource(R.color.highlight)
        } else {
            binding.root.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    class Diff : DiffUtil.ItemCallback<SongMeta>() {
        // Item identity
        override fun areItemsTheSame(oldItem: SongMeta, newItem: SongMeta): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
        // Item contents are the same, but the object might have changed
        override fun areContentsTheSame(oldItem: SongMeta, newItem: SongMeta): Boolean {
            return oldItem.firestoreID == newItem.firestoreID
                    && oldItem.name == newItem.name
                    && oldItem.ownerID == newItem.ownerID
                    && oldItem.album == newItem.album
                    && oldItem.coverURL == newItem.coverURL
                    && oldItem.durationMs == newItem.durationMs
                    && oldItem.totalListens == newItem.totalListens
        }
    }
}