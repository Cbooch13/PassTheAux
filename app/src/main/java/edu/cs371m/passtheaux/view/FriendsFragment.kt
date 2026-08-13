package edu.cs371m.passtheaux.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import edu.cs371m.passtheaux.MainViewModel
import edu.cs371m.passtheaux.R
import edu.cs371m.passtheaux.databinding.FriendsFragmentBinding

class FriendsFragment : Fragment(R.layout.friends_fragment) {
    companion object {
        private const val TAG = "FriendsFragment"
    }

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FriendsFragmentBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FriendsFragmentBinding.bind(view)
        Log.d(TAG, "onViewCreated")

        val adapter = FriendsAdapter { friend ->
            val action = FriendsFragmentDirections.actionFriendsFragmentToFriendProfileFragment(friend.uid)
            findNavController().navigate(action)
        }
        val rv = binding.playerRV
        val itemDecor = DividerItemDecoration(rv.context, LinearLayoutManager.VERTICAL)
        rv.addItemDecoration(itemDecor)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(rv.context)

        viewModel.fetchFriends { //Input anything I want to run after this fetch completes
        }

        viewModel.observeFriends().observe(viewLifecycleOwner) {
            Log.d(TAG, "observe friends")
            adapter.submitList(it)
        }


    }
}