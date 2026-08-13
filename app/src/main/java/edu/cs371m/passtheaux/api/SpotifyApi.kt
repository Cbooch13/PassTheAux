package edu.cs371m.passtheaux.api


import edu.cs371m.passtheaux.MainViewModel
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header


interface SpotifyApi {


    //Fetches user's top 3 songs from spotify api
    @GET("/v1/me/top/tracks?limit=3")
    suspend fun fetchTopSongs(): SpotifyResponse

    //Fetches user's last played song from spotify api
    @GET("/v1/me/player/recently-played?limit=1")
    suspend fun fetchLastPlayedSong(): RecentlyPlayedResponse

    //Spotify response data classes
    data class SpotifyResponse(val items: List<Song>)
    data class RecentlyPlayedResponse(val items: List<PlayHistory>)
    data class PlayHistory(val track: Song)

    companion object {

        //Constants for Spotify API
        const val REDIRECT_URI = "edu.cs371m.passtheaux://callback"
        const val CLIENT_ID = "6254c5654e754e73a069c674a977be61"
        const val SCOPES = "user-top-read user-read-recently-played"

        const val SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize"
        const val SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token"


        //Base URL for Spotify API
        val apiUrl = HttpUrl.Builder()
            .scheme("https")
            .host("api.spotify.com")
            .build()


        //Public create function that ties together building the base
        //URL and the private create function that initializes Retrofit
        fun create(authService: AuthorizationService,
                   viewModel: MainViewModel): SpotifyApi {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    this.level = HttpLoggingInterceptor.Level.BASIC
                })
                .addInterceptor(FreshTokenInterceptor(authService, viewModel))
                .build()


            return Retrofit.Builder()
                .baseUrl(apiUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SpotifyApi::class.java)
        }

    }
}