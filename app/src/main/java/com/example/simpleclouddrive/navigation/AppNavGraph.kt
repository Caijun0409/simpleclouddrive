package com.example.simpleclouddrive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.simpleclouddrive.feature.file.FileListScreen
import com.example.simpleclouddrive.feature.home.CloudHomeScreen
import com.example.simpleclouddrive.feature.reader.ReaderScreen
import com.example.simpleclouddrive.feature.share.ShareFileScreen

object AppRoute {
    const val CloudHome = "cloud_home"
    const val FileList = "file_list"
    const val Reader = "reader/{fileId}"
    const val Share = "share/{shareId}"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.CloudHome,
        modifier = modifier
    ) {
        composable(AppRoute.CloudHome) {
            CloudHomeScreen()
        }
        composable(AppRoute.FileList) {
            FileListScreen()
        }
        composable(
            route = AppRoute.Reader,
            arguments = listOf(
                navArgument("fileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ReaderScreen(
                fileId = backStackEntry.arguments?.getString("fileId").orEmpty()
            )
        }
        composable(
            route = AppRoute.Share,
            arguments = listOf(
                navArgument("shareId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ShareFileScreen(
                shareId = backStackEntry.arguments?.getString("shareId").orEmpty()
            )
        }
    }
}
