package com.krisbiketeam.smarthomeraspbpi3.usecases

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import timber.log.Timber

interface ReloginLastUserWithHomeUseCase {
    operator fun invoke(): Boolean
}

class ReloginLastUserWithHomeUseCaseImpl(
    private val secureStorage: SecureStorage,
    private val homeInformationRepository: FirebaseHomeInformationRepository
) : ReloginLastUserWithHomeUseCase {

    // TODO should reture properl Login/Home state
    override operator fun invoke(): Boolean {
        val currentUser = Firebase.auth.currentUser
        return if (currentUser == null || !secureStorage.isAuthenticated() || currentUser.uid != secureStorage.firebaseCredentials.uid) {
            Timber.d("No credentials defined or changed, starting LoginSettingsFragment")
            //navController.navigate(NavHomeDirections.goToLoginSettingsFragment())
            false
        } else if (secureStorage.homeName.isEmpty()) {
            homeInformationRepository.setUserReference(currentUser.uid)
            Timber.d("No Home Name defined, starting HomeSettingsFragment")
            //navController.navigate(NavHomeDirections.goToHomeSettingsFragment())
            false
        } else {
            homeInformationRepository.setUserReference(currentUser.uid)
            homeInformationRepository.setHomeReference(secureStorage.homeName)
            true
        }
    }
}