package edu.cs371m.passtheaux.view

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import edu.cs371m.passtheaux.MainActivity
import edu.cs371m.passtheaux.MainActivity.Companion
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.R
import edu.cs371m.passtheaux.Utils
import edu.cs371m.passtheaux.api.Song
import edu.cs371m.passtheaux.databinding.ProfileFragmentBinding

class ProfileFragment : Fragment(R.layout.profile_fragment) {
    companion object {
        val TAG: String = "ProfileFragment"
    }
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: ProfileFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ProfileFragmentBinding.bind(view)
        Log.d(TAG, "onViewCreated")
        val mainActivity = (requireActivity() as MainActivity)

        //Handles swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "swipeRefresh")
            binding.swipeRefresh.isRefreshing = false
            handleSpotify()
            handleLocalSongs()
        }
        //Updates user name and page
        viewModel.observeUser().observe(viewLifecycleOwner) {
            binding.name.text = it.name
        }
        viewModel.observeAuthState().observe(viewLifecycleOwner) {
            Log.d(TAG, "AuthState changed")
            handleSpotify()
            handleLocalSongs()
        }

        viewModel.observeProfile().observe(viewLifecycleOwner) {
            Log.d(MainActivity.TAG, "Profile changed")
            binding.bio.text = it.bio
        }


        //Creates a text editor for the bio
        binding.bio.text = viewModel.getBio()
        binding.bio.setOnLongClickListener {
            val edit = EditText(context).apply {
                setText(viewModel.getBio())
                setSelection(viewModel.getBio().length)
            }

            AlertDialog.Builder(context)
                .setTitle("Edit your bio")
                .setView(edit)
                .setPositiveButton("Save") { dialog, _ ->
                    val newBio = edit.text.toString()
                    viewModel.setBio(newBio)
                    binding.bio.text = newBio
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            true
        }
    }

    //Handles top spotify songs
    private fun handleSpotify() {
        if (!viewModel.getAuthState().isAuthorized) {
            Log.d(TAG, "Spotify auth state is unauthorized")
            binding.spotifyAuthButton.setOnClickListener {
                Log.d(TAG, "Spotify button clicked")
                viewModel.startAuthentication()
            }
            Utils.loadSongRow(binding.lastSong, Song(name="Spotify Not Connected"))
            binding.spotifyAuthButton.visibility = View.VISIBLE
            binding.topSpotifySong1.root.visibility = View.INVISIBLE
            binding.topSpotifySong2.root.visibility = View.INVISIBLE
            binding.topSpotifySong3.root.visibility = View.INVISIBLE
        } else {
            binding.spotifyAuthButton.visibility = View.GONE
            binding.topSpotifySong1.root.visibility = View.VISIBLE
            binding.topSpotifySong2.root.visibility = View.VISIBLE
            binding.topSpotifySong3.root.visibility = View.VISIBLE

            //Updates text of the top spotify songs
            viewModel.observeSpotifyTop1().observe(viewLifecycleOwner) {
                Utils.loadSongRow(binding.topSpotifySong1, it)
            }
            viewModel.observeSpotifyTop2().observe(viewLifecycleOwner) {
                Utils.loadSongRow(binding.topSpotifySong2, it)
            }
            viewModel.observeSpotifyTop3().observe(viewLifecycleOwner) {
                Utils.loadSongRow(binding.topSpotifySong3, it)
            }
            viewModel.observeLastPlayedSong().observe(viewLifecycleOwner) {
                Utils.loadSongRow(binding.lastSong, it)
            }
            viewModel.fetchTopSongs()
            viewModel.fetchLastPlayedSong()

        }
    }

    //Handles top local songs
    private fun handleLocalSongs() {
        viewModel.observeLocalTop1().observe(viewLifecycleOwner) {
            Utils.loadLocalSongRow(binding.topLocalSong1, it, count=viewModel.getLocalCount(it))
        }
        viewModel.observeLocalTop2().observe(viewLifecycleOwner) {
            Utils.loadLocalSongRow(binding.topLocalSong2, it, count=viewModel.getLocalCount(it))
        }
        viewModel.observeLocalTop3().observe(viewLifecycleOwner) {
            Utils.loadLocalSongRow(binding.topLocalSong3, it, count=viewModel.getLocalCount(it))
        }
        viewModel.getTopLocalSongs()
    }




}