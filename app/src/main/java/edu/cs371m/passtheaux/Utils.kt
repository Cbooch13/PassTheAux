package edu.cs371m.passtheaux

import android.util.Base64
import android.view.View
import android.widget.ImageButton
import edu.cs371m.passtheaux.api.Song
import edu.cs371m.passtheaux.databinding.ProfileRowBinding
import edu.cs371m.passtheaux.databinding.SongRowBinding
import edu.cs371m.passtheaux.glide.Glide
import edu.cs371m.passtheaux.model.ProfileMeta
import edu.cs371m.passtheaux.model.SongMeta

object Utils {
    fun setBackgroundDrawable(button: ImageButton, resourceId: Int) {
        button.setBackgroundResource(resourceId)
        button.tag = resourceId
    }
    fun setBackgroundColor(view: View, color: Int) {
        view.setBackgroundColor(color)
        view.tag = color
    }

    //Loads the song row with the local songMeta data from firebase
    fun loadLocalSongRow(binding: SongRowBinding, song: SongMeta, showListens: Boolean = true, count : Int = 0, decode: Boolean = true) {
        //Handles song
        binding.rowName.text = song.name

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
        binding.rowArtist.text = artistName

        //Handles album art
        if (song.coverURL.isEmpty()) {
            Glide.clearImage(binding.rowImageView)
            binding.rowImageView.setImageResource(R.drawable.music)
        }
        else {
            if (decode) {
                Glide.loadImage(binding.rowImageView, Base64.decode(song.coverURL, Base64.DEFAULT))
            } else {
                Glide.loadImage(binding.rowImageView, song.coverURL)
            }
        }

        //Handles listens
        if (showListens) {
            binding.rowListensLabel.visibility = View.VISIBLE
            binding.rowListensColon.visibility = View.VISIBLE
            binding.rowListens.visibility = View.VISIBLE
            binding.rowListens.text = count.toString()
        } else {
            //Handles listens
            binding.rowListensLabel.visibility = View.INVISIBLE
            binding.rowListensColon.visibility = View.INVISIBLE
            binding.rowListens.visibility = View.INVISIBLE
        }


        //Handles time
        binding.rowTime.text = convertTime(song.durationMs)
    }

    //Loads the song row with the song data
    fun loadSongRow(binding: SongRowBinding, song: Song) {
        //Handles song name
        binding.rowName.text = song.name

        //Handles artists
        var artistName = ""
        if (song.artists.isNotEmpty()) {
            var i = 0
            for (artist in song.artists) {
                artistName += artist.name
                if (i < song.artists.size - 1) {
                    artistName += ", "
                }
                i+=1
            }
        } else {
            artistName = "No artist"
        }
        binding.rowArtist.text = artistName

        //Handles album art
        if (song.album.images.isEmpty()) {
            Glide.clearImage(binding.rowImageView)
            binding.rowImageView.setImageResource(R.drawable.music)
        } else {
            Glide.loadImage(binding.rowImageView, song.album.images[0].url)
        }

        //Handles listens
        binding.rowListensLabel.visibility = View.INVISIBLE
        binding.rowListensColon.visibility = View.INVISIBLE
        binding.rowListens.visibility = View.INVISIBLE


        //Handles time
        binding.rowTime.text = convertTime(song.durationMs)
    }

    fun loadProfileRow(binding: ProfileRowBinding, profile: ProfileMeta) {
        binding.name.text = profile.name
        binding.email.text = profile.email
        binding.songName.text = profile.lastPlayedSong.name

        //Handles song image
        if (profile.lastPlayedSong.coverURL.isEmpty()) {
            Glide.clearImage(binding.songImage)
            binding.songImage.setImageResource(R.drawable.music)
        } else {
            Glide.loadImage(binding.songImage, profile.lastPlayedSong.coverURL)
        }

    }

    //Converts milliseconds to minutes and seconds
    fun convertTime(milliseconds: Int): String {
        //XXX Write me
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return ("%02d:%02d").format(minutes, seconds)
    }
}