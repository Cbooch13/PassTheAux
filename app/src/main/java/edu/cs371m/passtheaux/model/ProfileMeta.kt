package edu.cs371m.passtheaux.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

data class ProfileMeta(
    var name: String = "",
    //email or uid will be used as primary key
    var email: String = "",
    @DocumentId
    var uid: String = "",
    var bio: String = "",
    var topSpotifySong1: SongMeta = SongMeta(),
    var topSpotifySong2: SongMeta = SongMeta(),
    var topSpotifySong3: SongMeta = SongMeta(),
    var topLocalSong1: SongMeta = SongMeta(),
    var topLocalSong2: SongMeta = SongMeta(),
    var topLocalSong3: SongMeta = SongMeta(),
    var lastPlayedSong: SongMeta = SongMeta(),
    var localSongCount1 : Int = 0,
    var localSongCount2 : Int = 0,
    var localSongCount3 : Int = 0,
    )
