package com.example.simpleclouddrive.navigation

sealed class BottomNavItem(
    val route: String,
    val label: String
) {
    data object CloudDrive : BottomNavItem(
        route = AppRoute.CloudHome,
        label = "网盘"
    )

    data object Files : BottomNavItem(
        route = AppRoute.FileList,
        label = "文件"
    )
}
