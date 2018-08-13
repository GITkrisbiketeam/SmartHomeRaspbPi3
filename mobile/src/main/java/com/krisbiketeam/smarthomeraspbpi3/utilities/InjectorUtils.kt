package com.krisbiketeam.smarthomeraspbpi3.utilities

import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModelFactory
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomListViewModelFactory
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.data.storage.HomeInformationRepository

/**
 * Static methods used to inject classes needed for various Activities and Fragments.
 */
object InjectorUtils {

    private fun getHomeRepository(): HomeInformationRepository {
        return FirebaseHomeInformationRepository
    }

    /*private fun getGardenPlantingRepository(context: Context): GardenPlantingRepository {
        return GardenPlantingRepository.getInstance(
                AppDatabase.getInstance(context).gardenPlantingDao())
    }*/

    /*fun provideGardenPlantingListViewModelFactory(
        context: Context
    ): GardenPlantingListViewModelFactory {
        val repository = getGardenPlantingRepository(context)
        return GardenPlantingListViewModelFactory(repository)
    }*/

    fun provideRoomListViewModelFactory(): RoomListViewModelFactory {
        return RoomListViewModelFactory(getHomeRepository())
    }

    fun provideRoomDetailViewModelFactory(
            roomName: String
    ): RoomDetailViewModelFactory {
        return RoomDetailViewModelFactory(getHomeRepository(), roomName)
    }
}
