package edu.cs371m.passtheaux.view

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.R
import edu.cs371m.passtheaux.Utils
import edu.cs371m.passtheaux.databinding.PlayerFragmentBinding
import edu.cs371m.passtheaux.glide.Glide
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class PlayerFragment : Fragment(R.layout.player_fragment) {
    companion object {
        const val TAG = "PlayerFragment"
    }
    // When this is true, the displayTime coroutine should not modify the seek bar
    val userModifyingSeekBar = AtomicBoolean(false)
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RVDiffAdapter

    private var _binding: PlayerFragmentBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START)
            {
                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                      direction: Int) {
                    val position = getPos(viewHolder)
                    Log.d(TAG, "Swipe delete $position")
                    Snackbar.make(binding.root, "Attempting to delete song", Snackbar.LENGTH_LONG).show()
                    viewModel.removeFromLibrary(position)
                    if (position == viewModel.currentIndex && viewModel.getCopyOfSongInfo().isNotEmpty()) {
                        //Plays next song
                        playNextSong()
                    }
                    binding.playerRV.adapter?.notifyItemChanged(position)
                    adapter.submitList(viewModel.getCopyOfSongInfo())
                }
            }
        return ItemTouchHelper(simpleItemTouchCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initRecyclerViewDividers(rv: RecyclerView) {
        // Let's have dividers between list items
        val dividerItemDecoration = DividerItemDecoration(
            rv.context, LinearLayoutManager.VERTICAL
        )
        rv.addItemDecoration(dividerItemDecoration)
    }

    //Updates current song ui
    private fun updateCurrentSongUI() {
        //XXX Write me. Update the current song
        val song = viewModel.getCurrentSong()
        binding.playerSongName.text = song.name
        //Handles artists
        var artistName = ""
        if (song.artist.isNotEmpty()) {
            var i = 0
            for (artist in song.artist) {
                artistName += artist
                if (i < song.artist.size - 1) {
                    artistName += ", "
                }
                i+=1
            }
        } else {
            artistName = "No artist"
        }
        binding.playerArtistName.text = artistName

        //Handles album art
        if (song.coverURL.isEmpty()) {
            binding.playerAlbumArt.setImageResource(R.drawable.music)
        }
        else {
            Glide.loadImage(binding.playerAlbumArt, Base64.decode(song.coverURL, Base64.DEFAULT))
        }
    }

    // Please put all display updates in this function
    // The exception is that
    //   displayTime updates the seek bar, time passed & time remaining
    private fun updateDisplay() {
        // If settings is active, we are in the background and do
        // not have a binding.  Return early.
        if (_binding == null) {
            return
        }
        //XXX Write me. Make sure all player UI elements are up to date
        // That includes all buttons, textViews, icons & the seek bar
        updateCurrentSongUI()
        binding.playerNextSongText.text = viewModel.getNextSongName()


        if (viewModel.isPlaying) {
            Utils.setBackgroundDrawable(binding.playerPlayPauseButton,
                R.drawable.ic_pause_black_24dp)
        } else {
            Utils.setBackgroundDrawable(binding.playerPlayPauseButton,
                R.drawable.ic_play_arrow_black_24dp)
        }

        if (viewModel.loop) {
            Utils.setBackgroundDrawable(binding.loopIndicator, R.color.highlight)

        } else {
            Utils.setBackgroundColor(binding.loopIndicator, Color.TRANSPARENT)
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Make the RVDiffAdapter and set it up
        //XXX Write me. Setup adapter.
        adapter = RVDiffAdapter(viewModel, { songIndex ->
            val previous = viewModel.currentIndex
            viewModel.currentIndex = songIndex

            adapter.notifyItemChanged(previous)
            adapter.notifyItemChanged(songIndex)

            handlePlayerChange()
            startPlayerIfPlaying()

            updateDisplay()
        })


        binding.playerRV.adapter = adapter
        binding.playerRV.layoutManager = LinearLayoutManager(binding.playerRV.context)
        initRecyclerViewDividers(binding.playerRV)
        adapter.submitList(viewModel.getCopyOfSongInfo())
        // Swipe left to delete
        initTouchHelper().attachToRecyclerView(binding.playerRV)

        //XXX Write me. Write callbacks for buttons
        binding.playerPlayPauseButton.setOnClickListener {
            if (viewModel.isPlaying) {
                viewModel.player.pause()
                viewModel.isPlaying = false
            } else {
                viewModel.isPlaying = true
                startPlayerIfPlaying()
            }
            updateDisplay()
        }
        binding.playerSkipBackButton.setOnClickListener {
            playLastSong()
        }

        binding.playerSkipForwardButton.setOnClickListener {
            playNextSong()
        }

        binding.playerPermuteButton.setOnClickListener {
            adapter.submitList(viewModel.shuffleAndReturnCopyOfSongInfo())
            updateDisplay()
        }

        binding.loopIndicator.setOnClickListener {
            viewModel.loop = !viewModel.loop
            updateDisplay()
        }



        //XXX Write me. binding.playerSeekBar.setOnSeekBarChangeListener
        binding.playerSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                //Do nothing
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                userModifyingSeekBar.set(true)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                userModifyingSeekBar.set(false)
                viewModel.player.seekTo(binding.playerSeekBar.progress)
            }

        })

        //Handles initial player setup
        if (!viewModel.initializePlayer && viewModel.getCopyOfSongInfo().isNotEmpty()) {
            handlePlayerChange()
            viewModel.initializePlayer = true
            Log.d("PlayerFragment", "Player initialized")
        }
        updateDisplay()

        // Don't change this code.  We are launching a coroutine (user-level thread) that will
        // execute concurrently with our code, but will update the displayed time
        val millisec = 100L
        viewLifecycleOwner.lifecycleScope.launch {
            displayTime(millisec)
        }
    }

    // The suspend modifier marks this as a function called from a coroutine
    // Note, this whole function is somewhat reminiscent of the Timer class
    // from Fling and Peck.  We use an independent thread to manage one small
    // piece of our GUI.  This coroutine should not modify any data accessed
    // by the main thread (it can read property values)
    private suspend fun displayTime(misc: Long) {
        // This only runs while the display is active
        while (viewLifecycleOwner.lifecycleScope.coroutineContext.isActive) {
            val currentPosition = viewModel.player.currentPosition
            val maxTime = viewModel.player.duration
            // Update the seek bar (if the user isn't updating it)
            // and update the passed and remaining time
            //XXX Write me

            //Removed from !userModifyingSeekBar.get() code below because if I were to hold the
            //seekbar, let the next song play, and then release the seekbar, the seekbar would
            //jump to the end of the song since it would be using the wrong max.
            //This is because the seekbar would be set to the max
            //of the previous song, so doing this makes the seekbar jump one time when a new song
            //starts playing, which I found to be better than the alternative.
            binding.playerSeekBar.max = maxTime

            if(!userModifyingSeekBar.get()) {
                binding.playerSeekBar.progress = currentPosition
            }

            binding.playerTimePassedText.text = convertTime(currentPosition)
            binding.playerTimeRemainingText.text = convertTime(maxTime - currentPosition)

            // Leave this code as is.  it inserts a delay so that this thread does
            // not consume too much CPU
            delay(misc)
        }
    }

    // This method converts time in milliseconds to minutes-second formatted string
    // with two digit minutes and two digit sections, e.g., 01:30
    private fun convertTime(milliseconds: Int): String {
        //XXX Write me
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return ("%02d:%02d").format(minutes, seconds)
    }

    // XXX Write me, handle player dynamics and currently playing/next song
    private fun handlePlayerChange() {
        //Resets the player and starts the next song
        viewModel.player.reset()
        viewModel.player.release()
        viewModel.currentSongPlayed = false
        viewModel.player = viewModel.createPlayer()
        viewModel.player.setOnCompletionListener {
            if (viewModel.loop) {
                handlePlayerChange()
                startPlayerIfPlaying()
                updateDisplay()
            } else {
                playNextSong()
            }
        }
    }

    //Plays the next song and updates the adapter and display
    private fun playNextSong()  {
        val previous = viewModel.currentIndex
        viewModel.nextSong()

        adapter.notifyItemChanged(previous)
        adapter.notifyItemChanged(viewModel.currentIndex)

        handlePlayerChange()
        startPlayerIfPlaying()
        updateDisplay()
    }

    //Plays the last song and updates the adapter and display
    private fun playLastSong()  {
        val previous = viewModel.currentIndex
        viewModel.prevSong()

        adapter.notifyItemChanged(previous)
        adapter.notifyItemChanged(viewModel.currentIndex)

        handlePlayerChange()
        startPlayerIfPlaying()
        updateDisplay()
    }

    //Starts the player if it is playing, and increments the number of songs played
    private fun startPlayerIfPlaying() {
        if (viewModel.isPlaying) {
            viewModel.player.start()
            if (!viewModel.currentSongPlayed) {
                viewModel.incrementSongPlayed(viewModel.getCurrentSong())
                viewModel.currentSongPlayed = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
