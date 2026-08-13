package edu.cs371m.passtheaux.api

import com.google.gson.annotations.SerializedName


data class Album (
    val name: String = "",
    val images: List<Image> = listOf(),
    @SerializedName("album_type")
    val albumType: String = "",
    @SerializedName("total_tracks")
    val numTracks: Int = 0,
    val href: String = "",
    val id: String = "",
    @SerializedName("release_date")
    val releaseDate: String = "",
    val uri: String = "",
    val artists: List<Artist> = listOf()
)