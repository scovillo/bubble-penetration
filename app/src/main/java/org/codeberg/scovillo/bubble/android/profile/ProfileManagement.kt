package org.codeberg.scovillo.bubble.android.profile

import org.codeberg.scovillo.bubble.android.THREAD_POOL
import org.codeberg.scovillo.bubble.android.api.ApiService
import org.codeberg.scovillo.bubble.android.api.UserResource
import org.codeberg.scovillo.bubble.android.api.findHttpStatusException
import org.codeberg.scovillo.bubble.android.persistence.LocalFileStorage
import java.util.concurrent.TimeUnit

/** Owns profile selection, persistence, and legacy-profile migration state. */
class ProfileManagement(private val storage: LocalFileStorage) {

    val profiles: MutableList<UserResource> = storage.readFromFile()

    private var selectedProfile: UserResource? = null
    private val registrationsInProgress = mutableSetOf<String>()

    val hasSelectedProfile: Boolean
        get() = selectedProfile != null

    val selectedUser: UserResource
        get() = checkNotNull(selectedProfile) { "A profile must be selected first." }

    fun restoreSelectedProfile(profile: UserResource) {
        selectedProfile = profile
    }

    fun selectProfile(profile: UserResource) {
        selectedProfile = profile
    }

    fun createOfflineProfile(username: String): UserResource =
        UserResource(username, isOfflineOnly = true).also(::addProfile)

    fun addProfile(profile: UserResource) {
        profiles.add(profile)
        persist()
    }

    fun legacyProfileAwaitingRegistration(isOnlineLeaderboardEnabled: Boolean): UserResource? =
        selectedProfile?.takeIf {
            isOnlineLeaderboardEnabled && it.credential == null && !it.isOfflineOnly
        }

    fun registerSelectedUserIfNeeded(
        isOnlineLeaderboardEnabled: Boolean,
        postToUi: ((() -> Unit) -> Unit),
        onRegistered: () -> Unit,
        onUsernameRejected: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val profile = legacyProfileAwaitingRegistration(isOnlineLeaderboardEnabled) ?: return
        synchronized(registrationsInProgress) {
            if (!registrationsInProgress.add(profile.username)) return
        }

        THREAD_POOL.execute {
            try {
                ApiService.validateUsername(profile.username)[6000, TimeUnit.MILLISECONDS]
                val registeredProfile =
                    ApiService.registerUsername(profile.username)[6000, TimeUnit.MILLISECONDS]
                postToUi {
                    replaceLegacyProfile(profile, registeredProfile)
                    onRegistered()
                }
            } catch (exception: Exception) {
                postToUi {
                    if (exception.findHttpStatusException()?.statusCode == 400) {
                        markProfileOfflineOnly(profile)
                        onUsernameRejected()
                    } else {
                        onFailure(exception)
                    }
                }
            } finally {
                synchronized(registrationsInProgress) {
                    registrationsInProgress.remove(profile.username)
                }
            }
        }
    }

    fun replaceLegacyProfile(previous: UserResource, registered: UserResource) {
        val profileIndex = profiles.indexOfFirst { it === previous }
            .takeIf { it >= 0 }
            ?: profiles.indexOfFirst {
                it.username == previous.username && it.credential == null && !it.isOfflineOnly
            }.takeIf { it >= 0 }
            ?: return

        profiles[profileIndex] = registered
        if (selectedProfile === previous ||
            (selectedProfile?.username == previous.username &&
                selectedProfile?.credential == null &&
                selectedProfile?.isOfflineOnly == false)
        ) {
            selectedProfile = registered
        }
        persist()
    }

    private fun markProfileOfflineOnly(profile: UserResource) {
        replaceLegacyProfile(
            profile,
            UserResource(profile.username, profile.credential, isOfflineOnly = true),
        )
    }

    private fun persist() {
        storage.writeToFile(profiles)
    }
}
