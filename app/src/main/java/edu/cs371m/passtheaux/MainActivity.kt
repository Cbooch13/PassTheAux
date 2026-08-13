package edu.cs371m.passtheaux

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import edu.cs371m.passtheaux.api.SpotifyApi
import edu.cs371m.passtheaux.databinding.ActivityMainBinding
import edu.cs371m.passtheaux.view.ProfileFragment
import edu.cs371m.passtheaux.view.ProfileFragment.Companion
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.browser.BrowserAllowList
import net.openid.appauth.browser.VersionedBrowserMatcher

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG: String = "MainActivity"
    }
    private lateinit var authUser : AuthUser
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authService: AuthorizationService by lazy { initService() }
    private val viewModel: MainViewModel by viewModels()
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
            result ->
        run {
            if (result.resultCode == Activity.RESULT_OK) {
                handleResponse(result.data!!)
            }
        }
    }

    private fun initMenu() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate the menu; this adds items to the action bar if it is present.
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menuLogout -> {
                        authUser.logout()
                        viewModel.setAuthState(AuthState())
                        viewModel.setCurrentAuthUser(invalidUser)
                        true
                    }
                    else -> false
                }
            }
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.appBarMain.toolbar.title = "Pass the Aux"
        setSupportActionBar(binding.appBarMain.toolbar)
        initMenu()

        //Set up nav graph
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView = binding.navView
        navController = findNavController(R.id.mainFrame)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.profileFragment, R.id.playerFragment, R.id.friendsFragment, R.id.libraryFragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.getHeaderView(0).findViewById<TextView>(R.id.appName).text = "Pass the Aux"

        //Initialize the authService
        viewModel.observeAuthorized().observe(this) {
            authenticate()
        }

        //Create authUser
        authUser = AuthUser(activityResultRegistry)
        authUser.observeUser().observe(this) {
            // XXX Write me, user status has changed
            Log.d(TAG, "AuthUser: $it")
            viewModel.setCurrentAuthUser(it)
            navView.getHeaderView(0).findViewById<TextView>(R.id.userEmail).text = it.email
            //Restore spotify auth state from Firestore
            restoreState()
        }
        //authuser needs to observe lifecycle
        lifecycle.addObserver(authUser)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.mainFrame)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //
    //
    //Authenticates the user with spotify
    //
    //

    //Initialize the authentication process with AppAuth
    private fun authenticate() {
        val config = AuthorizationServiceConfiguration(
            Uri.parse(SpotifyApi.SPOTIFY_AUTH_URL),
            Uri.parse(SpotifyApi.SPOTIFY_TOKEN_URL)
        )

        Log.d(TAG, "Redirect URI: ${Uri.parse(SpotifyApi.REDIRECT_URI)}")
        val authRequest = AuthorizationRequest.Builder(
            config,
            SpotifyApi.CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(SpotifyApi.REDIRECT_URI)
        )
            .setScopes(SpotifyApi.SCOPES)
            // Used to force reauthentication whenever access token expires
            // .setAdditionalParameters(mapOf("show_dialog" to "true"))
            .build()

        Log.d(TAG, "Auth URL: ${authRequest.toUri()}")

        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        authLauncher.launch(authIntent)
    }

    //Initialize the authService
    private fun initService(): AuthorizationService {
        val appAuthConfig = AppAuthConfiguration.Builder()
            .setBrowserMatcher(
                BrowserAllowList(
                    VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                    VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
                )
            ).build()
        val service = AuthorizationService(this, appAuthConfig)
        Log.d(TAG, "AuthService: $service")
        viewModel.setAuthService(service)
        return service
    }

    //Handles authentication response from spotify in intent
    private fun handleResponse(data: Intent) {
        val authResponse = AuthorizationResponse.fromIntent(data)
        val authException = AuthorizationException.fromIntent(data)

        Log.d(TAG, "AuthResponse: $authResponse")
        Log.d(TAG, "AuthException: $authException")
        viewModel.setAuthState(AuthState(authResponse, authException))

        //If authResponse is not null, exchange the code for a token
        if (authResponse != null) {
            val tokenExchangeRequest = authResponse.createTokenExchangeRequest()
            authService.performTokenRequest(tokenExchangeRequest) { tokenResponse, tokenException ->
                if (tokenException != null) {
                    Log.d(TAG, "Token exchange failed: ${tokenException.error}")
                    viewModel.setAuthState(AuthState())
                } else {
                    if (tokenResponse != null) {
                        Log.d(TAG, "Token exchange successful")
                        viewModel.getAuthState().update(tokenResponse, null)
                        viewModel.setAuthState(viewModel.getAuthState())
                        viewModel.persistState(viewModel.getAuthState())
                    }
                }
            }
        } else {
            Log.d(TAG, "Authorization failed: ${authException?.error}")
        }
    }

    //Inspired by https://medium.com/androiddevelopers/authenticating-on-android-with-the-appauth-library-7bea226555d5
    //Restore the auth state from firestore
    private fun restoreState() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.let { user ->
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        snapshot.getString("spotifyAuthState")?.let { json ->
                            Log.d(TAG, "Restore Spotify auth state: $json")
                            viewModel.setAuthState(AuthState.jsonDeserialize(json))
                            viewModel.setAuthService(authService)

                        }
                    }
            }
        }
    }


}