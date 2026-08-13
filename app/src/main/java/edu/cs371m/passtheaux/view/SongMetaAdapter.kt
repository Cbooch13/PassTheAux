package edu.cs371m.passtheaux.view

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.Utils
import edu.cs371m.passtheaux.databinding.SongRowBinding
import edu.cs371m.passtheaux.model.SongMeta


class SongMetaAdapter(private val viewModel: MainViewModel)
    : ListAdapter<SongMeta, SongMetaAdapter.VH>(Diff()) {
    // This class allows the adapter to compute what has changed
    class Diff : DiffUtil.ItemCallback<SongMeta>() {
        override fun areItemsTheSame(oldItem: SongMeta, newItem: SongMeta): Boolean {
            return oldItem.firestoreID == newItem.firestoreID
        }

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

    inner class VH(private val rowBinding: SongRowBinding) :
        RecyclerView.ViewHolder(rowBinding.root) {


        fun bind(holder: VH, position: Int) {
            val songMeta = getItem(position)
            Utils.loadLocalSongRow(rowBinding, songMeta, count=songMeta.totalListens)
            // Note to future me: It might be fun to display the date

//            //Creates a text editor for the row
//            rowBinding.root.setOnLongClickListener {
//                val edit = EditText(rowBinding.root.context).apply {
//                    setText(songMeta.name)
//                    setSelection(songMeta.name.length)
//                }
//
//                AlertDialog.Builder(rowBinding.root.context)
//                    .setTitle("Edit song name")
//                    .setView(edit)
//                    .setPositiveButton("Save") { dialog, _ ->
//                        val newName = edit.text.toString()
//                        viewModel.updateSongName(songMeta, "name", newName)
//                        rowBinding.rowName.text = newName
//                        dialog.dismiss()
//                    }
//                    .setNegativeButton("Cancel") { dialog, _ ->
//                        dialog.dismiss()
//                    }
//                    .show()
//
//                true
//            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val rowBinding = SongRowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return VH(rowBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(holder, position)
    }
}