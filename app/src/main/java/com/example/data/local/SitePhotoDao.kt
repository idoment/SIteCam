package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.SitePhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface SitePhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: SitePhoto): Long

    @Query("SELECT * FROM site_photos WHERE uid = :uid ORDER BY timestamp DESC")
    fun getPhotosForUser(uid: String): Flow<List<SitePhoto>>

    @Query("SELECT * FROM site_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): SitePhoto?

    @Delete
    suspend fun deletePhoto(photo: SitePhoto)

    @Query("DELETE FROM site_photos WHERE id IN (:ids) AND uid = :uid")
    suspend fun deletePhotosByIds(ids: List<Long>, uid: String)

    @Query("DELETE FROM site_photos WHERE uid = :uid")
    suspend fun deleteAllPhotosForUser(uid: String)

    @Query("SELECT DISTINCT siteLocation FROM site_photos WHERE uid = :uid")
    suspend fun getDistinctSitesForUser(uid: String): List<String>

    @Query("SELECT DISTINCT siteLocation FROM site_photos WHERE siteLocation IS NOT NULL AND siteLocation != '' ORDER BY siteLocation ASC")
    fun getAllDistinctSites(): Flow<List<String>>
}
