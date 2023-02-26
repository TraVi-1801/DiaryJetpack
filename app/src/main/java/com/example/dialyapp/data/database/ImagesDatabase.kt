package com.example.dialyapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.dialyapp.data.database.entity.ImageToDelete
import com.example.dialyapp.data.database.entity.ImageToUpload

@Database(
    entities = [ImageToUpload::class, ImageToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImagesToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}