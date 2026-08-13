package edu.cs371m.passtheaux.api

import com.google.gson.annotations.SerializedName
import kotlin.time.Duration

data class Song (
    @SerializedName("name")
    val name: String = "",
    @SerializedName("artists")
    val artists: List<Artist> = listOf(),
    @SerializedName("album")
    val album: Album = Album(),
    @SerializedName("duration_ms")
    val durationMs: Int = 0,
    val explicit: Boolean = false,
    val href: String = "",
    val id: String = "",
    @SerializedName("preview_url")
    val previewUrl: String? = null,
    val uri: String = "",
    val popularity: Int = -1,
    @SerializedName("disc_number")
    val discNumber: Int = -1,
    @SerializedName("track_number")
    val trackNumber: Int = -1,
    val ownerID: String = "",
    val listens: Int = 0
)