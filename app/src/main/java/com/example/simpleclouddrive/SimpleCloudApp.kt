package com.example.simpleclouddrive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.simpleclouddrive.domain.repository.FileRepository
import com.example.simpleclouddrive.navigation.AppNavGraph
import com.example.simpleclouddrive.navigation.BottomNavItem

@Composable
fun SimpleCloudApp(
    fileRepository: FileRepository
) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.CloudDrive,
        BottomNavItem.Files
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val shouldShowBottomBar = bottomNavItems.any { item ->
        currentDestination.isInHierarchy(item.route)
    }

    LaunchedEffect(fileRepository) {
        fileRepository.initializeIfNeeded()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination.isInHierarchy(item.route),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(text = item.label.take(1)) },
                            label = { Text(text = item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

private fun NavDestination?.isInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { destination -> destination.route == route } == true
}
