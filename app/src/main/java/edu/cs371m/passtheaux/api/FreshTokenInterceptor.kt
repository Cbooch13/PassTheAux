package edu.cs371m.passtheaux.api

import android.nfc.Tag
import android.util.Log
import androidx.activity.viewModels
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import edu.cs371m.passtheaux.MainActivity
import edu.cs371m.passtheaux.MainViewModel
import net.openid.appauth.AuthorizationException


//Inspired by prompt from AI, ai showed me how to use an interceptor in a request

class FreshTokenInterceptor(
    private val authService: AuthorizationService,
    private val viewModel: MainViewModel
) : Interceptor {
    companion object {
        val TAG: String = "FreshTokenInterceptor"
    }
    override fun intercept(chain: Interceptor.Chain): Response {
        // Retrieve current AuthState (it should be kept up-to-date in your ViewModel)
        val authState = viewModel.getAuthState()
        val needsRefresh = authState.needsTokenRefresh

        // Run token refresh synchronously using runBlocking
        val freshToken: String = runBlocking {
            suspendCancellableCoroutine { cont ->
                authState.performActionWithFreshTokens(authService) { accessToken, idToken, ex ->
                    if (ex != null) {
                        Log.d(TAG, "Error performing fetch with fresh tokens, exception: $ex")
                        cont.resumeWithException(ex)
                    } else {
                        // If accessToken is null, resume with an empty string
                        viewModel.persistState(authState)
                        Log.d(TAG, "Access Token: $accessToken")
                        cont.resume(accessToken ?: "")
                    }
                }
            }
        }

        // Create new request with fresh access token added as a Bearer token
        val newRequest = chain.request().newBuilder()
            .header("Authorization", "Bearer $freshToken")
            .build()
        return chain.proceed(newRequest)
    }
}
