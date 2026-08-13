package edu.cs371m.passtheaux.view

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.R
import edu.cs371m.passtheaux.databinding.LibraryFragmentBinding
import java.util.UUID

class LibraryFragment : Fragment(R.layout.library_fragment) {
    companion object {
        private const val TAG = "LibraryFragment"
    }

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: LibraryFragmentBinding
    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()) { uri ->
        run {
            if (uri != null) {
                Log.d(TAG, "Picked audio: $uri")
                viewModel.uploadAudio(uri, requireContext())
            }
        }
    }

    // https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.ViewHolder#getBindingAdapterPosition()
    // Getting the position of the selected item is unfortunately complicated
    // This always returns a valid index.
    private fun getPos(holder: RecyclerView.ViewHolder) : Int {
        val pos = holder.adapterPosition
        // notifyDataSetChanged was called, so position is not known
        if( pos == RecyclerView.NO_POSITION) {
            return holder.layoutPosition
        }
        return pos
    }

    // Touch helpers provide functionality like detecting swipes or moving
    // entries in a recycler view.  Here we do swipe left to delete
    private fun initTouchHelper(): ItemTouchHelper {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END)
            {
                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                      direction: Int) {
                    val position = getPos(viewHolder)
                    if (direction == ItemTouchHelper.START) {
                        Log.d(TAG, "Swipe delete $position")
                        Snackbar.make(binding.root, "Attempting to delete song", Snackbar.LENGTH_LONG).show()
                        viewModel.removeSongAt(position)
                    } else { //direction == ItemTouchHelper.END
                        Log.d(TAG, "Song saved $position")
                        Snackbar.make(binding.root, "Saving song to library", Snackbar.LENGTH_LONG).show()
                        viewModel.addToLibrary(position)

                    }
                    binding.playerRV.adapter?.notifyItemChanged(position)
                }
            }
        return ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = LibraryFragmentBinding.bind(view)
        Log.d(TAG, "onViewCreated")

        binding.addSongButton.setOnClickListener {
            Log.d(TAG, "Add song button clicked")
            viewModel.saveSongUUID(UUID.randomUUID().toString())
            pickAudioLauncher.launch("audio/*")
        }

        val adapter = SongMetaAdapter(viewModel)
        val rv = binding.playerRV
        val itemDecor = DividerItemDecoration(rv.context, LinearLayoutManager.VERTICAL)
        rv.addItemDecoration(itemDecor)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(rv.context)
        // Swipe left to delete and right to save
        initTouchHelper().attachToRecyclerView(rv)

        viewModel.fetchSongMeta { //Input anything I want to run after this fetch completes
        }
        viewModel.observeSongMeta().observe(viewLifecycleOwner) {
            Log.d(TAG, "observe songMeta")
            adapter.submitList(
                viewModel.getCopyOfFirebaseSongInfo()
            )
        }

    }

}