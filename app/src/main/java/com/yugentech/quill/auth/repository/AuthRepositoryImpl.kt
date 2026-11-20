package com.yugentech.quill.auth.repository

import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.auth.FirebaseUser
import com.yugentech.quill.auth.guest.GuestSession
import com.yugentech.quill.auth.service.AuthService
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.AuthResult
import com.yugentech.quill.domain.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import timber.log.Timber

// Implementation that delegates calls to the underlying AuthService, plus the local guest
class AuthRepositoryImpl(
    private val authService: AuthService,
    private val guestSession: GuestSession
) : AuthRepository {

    init {
        adoptAnonymousGuest()
    }

    /**
     * An anonymous Firebase account (never made by this app, only by older versions) becomes the
     * local guest: its ID is kept as the guest's, so its data stays, and it's signed out of
     * Firebase. Done once, here, as the app starts, before anything reads the auth state, so
     * [isGuest] and [currentUser] are right from the first read and every collector of
     * [authState] sees the same guest without each moving it again.
     */
    private fun adoptAnonymousGuest() {
        val user = authService.currentUser ?: return
        if (!user.isAnonymous) return
        Timber.i("Moving an anonymous Firebase guest to a local guest")
        guestSession.adopt(user.uid)
        authService.signOut()
    }

    // A Firebase account wins; otherwise the local guest, if there is one. No side effects: it
    // runs once for every collector.
    override val authState: Flow<AuthUser?> =
        combine(authService.authStateFlow, guestSession.guestId) { user, guestId ->
            when {
                // Already adopted as the local guest above; seen here only if Firebase reports
                // it once more before the sign-out reaches the listener
                user != null && user.isAnonymous -> AuthUser(uid = user.uid, isGuest = true)

                user != null -> AuthUser(
                    uid = user.uid,
                    isGuest = false,
                    displayName = user.displayName,
                    email = user.email
                )

                guestId != null -> AuthUser(uid = guestId, isGuest = true)
                else -> null
            }
        }

    // The signed-in account's ID, or the local guest's
    override val currentUser: String?
        get() = authService.currentUser?.uid ?: guestSession.guestId.value

    // The reader's display name, for personalizing Aira's responses -- null if not set
    // (e.g. an email/password account that never set one), not logged in, or a guest.
    override val currentUserName: String?
        get() = authService.currentUser?.displayName?.takeIf { it.isNotBlank() }

    // Registers a new user with email and password
    override suspend fun signUp(
        name: String,
        email: String,
        password: String
    ): AuthResult<FirebaseUser> {
        Timber.i("Sign up requested")
        return authService.signUp(name, email, password)
    }

    // Authenticates an existing user
    override suspend fun signIn(email: String, password: String): AuthResult<FirebaseUser> {
        Timber.i("Sign in requested")
        return authService.signIn(email, password)
    }

    // Triggers a password recovery email for the user
    override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
        Timber.i("Password reset requested")
        return authService.sendPasswordResetEmail(email)
    }

    // Logs the current user out of the application
    override fun signOut() {
        Timber.i("Signing out current user")
        authService.signOut()
    }

    // Prepares the intent required to launch the Google Sign-In flow
    override suspend fun getGoogleSignInIntent(webClientId: String): AuthResult<PendingIntent> {
        Timber.d("Requesting Google Sign-In Intent")
        return authService.getGoogleSignInIntent(webClientId)
    }

    // Processes the result returned from the Google Sign-In activity
    override suspend fun handleGoogleSignInResult(data: Intent?): AuthResult<FirebaseUser> {
        Timber.i("Handling Google Sign-In result")
        return authService.handleGoogleSignInResult(data)
    }

    override val isGuest: Boolean
        get() = authService.currentUser == null && guestSession.guestId.value != null

    override fun startGuest() {
        Timber.i("Guest mode started")
        guestSession.start()
    }

    override fun endGuest() {
        Timber.i("Guest mode ended")
        guestSession.end()
    }
}
