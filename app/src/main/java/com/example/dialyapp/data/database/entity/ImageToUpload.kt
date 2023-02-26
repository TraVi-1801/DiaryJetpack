package com.example.dialyapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dialyapp.util.Constants.IMAGES_TO_UPLOAD_TABLE

@Entity(IMAGES_TO_UPLOAD_TABLE)
data class ImageToUpload(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val remoteImagePath: String,
    val imageUri: String,
    val sessionUri: String,
)
