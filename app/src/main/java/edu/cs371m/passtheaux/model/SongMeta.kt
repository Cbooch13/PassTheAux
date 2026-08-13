package edu.cs371m.passtheaux.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import edu.cs371m.passtheaux.api.Song
import kotlinx.parcelize.Parcelize
import kotlin.uuid.Uuid

data class SongMeta(
    @DocumentId
    var firestoreID: String = "",
    var uuid: String = "",
    var totalListens: Int = 0,
    var ownerID: String = "",
    val coverURL: String = "",
    var name: String = "",
    val artist: List<String> = listOf(),
    val album: String = "",
    val durationMs: Int = 0
)