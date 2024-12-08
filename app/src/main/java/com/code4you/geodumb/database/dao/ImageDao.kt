package com.code4you.geodumb.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.code4you.geodumb.database.Image

@Dao
interface ImageDao {

    @Insert
    suspend fun insertImage(image: Image)

    @Delete
    suspend fun deleteImage(image: Image)

    @Query("SELECT * FROM images WHERE path = :imagePath LIMIT 1")
    suspend fun getImageByPath(imagePath: String): Image?

    @Query("SELECT * FROM images")
    suspend fun getAllImages(): List<Image>
}
