package edu.cs371m.passtheaux.api

class Repository(private val api: SpotifyApi) {


    suspend fun fetchTopSongs(): List<Song> {
        return api.fetchTopSongs().items
    }

    suspend fun fetchLastPlayedSong() : Song {
        return api.fetchLastPlayedSong().items[0].track
    }




}