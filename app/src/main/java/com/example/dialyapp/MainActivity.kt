package com.example.dialyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.dialyapp.data.database.ImageToDeleteDao
import com.example.dialyapp.data.database.ImagesToUploadDao
import com.example.dialyapp.navigation.Screen
import com.example.dialyapp.navigation.SetupNavGraph
import com.example.dialyapp.ui.theme.AppTheme
import com.example.dialyapp.util.Constants.APP_ID
import com.example.dialyapp.util.retryDeletingImageFromFirebase
import com.example.dialyapp.util.retryUploadingImageToFirebase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imagesToUploadDao: ImagesToUploadDao

    @Inject
    lateinit var imagesToDeleteDao: ImageToDeleteDao

    private var keepSplashOpen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition{
            keepSplashOpen
        }
        WindowCompat.setDecorFitsSystemWindows(window,false)
        FirebaseApp.initializeApp(this)
        setContent {
            AppTheme(darkTheme = true, dynamicColor = false) {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController,
                    onDataLoaded = {
                        keepSplashOpen = false
                    }
                )
            }
        }

        cleanupCheck(scope = lifecycleScope, imagesToUploadDao = imagesToUploadDao, imageToDeleteDao = imagesToDeleteDao)
    }
}

private fun cleanupCheck(
    scope: CoroutineScope,
    imagesToUploadDao: ImagesToUploadDao,
    imageToDeleteDao: ImageToDeleteDao
){
    scope.launch(Dispatchers.IO) {
        val result = imagesToUploadDao.getAllImages()
        result.forEach {imageToUpload ->
            retryUploadingImageToFirebase(
                imageToUpload = imageToUpload,
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        imagesToUploadDao.cleanupImage(imageId = imageToUpload.id)
                    }
                }
            )
        }

        val result2 = imageToDeleteDao.getAllImages()
        result2.forEach {imageToDelete ->
            retryDeletingImageFromFirebase(
                imageToDelete = imageToDelete,
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        imageToDeleteDao.cleanupImage(imageId = imageToDelete.id)
                    }
                }
            )
        }
    }
}

private fun getStartDestination(): String {
    val user = App.create(APP_ID).currentUser
    return if(user != null && user.loggedIn) Screen.Home.route
    else Screen.Authentication.route
}