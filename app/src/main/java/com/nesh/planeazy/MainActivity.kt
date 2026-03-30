package com.nesh.planeazy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.nesh.planeazy.ui.navigation.Screen
import com.nesh.planeazy.ui.navigation.bottomNavItems
import com.nesh.planeazy.ui.navigation.drawerItems
import com.nesh.planeazy.ui.screens.*
import com.nesh.planeazy.ui.theme.PlaneazyTheme
import com.nesh.planeazy.ui.viewmodel.AuthViewModel
import com.nesh.planeazy.ui.viewmodel.TransactionViewModel
import com.nesh.planeazy.util.BiometricHelper
import com.nesh.planeazy.util.ReminderWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            } else {
                scheduleDailyReminder(this)
            }
        } else {
            scheduleDailyReminder(this)
        }

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val isDarkMode by authViewModel.isDarkMode.collectAsState()
            val isBiometricAuthenticated by authViewModel.isBiometricAuthenticated.collectAsState()
            val context = LocalContext.current
            val user by authViewModel.user.collectAsState()
            
            PlaneazyTheme(darkTheme = isDarkMode) {
                if (user != null && !isBiometricAuthenticated && BiometricHelper.isBiometricAvailable(context)) {
                    BiometricLockScreen(
                        onAuthenticated = { authViewModel.setBiometricAuthenticated(true) },
                        activity = this@MainActivity
                    )
                } else {
                    MainAppContent(authViewModel, isDarkMode)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleDailyReminder(this)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppContent(authViewModel: AuthViewModel, isDarkMode: Boolean) {
        val navController = rememberNavController()
        val transactionViewModel: TransactionViewModel = viewModel()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val user by authViewModel.user.collectAsState()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        
        // Drawer state scoped to the authenticated content
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        LaunchedEffect(user) {
            if (user != null) {
                transactionViewModel.restoreFromCloud()
            }
        }

        val startDestination = if (user == null) Screen.Login.route else Screen.Home.route
        val isNavVisible = user != null && (bottomNavItems.any { it.route == currentRoute } || drawerItems.any { it.route == currentRoute } || currentRoute?.startsWith("add_transaction") == true)
        val userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"

        // Helper to handle navigation and close drawer
        val onNavigate: (String) -> Unit = { route ->
            scope.launch { drawerState.close() }
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        val content = @Composable {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (isNavVisible) {
                        CenterAlignedTopAppBar(
                            title = { Text(bottomNavItems.find { it.route == currentRoute }?.title ?: drawerItems.find { it.route == currentRoute }?.title ?: "Plan Eazy") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, "Menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = { authViewModel.toggleDarkMode(!isDarkMode) }) {
                                    Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, "Mode")
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (isNavVisible) {
                        NavigationBar {
                            bottomNavItems.forEach { screen ->
                                NavigationBarItem(
                                    icon = { screen.icon?.let { Icon(it, screen.title) } },
                                    label = { Text(screen.title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    selected = currentRoute == screen.route,
                                    onClick = { onNavigate(screen.route) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.padding(padding)) {
                    composable(Screen.Login.route) { LoginScreen(authViewModel, navController) }
                    composable(Screen.Signup.route) { SignupScreen(authViewModel, navController) }
                    composable(Screen.Home.route) { HomeScreen(transactionViewModel, navController, snackbarHostState) }
                    composable(Screen.Transactions.route) { TransactionsScreen(transactionViewModel, snackbarHostState, navController) }
                    composable(Screen.Reports.route) { ReportsScreen(transactionViewModel) }
                    composable(Screen.Goals.route) { GoalsScreen(transactionViewModel) }
                    composable(Screen.Budget.route) { BudgetScreen(transactionViewModel) }
                    composable(Screen.Settings.route) { SettingsScreen(transactionViewModel, authViewModel) }
                    composable(Screen.Profile.route) { ProfileScreen(authViewModel) }
                    composable(
                        route = "add_transaction?transactionId={transactionId}",
                        arguments = listOf(navArgument("transactionId") { 
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
                        val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
                        AddTransactionScreen(transactionViewModel, navController, transactionId, snackbarHostState)
                    }
                }
            }
        }

        if (isNavVisible) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color(0xFF101D3D),
                        drawerContentColor = Color.White,
                        modifier = Modifier.width(280.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(56.dp), tint = Color.White.copy(alpha = 0.9f))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Hi, ${userName.uppercase()} !", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                }
                                IconButton(onClick = { authViewModel.toggleDarkMode(!isDarkMode) }) {
                                    Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, "Mode", tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onNavigate(Screen.Profile.route) }, contentPadding = PaddingValues(0.dp)) {
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Profile", color = Color.White)
                                }
                                VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 12.dp), color = Color.White.copy(alpha = 0.3f) )
                                TextButton(onClick = { 
                                    authViewModel.signOut()
                                    scope.launch { drawerState.close() }
                                }, contentPadding = PaddingValues(0.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Logout", color = Color.White)
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        drawerItems.forEach { item ->
                            NavigationDrawerItem(
                                icon = { item.icon?.let { Icon(it, null) } },
                                label = { Text(item.title, style = MaterialTheme.typography.bodyLarge) },
                                selected = currentRoute == item.route,
                                onClick = { onNavigate(item.route) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unselectedIconColor = Color.White.copy(alpha = 0.8f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.8f),
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            ) {
                content()
            }
        } else {
            content()
        }
    }

    private fun scheduleDailyReminder(context: android.content.Context) {
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(8, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily_reminder", ExistingPeriodicWorkPolicy.KEEP, reminderRequest)
    }
}

@Composable
fun BiometricLockScreen(onAuthenticated: () -> Unit, activity: FragmentActivity) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        BiometricHelper.showBiometricPrompt(activity = activity, onSuccess = onAuthenticated, onError = { errorMessage = it })
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Security, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text("App Locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please authenticate to access your financial data.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = {
                BiometricHelper.showBiometricPrompt(activity = activity, onSuccess = onAuthenticated, onError = { errorMessage = it })
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Fingerprint, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with Biometrics")
            }
        }
    }
}
