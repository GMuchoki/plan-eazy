package com.example.plan_eazy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plan_eazy.ui.navigation.Screen
import com.example.plan_eazy.ui.navigation.bottomNavItems
import com.example.plan_eazy.ui.screens.*
import com.example.plan_eazy.ui.theme.PlaneazyTheme
import com.example.plan_eazy.ui.viewmodel.AuthViewModel
import com.example.plan_eazy.ui.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val isDarkMode by authViewModel.isDarkMode.collectAsState()

            PlaneazyTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val transactionViewModel: TransactionViewModel = viewModel()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val user by authViewModel.user.collectAsState()

                // AUTO-RESTORE LOGIC:
                // When a user logs in (or app starts while logged in), 
                // check if we need to sync data from the cloud.
                LaunchedEffect(user) {
                    if (user != null) {
                        // Automatically pull data from Firebase if the local DB is empty
                        // or just to ensure we have the latest.
                        transactionViewModel.restoreFromCloud()
                    }
                }

                // Decide start destination based on auth state
                val startDestination = if (user == null) Screen.Login.route else Screen.Home.route

                Scaffold(
                    bottomBar = {
                        val isBottomBarVisible = bottomNavItems.any { it.route == currentRoute }
                        if (isBottomBarVisible) {
                            NavigationBar {
                                bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { 
                                            screen.icon?.let { 
                                                Icon(it, contentDescription = screen.title) 
                                            } 
                                        },
                                        label = { Text(screen.title) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(authViewModel, navController)
                        }
                        composable(Screen.Signup.route) {
                            SignupScreen(authViewModel, navController)
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(transactionViewModel, navController)
                        }
                        composable(Screen.Transactions.route) {
                            TransactionsScreen(transactionViewModel)
                        }
                        composable(Screen.Reports.route) {
                            ReportsScreen(transactionViewModel)
                        }
                        composable(Screen.Goals.route) {
                            GoalsScreen(transactionViewModel)
                        }
                        composable(Screen.Budget.route) {
                            BudgetScreen(transactionViewModel)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(transactionViewModel, authViewModel)
                        }
                        composable("add_transaction") {
                            AddTransactionScreen(transactionViewModel, navController)
                        }
                    }
                }
            }
        }
    }
}
