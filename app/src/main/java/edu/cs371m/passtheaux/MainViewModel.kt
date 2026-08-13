package edu.cs371m.passtheaux

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import edu.cs371m.passtheaux.api.Repository
import edu.cs371m.passtheaux.api.SpotifyApi
import edu.cs371m.passtheaux.api.Song
import edu.cs371m.passtheaux.model.ProfileMeta
import edu.cs371m.passtheaux.model.SongMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG: String = "MainViewModel"
        const val LOCAL_FILE_DIRECTORY = "local_songs"
        const val SHARED_PREFERENCES_NAME = "app_prefs"
        const val SONG_PREFERENCES_NAME = "library_songs"
    }
    private var spotifyApi: SpotifyApi? = null
    private var repository: Repository? = null
    private var authService: AuthorizationService? = null
    // Firestore state
    private val storage = Storage()
    // Database access
    private val dbHelp = ViewModelDBHelper()
    // Live data for profiles
    private var profileMetaList = MutableLiveData<List<ProfileMeta>>()
    // Live data for song meta
    private var songMetaList = MutableLiveData<List<SongMeta>>()
    private var librarySongs = mutableListOf<SongMeta>()
    private var currentSongUUID = ""
    // Only call this when we upload a song to firebase
    fun saveSongUUID(uuid: String) {
        currentSongUUID = uuid
    }

    //Live data for top spotify songs
    private val bio = MutableLiveData<String>()
    private val spotifyTop1 = MutableLiveData<Song>()
    private val spotifyTop2 = MutableLiveData<Song>()
    private val spotifyTop3 = MutableLiveData<Song>()
    //Live data for top local songs
    private val localTop1 = MutableLiveData<SongMeta>()
    private val localTop2 = MutableLiveData<SongMeta>()
    private val localTop3 = MutableLiveData<SongMeta>()
    private val listenCount1 = MutableLiveData<Int>()
    private val listenCount2 = MutableLiveData<Int>()
    private val listenCount3 = MutableLiveData<Int>()
    // Live data for last played song
    private val lastPlayedSong = MutableLiveData<Song>()

    //Index of currently playing song
    var currentIndex = 0
    //Proactively create media player
    var player: MediaPlayer = MediaPlayer()
    // Should I loop the current song?
    var loop = false
    // How many songs have played?
    var songsPlayed = 0
    // Is the player playing?
    var isPlaying = false
    var currentSongPlayed = false
    var initializePlayer = false


    // Track current authenticated user
    private var currentAuthUser = MutableLiveData<User>()
    // Auth state for spotify authentication
    private var authState = MutableLiveData<AuthState>()
    private val authorized = MutableLiveData<Unit>()
    //Sends updates to firestore
    private val profile = MediatorLiveData<ProfileMeta>()
    private val friendProfile = MutableLiveData<ProfileMeta>()
    init {
        authState.value = AuthState()
        restoreLibrarySongs()
        initProfile()

    }



    //Observing top songs
    fun observeProfile(): LiveData<ProfileMeta> {
        return profile
    }
    fun observeFriendProfile(): LiveData<ProfileMeta> {
        return friendProfile
    }
    fun observeSpotifyTop1(): LiveData<Song> {
        return spotifyTop1
    }
    fun observeSpotifyTop2(): LiveData<Song> {
        return spotifyTop2
    }
    fun observeSpotifyTop3(): LiveData<Song> {
        return spotifyTop3
    }
    fun observeLocalTop1(): LiveData<SongMeta> {
        return localTop1
    }
    fun observeLocalTop2(): LiveData<SongMeta> {
        return localTop2
    }
    fun observeLocalTop3(): LiveData<SongMeta> {
        return localTop3
    }
    fun observeLastPlayedSong(): LiveData<Song> {
        return lastPlayedSong
    }
    fun getBio(): String {
        return bio.value ?: "Press and hold to edit bio"
    }

    fun setBio(newBio: String) {
        bio.postValue(newBio)
    }

    private fun setupProfile() {
        if (currentAuthUser.value == null || currentAuthUser.value == invalidUser) {
            Log.d(TAG, "No user found")
            return
        }
        dbHelp.fetchProfileMeta(currentAuthUser.value!!) { prof ->
            profile.value = prof
            bio.value = prof.bio
        }
    }

    //Adds sources to profile
    private fun initProfile() {
        Log.d(TAG, "Initializing profile for ${profile.value?.uid}")
        profile.apply {
            addSource(bio) { bio ->
                profile.value?.let { prof ->
                    dbHelp.updateProfileMeta(prof, "bio", bio) {
                        postValue(it)
                    }
                }
            }
            addSource(spotifyTop1) { song ->
                profile.value?.let { prof ->
                    val songMeta: SongMeta = convertToSongMeta(song)
                    dbHelp.updateProfileMeta(prof, "topSpotifySong1", songMeta) {
                        postValue(it)
                    }
                }
            }
            addSource(spotifyTop2) { song ->
                profile.value?.let { prof ->
                    val songMeta: SongMeta = convertToSongMeta(song)
                    dbHelp.updateProfileMeta(prof, "topSpotifySong2", songMeta) {
                        postValue(it)
                    }
                }
            }
            addSource(spotifyTop3) { song ->
                profile.value?.let { prof ->
                    val songMeta: SongMeta = convertToSongMeta(song)
                    dbHelp.updateProfileMeta(prof, "topSpotifySong3", songMeta) {
                        postValue(it)
                    }
                }
            }
            addSource(localTop1) { song ->
                profile.value?.let { prof ->
                    dbHelp.updateProfileMeta(prof, "topLocalSong1", song) {
                        postValue(it)
                    }
                }
            }
            addSource(localTop2) { song ->
                profile.value?.let { prof ->
                    dbHelp.updateProfileMeta(prof, "topLocalSong2", song) {
                        postValue(it)
                    }
                }
            }
            addSource(localTop3) {song ->
                profile.value?.let { prof ->
                    dbHelp.updateProfileMeta(prof, "topLocalSong3", song) {
                        postValue(it)
                    }
                }
            }
            addSource(listenCount1) { count ->
                profile.value?.let { prof ->
                    dbHelp.updateProfileMeta(prof, "localSongCount1", count) {
                        postValue(it)
                    }
                }
            }
            addSource(listenCount2) { count ->
                profile.value?.let { prof ->
                    dbHelp.updateProfileMeta(prof, "localSongCount2", count) {
                        postValue(it)
                    }
                }
            }
            addSource(listenCount3) { count ->
                profile.value?.let { prof ->
                    dbHelp.updateProfileMeta(prof, "localSongCount3", count) {
                        postValue(it)
                    }
                }
            }
            addSource(lastPlayedSong) { song ->
                profile.value?.let { prof ->
                    val songMeta: SongMeta = convertToSongMeta(song)
                    dbHelp.updateProfileMeta(prof, "lastPlayedSong", songMeta) {
                        postValue(it)
                    }
                }
            }
        }
    }

    //Observe this to know when to start authentication process
    fun observeAuthorized(): LiveData<Unit> {
        return authorized
    }

    //Used to start authentication process
    fun startAuthentication() {
        authorized.postValue(Unit)
    }

    // Auth state for spotify authentication
    fun observeAuthState(): LiveData<AuthState> {
        return authState
    }
    fun getAuthState(): AuthState {
        return authState.value!!
    }
    fun setAuthState(state: AuthState) {
        authState.postValue(state)
    }

    //Save the auth state to firestore, inspired by
    // https://medium.com/androiddevelopers/authenticating-on-android-with-the-appauth-library-7bea226555d5
    fun persistState(authState: AuthState) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val spotifyJson = authState.jsonSerializeString()
        Log.d(MainActivity.TAG, "Persist Spotify auth state: $spotifyJson")
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(mapOf("spotifyAuthState" to spotifyJson), SetOptions.merge())
    }

    //Initialize the authService
    fun setAuthService(service: AuthorizationService) {
        authService = service
        spotifyApi = SpotifyApi.create(authService!!, this)
        repository = Repository(spotifyApi!!)

    }

    //Gets top songs from library
    fun getTopLocalSongs() {
        val sorted = librarySongs.sortedByDescending { getLocalCount(it) }
        if (sorted.isNotEmpty()) {
            localTop1.postValue(sorted[0])
            listenCount1.postValue(getLocalCount(sorted[0]))
        } else {
            localTop1.postValue(SongMeta(name = "No song found", artist = listOf("No artist")))
        }
        if (sorted.size > 1) {
            localTop2.postValue(sorted[1])
            listenCount2.postValue(getLocalCount(sorted[1]))
        } else {
            localTop2.postValue(SongMeta(name = "No song found", artist = listOf("No artist")))
        }
        if (sorted.size > 2) {
            localTop3.postValue(sorted[2])
            listenCount3.postValue(getLocalCount(sorted[2]))
        } else {
            localTop3.postValue(SongMeta(name = "No song found", artist = listOf("No artist")))
        }
    }

    //Fetches top songs from spotify
    fun fetchTopSongs() {
        viewModelScope.launch(context = Dispatchers.IO) {
            if (repository == null) {
                Log.e(TAG, "Repository is not initialized. Have you set authService?")
                return@launch
            }
            try {
                val songs = repository!!.fetchTopSongs()
                Log.d(TAG, "Top Songs: $songs")
                if (songs.isNotEmpty()) {
                    spotifyTop1.postValue(songs[0])
                } else {
                    spotifyTop1.postValue(Song(name = "No song found"))
                }
                if (songs.size > 1) {
                    spotifyTop2.postValue(songs[1])
                } else {
                    spotifyTop2.postValue(Song(name = "No song found"))
                }
                if (songs.size > 2) {
                    spotifyTop3.postValue(songs[2])
                } else {
                    spotifyTop3.postValue(Song(name = "No song found"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching top songs", e)
            }
        }
    }

    //Fetches last played song from spotify
    fun fetchLastPlayedSong() {
        viewModelScope.launch(context = Dispatchers.IO) {
            if (repository == null) {
                Log.e(TAG, "Repository is not initialized. Have you set authService?")
                return@launch
            }
            try {
                val song = repository!!.fetchLastPlayedSong()
                Log.d(TAG, "Last played: $song")
                lastPlayedSong.postValue(song)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching top songs", e)
            }
        }
    }


    // MainActivity gets updates on this via live data and informs view model
    fun setCurrentAuthUser(user: User) {
        currentAuthUser.value = user
        setupProfile()
    }
    fun observeUser(): LiveData<User> {
        return currentAuthUser
    }

    fun observeFriends(): LiveData<List<ProfileMeta>> {
        return profileMetaList
    }

    fun fetchFriends(resultListener: () -> Unit) {
        dbHelp.fetchFriends {
            profileMetaList.postValue(it)
            resultListener.invoke()
        }
    }

    fun fetchFriend(uuid: String, resultListener: () -> Unit) {
        dbHelp.fetchProfileMeta(User(
            uid = uuid,
            nullableName = null,
            nullableEmail = null
        ), createUser = false) {
            friendProfile.postValue(it)
            resultListener.invoke()

        }
    }

    fun fetchSongMeta(resultListener: () -> Unit) {
        dbHelp.fetchSongMeta {
            songMetaList.postValue(it)
            resultListener.invoke()
        }
    }

    //Creates song meta from file and uploads it to firebase storage
    fun createSongMeta(uri: Uri, uuid: String, context: Context) {
        //Get duration (found from https://stackoverflow.com/questions/15394640/get-duration-of-audio-file)
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toIntOrNull() ?: 0

        val image = mmr.embeddedPicture
        val coverUrl = if (image == null) {
                Log.d(TAG, "No embedded picture found")
                ""
            } else {
                Base64.encodeToString(image, Base64.DEFAULT)
        }
        val name = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: uri.lastPathSegment ?: "Unknown"
        val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown"
        val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown"

        val ownerUid = currentAuthUser.value?.uid ?: "-1"

        val songMeta = SongMeta(
            name = name,
            artist = listOf(artist),
            uuid = uuid,
            album = album,
            ownerID = ownerUid,
            durationMs = durationMs,
            coverURL = coverUrl
        )

        dbHelp.uploadSongMeta(songMeta) {
            songMetaList.postValue(it)

        }

    }

    fun convertToSongMeta(song: Song) : SongMeta {
        return SongMeta(
            name = song.name,
            artist = song.artists.map { it.name },
            uuid = song.id,
            album = song.album.name,
            ownerID = currentAuthUser.value?.uid ?: "-1",
            durationMs = song.durationMs,
            coverURL = song.album.images[0].url
        )
    }

    fun removeSongAt(position: Int) {
        // XXX Deletion requires two different operations.  What are they?
        val song = getSongMeta(position)
        if (song.ownerID == currentAuthUser.value?.uid) {
            Log.d(TAG, "Removing song: ${song.uuid}")
            storage.deleteSong(song.uuid)
            dbHelp.removeSongMeta(song) {
                songMetaList.postValue(it)
            }
        } else {
            Log.d(TAG, "ERROR DELETING SONG, NOT OWNER")
        }

    }
    //Add song to library and download it to local storage
    fun addToLibrary(position: Int) {
        val song = getSongMeta(position)
        val songFile = storage.uuid2StorageReference(song.uuid)
        val songsDir = File(getApplication<Application>().filesDir, LOCAL_FILE_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val localFile = File(songsDir, "${song.uuid}.mp3")
        if (localFile.exists() || librarySongs.contains(song)) {
            Log.d(TAG, "Song already exists in local storage: $localFile")
            return
        }
        songFile.getFile(localFile)
            .addOnSuccessListener {
                Log.d(TAG, "Song downloaded successfully: $localFile")
                librarySongs.add(song)
                persistLibrarySongs()
            }
            .addOnFailureListener { ex ->
                Log.e(TAG, "Error downloading song", ex)
            }
        Log.d(TAG, "Added song to library: ${song.uuid}")
    }

    fun removeFromLibrary(position: Int) {
        val song = librarySongs[position]
        val songsDir = File(getApplication<Application>().filesDir, LOCAL_FILE_DIRECTORY)
        val localFile = File(songsDir, "${song.uuid}.mp3")
        if (localFile.exists()) {
            Log.d(TAG, "Removing song from local storage: $localFile")
            librarySongs.remove(song)
            localFile.delete()
            persistLibrarySongs()
        } else {
            Log.d(TAG, "ERROR DELETING SONG, NOT FOUND")
        }

    }

    fun uploadAudio(uri: Uri, context: Context) {
        storage.uploadSong(uri, currentSongUUID) {
            createSongMeta(uri, currentSongUUID, context)
        }
    }

    fun observeSongMeta(): LiveData<List<SongMeta>> {
        return songMetaList
    }

    // Get a song from the memory cache
    fun getSongMeta(position: Int) : SongMeta {
        val note = songMetaList.value?.get(position)
        return note!!
    }

    fun getCopyOfFirebaseSongInfo(): MutableList<SongMeta> {
        return songMetaList.value!!.toMutableList()
    }

    //Media Player functions

    fun createPlayer(): MediaPlayer {
        if (librarySongs.isEmpty()) {
            Log.d(TAG, "No songs in library")
            return MediaPlayer()
        }
        return MediaPlayer.create(getApplication<Application>(), getCurrentSongURI())
    }

    fun getCopyOfSongInfo(): MutableList<SongMeta> {
        return librarySongs.toMutableList()
    }

    fun shuffleAndReturnCopyOfSongInfo(): MutableList<SongMeta> {
        // XXX Write me
        val currID = librarySongs[currentIndex].uuid
        val shuffled = this.getCopyOfSongInfo()
        shuffled.shuffle()
        for (song in shuffled) {
            if (song.uuid == currID) {
                currentIndex = shuffled.indexOf(song)
                break
            }
        }
        librarySongs = shuffled
        return shuffled
    }

    fun getCurrentSong() : SongMeta {
        // XXX Write me
        if (librarySongs.isEmpty()) {
            return SongMeta(name = "No song found", artist = listOf("No artist"))
        }

        return librarySongs[currentIndex]
    }


    // Private function
    private fun nextIndex() : Int {
        // XXX Write me
        return (currentIndex + 1) % librarySongs.size
    }
    fun nextSong() {
        // XXX Write me
        currentIndex = nextIndex()

    }
    fun getNextSongName() : String {
        // XXX Write me
        if (librarySongs.isEmpty()) {
            return "No song found"
        }

        return librarySongs[nextIndex()].name
    }

    fun prevSong() {
        // XXX Write me
        currentIndex = (currentIndex - 1 + librarySongs.size) % librarySongs.size
    }

    fun getCurrentSongURI(): Uri {
        // XXX Write me
        val song = librarySongs[currentIndex]
        val songsDir = File(getApplication<Application>().filesDir, LOCAL_FILE_DIRECTORY)
        val localFile = File(songsDir, "${song.uuid}.mp3")
        return Uri.fromFile(localFile)
    }

    //Gets local count from shared preferences
    fun getLocalCount(song: SongMeta): Int {
        val prefs = getApplication<Application>().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val key = "localListens_${song.uuid}"
        return prefs.getInt(key, 0)
    }

    //Increments number of listens locally and in total
    fun incrementSongPlayed(song: SongMeta) {
        //Update local library
        val prefs = getApplication<Application>().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val key = "localListens_${song.uuid}"
        val current = prefs.getInt(key, 0)
        val updated = current + 1
        prefs.edit().putInt(key, updated).apply()

        //Update database entry
        dbHelp.updateSongMeta(song, "totalListens", FieldValue.increment(1)) {
            songMetaList.postValue(it)
        }
        Log.d(TAG, "Incremented song count")
    }

    fun updateSongName(song: SongMeta, field: String, newName: String) {
        for (lSong in librarySongs) {
            if (lSong.uuid == song.uuid) {
                lSong.name = newName
                persistLibrarySongs()
            }
        }
        dbHelp.updateSongMeta(song, field, newName) {
            songMetaList.postValue(it)
        }
    }

    //Persist library songs into shared preferences
    private fun persistLibrarySongs() {
        val prefs = getApplication<Application>().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(librarySongs)
        prefs.edit().putString(SONG_PREFERENCES_NAME, json).apply()
    }

    //Restore library songs from shared preferences
    private fun restoreLibrarySongs() {
        val prefs = getApplication<Application>().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(SONG_PREFERENCES_NAME, null)
        if (json != null) {
            //Got this line from copilot
            val restoredSongs: List<SongMeta> = Gson().fromJson(json, Array<SongMeta>::class.java).toList()
            librarySongs.addAll(restoredSongs)
        }
    }
}