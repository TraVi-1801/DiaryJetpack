package com.example.dialyapp.navigation

import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dialyapp.model.*
import com.example.dialyapp.presentation.components.DisplayAlertDialog
import com.example.dialyapp.presentation.screens.auth.AuthenticationScreen
import com.example.dialyapp.presentation.screens.auth.AuthenticationViewModel
import com.example.dialyapp.presentation.screens.home.HomeScreen
import com.example.dialyapp.presentation.screens.home.HomeViewModel
import com.example.dialyapp.presentation.screens.write.WriteScreen
import com.example.dialyapp.presentation.screens.write.WriteViewModel
import com.example.dialyapp.util.Constants.APP_ID
import com.example.dialyapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupNavGraph(
    startDestination: String, navController: NavHostController, onDataLoaded: () -> Unit
) {
    NavHost(startDestination = startDestination, navController = navController) {
        authenticationRoute(
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
            }, onDataLoaded = onDataLoaded
        )
        homeRoute(navigateToWrite = {
            navController.navigate(Screen.Write.route)
        }, navigateToAuth = {
            navController.popBackStack()
            navController.navigate(Screen.Authentication.route)
        }, onDataLoaded = onDataLoaded, navigateToWriteWithArg = {
            navController.navigate(Screen.Write.passDiaryId(diaryId = it))
        })
        writeRoute(onBackPressed = {
            navController.popBackStack()
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.authenticationRoute(
    navigateToHome: () -> Unit, onDataLoaded: () -> Unit
) {
    composable(route = Screen.Authentication.route) {
        val viewModel: AuthenticationViewModel = viewModel()
        val authenticated by viewModel.authenticated
        val loadingState by viewModel.loadingState
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()

        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }

        AuthenticationScreen(
            authenticated = authenticated,
            loadingState = loadingState,
            oneTapState = oneTapState,
            messageBarState = messageBarState,
            onButtonClicked = {
                oneTapState.open()
                viewModel.setLoading(true)
            },
            onSuccessfulFirebaseSignIn = {
                viewModel.signInWithMongoAtlas(tokenId = it, onSuccess = {
                    messageBarState.addSuccess("Success  Authentication")
                    viewModel.setLoading(false)
                }, onError = { message ->
                    messageBarState.addError(message)
                    viewModel.setLoading(false)
                })
            },
            onFailedFirebaseSignIn = {
                messageBarState.addError(Exception(it))
                viewModel.setLoading(false)
            },
            onDialogDismissed = {
                messageBarState.addError(Exception(it))
                viewModel.setLoading(false)
            },
            navigateToHome = navigateToHome
        )
    }
}


fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToWriteWithArg: (String) -> Unit,
    navigateToAuth: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val diaries by viewModel.diaries
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val context = LocalContext.current
        var signOutDialogOpened by remember {
            mutableStateOf(false)
        }
        var deleteDialogOpened by remember {
            mutableStateOf(false)
        }
        val scope = rememberCoroutineScope()

        LaunchedEffect(key1 = diaries) {
            if (diaries !is RequestState.Loading) {
                onDataLoaded()
            }
        }

        HomeScreen(
            diaries = diaries,
            drawerState = drawerState,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                }
            },
            onSignOutClicked = {
                signOutDialogOpened = true
            },
            navigateToWrite = navigateToWrite,
            navigateToWriteWithArg = navigateToWriteWithArg,
            onDeleteAllClicked = {
                deleteDialogOpened = true
            },
            dateIsSelected = viewModel.dateIsSelected,
            onDateSelected = { viewModel.getDiaries(it) },
            onDateReset = { viewModel.getDiaries() }
        )

        DisplayAlertDialog(title = "Sign Out",
            message = "Are you sure, you want to Sign Out from Google Account?",
            dialogOpened = signOutDialogOpened,
            onDialogClosed = { signOutDialogOpened = false },
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    val user = App.create(APP_ID).currentUser
                    if (user != null) {
                        user.logOut()
                        withContext(Dispatchers.Main) {
                            navigateToAuth()
                        }
                    }
                }
            })

        DisplayAlertDialog(title = "Delete All Diaries",
            message = "Are you sure, you want to permanently delete all your diaries?",
            dialogOpened = deleteDialogOpened,
            onDialogClosed = { deleteDialogOpened = false },
            onYesClicked = {
                viewModel.deleteAllDiaries(
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "All Diaries Deleted.",
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onError = {
                        Toast.makeText(
                            context,
                            if (it.message == "No Internet Connection.")
                                "We need an Internet Connection for this operation."
                            else it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            })
    }
}

@OptIn(ExperimentalPagerApi::class)
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit
) {
    composable(
        route = Screen.Write.route,
        arguments = listOf(navArgument(name = WRITE_SCREEN_ARGUMENT_KEY) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })
    ) {

        val vm: WriteViewModel = hiltViewModel()
        val uiState = vm.uiState
        val context = LocalContext.current
        val pagerState = rememberPagerState()
        val galleryState = vm.galleryState
        val pageNumber by remember {
            derivedStateOf { pagerState.currentPage }
        }


        WriteScreen(uiState = uiState,
            moodName = { Mood.values()[pageNumber].name },
            pagerState = pagerState,
            onTitleChanged = {
                vm.setTitle(it)
            },
            onDescriptionChanged = {
                vm.setDescription(it)
            },
            onDeleteConfirmed = {
                vm.deleteDiary(onSuccess = {
                    Toast.makeText(
                        context, "Deleted", Toast.LENGTH_SHORT
                    ).show()
                    onBackPressed()
                }, onError = {
                    Toast.makeText(
                        context, it, Toast.LENGTH_SHORT
                    ).show()
                })
            },
            onBackPressed = onBackPressed,
            onSaveClick = {
                vm.upsertDiary(diary = it.apply {
                    mood = Mood.values()[pageNumber].name
                }, onSuccess = onBackPressed, onError = { message ->
                    Toast.makeText(
                        context, message, Toast.LENGTH_SHORT
                    ).show()
                })
            },
            onDateTimeUpdated = {
                vm.updateDateTime(it)
            },
            galleryState = galleryState,
            onImageSelect = {
                val type = context.contentResolver.getType(it)?.split("/")?.last() ?: "jpg"
                vm.addImage(
                    image = it,
                    imageType = type
                )
            },
            onImageDeleteClicked = {
                galleryState.removeImage(it)
            }
        )
    }
}