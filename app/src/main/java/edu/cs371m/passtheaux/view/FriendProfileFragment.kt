package edu.cs371m.passtheaux.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.R
import edu.cs371m.passtheaux.Utils
import edu.cs371m.passtheaux.databinding.ProfileFragmentBinding
import edu.cs371m.passtheaux.model.ProfileMeta
import edu.cs371m.passtheaux.view.ProfileFragment.Companion.TAG

class FriendProfileFragment : Fragment(R.layout.profile_fragment) {
    private val viewModel: MainViewModel by activityViewModels()
    private val args: FriendProfileFragmentArgs by navArgs()
    private lateinit var binding: ProfileFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(javaClass.simpleName, "onViewCreated")
        binding = ProfileFragmentBinding.bind(view)
        Log.d(TAG, "onViewCreated")



        //Handles swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "swipeRefresh")
            viewModel.fetchFriend(args.friend){}
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.observeFriendProfile().observe(viewLifecycleOwner) {
            Log.d(TAG, "observeFriendProfile")
            updateDisplay(it)
        }

        viewModel.fetchFriend(args.friend) {}
    }

    private fun updateDisplay(profile: ProfileMeta) {
        binding.bio.text = profile.bio
        binding.name.text = profile.name
        binding.spotifyAuthButton.visibility = View.GONE
        Utils.loadLocalSongRow(binding.lastSong, profile.lastPlayedSong, showListens = false, decode = false)
        Utils.loadLocalSongRow(binding.topSpotifySong1, profile.topSpotifySong1, showListens = false, decode = false)
        Utils.loadLocalSongRow(binding.topSpotifySong2, profile.topSpotifySong2, showListens = false, decode = false)
        Utils.loadLocalSongRow(binding.topSpotifySong3, profile.topSpotifySong3, showListens = false, decode = false)
        Utils.loadLocalSongRow(binding.topLocalSong1, profile.topLocalSong1, count=profile.localSongCount1)
        Utils.loadLocalSongRow(binding.topLocalSong2, profile.topLocalSong2, count=profile.localSongCount2)
        Utils.loadLocalSongRow(binding.topLocalSong3, profile.topLocalSong3, count=profile.localSongCount3)
    }

}