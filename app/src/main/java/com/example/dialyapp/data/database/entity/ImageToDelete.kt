package com.example.dialyapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dialyapp.util.Constants.IMAGES_TO_DELETE_TABLE

@Entity(tableName = IMAGES_TO_DELETE_TABLE)
data class ImageToDelete(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val remoteImagePath: String
)
