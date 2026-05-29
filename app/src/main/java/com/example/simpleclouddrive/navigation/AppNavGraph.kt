package com.example.simpleclouddrive.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.simpleclouddrive.domain.repository.FileRepository
import com.example.simpleclouddrive.feature.file.FileListRoute
import com.example.simpleclouddrive.feature.home.CloudHomeScreen
import com.example.simpleclouddrive.feature.reader.ReaderScreen
import com.example.simpleclouddrive.feature.share.ShareFileScreen

object AppRoute {
    const val CloudHome = "cloud_home"
    const val FileList = "file_list"
    const val Reader = "reader/{fileId}"
    const val Share = "share/{shareId}"

    fun reader(fileId: String): String = "reader/${Uri.encode(fileId)}"

    fun share(shareId: String): String = "share/${Uri.encode(shareId)}"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    fileRepository: FileRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.CloudHome,
        modifier = modifier
    ) {
        composable(AppRoute.CloudHome) {
            CloudHomeScreen(
                fileRepository = fileRepository,
                onOpenReader = { fileId ->
                    navController.navigate(AppRoute.reader(fileId))
                },
                onOpenFilesRoot = {
                    navController.navigate(AppRoute.FileList) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(AppRoute.FileList) {
            FileListRoute(
                fileRepository = fileRepository,
                onOpenReader = { fileId ->
                    navController.navigate(AppRoute.reader(fileId))
                }
            )
        }
        composable(
            route = AppRoute.Reader,
            arguments = listOf(
                navArgument("fileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ReaderScreen(
                fileId = backStackEntry.arguments?.getString("fileId").orEmpty(),
                fileRepository = fileRepository
            )
        }
        composable(
            route = AppRoute.Share,
            arguments = listOf(
                navArgument("shareId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ShareFileScreen(
                shareId = backStackEntry.arguments?.getString("shareId").orEmpty(),
                fileRepository = fileRepository,
                onOpenReader = { fileId ->
                    navController.navigate(AppRoute.reader(fileId))
                }
            )
        }
    }
}
