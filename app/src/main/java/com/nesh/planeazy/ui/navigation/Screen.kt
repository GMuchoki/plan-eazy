package com.nesh.planeazy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Landing : Screen("landing", "Welcome", null)
    object Login : Screen("login", "Login", null)
    object Signup : Screen("signup", "Sign Up", null)
    object ForgotPassword : Screen("forgot_password", "Forgot Password", null)
    
    // Core Destinations (Bottom Nav)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.History)
    object Reports : Screen("reports", "Reports", Icons.Default.PieChart)
    object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    object Budget : Screen("budget", "Budget", Icons.Default.AccountBalanceWallet)
    
    // Secondary Destinations (Drawer Only)
    object Debts : Screen("debts", "Debts & Loans", Icons.Default.Handshake)
    object CategoryManagement : Screen("categories", "Manage Categories", Icons.Default.Category)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

// These appear in the Bottom Navigation Bar
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Reports,
    Screen.Goals,
    Screen.Budget
)

// These appear in the Hamburger Menu (Drawer) list
// We exclude items already in the Bottom Nav or the Drawer Header
val drawerItems = listOf(
    Screen.Debts,
    Screen.CategoryManagement,
    Screen.Settings
)
