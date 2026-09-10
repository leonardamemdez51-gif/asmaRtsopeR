package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.components.RamaBottomNavigation
import com.example.ui.components.RamaNavigationRail
import com.example.ui.screens.*

@Composable
fun RamaNavGraph(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    LaunchedEffect(session.isLoggedIn) {
        if (!session.isLoggedIn) {
            navController.navigate(NavRoute.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(session.isLoggedIn, currentRoute) {
        if (!session.isLoggedIn && currentRoute != null && currentRoute != NavRoute.Login.route && currentRoute != NavRoute.ForgotPassword.route) {
            navController.navigate(NavRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val navRoutesList = listOf(
        NavRoute.Dashboard.route,
        NavRoute.SupervisorDashboard.route,
        NavRoute.DailyTasks.route,
        NavRoute.ClientList.route,
        NavRoute.LoanList.route,
        NavRoute.CashRegister.route,
        NavRoute.CollectionVisits.route
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                com.example.core.security.SessionManager.updateLastActive()
            }
        )
    }) {
        val isTablet = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (session.isLoggedIn && isTablet && currentRoute != NavRoute.Login.route) {
                RamaNavigationRail(
                    currentRoute = currentRoute ?: NavRoute.Dashboard.route,
                    userRole = session.user?.role ?: "",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(0) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (session.isLoggedIn && !isTablet && currentRoute in navRoutesList) {
                        RamaBottomNavigation(
                            currentRoute = currentRoute ?: NavRoute.Dashboard.route,
                            userRole = session.user?.role ?: "",
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(0) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                val startDest = if (session.isLoggedIn) {
                    when (session.user?.role) {
                        "ADMINISTRADOR" -> NavRoute.Dashboard.route
                        "SUPERVISOR" -> NavRoute.SupervisorDashboard.route
                        "COBRADOR" -> NavRoute.DailyTasks.route
                        "CONSULTA" -> NavRoute.ClientList.route
                        else -> NavRoute.Login.route
                    }
                } else NavRoute.Login.route

                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(NavRoute.Login.route) {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                val role = viewModel.userSession.value.user?.role
                                val targetRoute = when (role) {
                                    "ADMINISTRADOR" -> NavRoute.Dashboard.route
                                    "SUPERVISOR" -> NavRoute.SupervisorDashboard.route
                                    "COBRADOR" -> NavRoute.DailyTasks.route
                                    "CONSULTA" -> NavRoute.ClientList.route
                                    else -> NavRoute.Dashboard.route
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(NavRoute.Login.route) { inclusive = true }
                                }
                            },
                            onForgotPasswordClick = {
                                navController.navigate(NavRoute.ForgotPassword.route)
                            }
                        )
                    }

                    composable(NavRoute.ForgotPassword.route) {
                        ForgotPasswordScreen(
                            viewModel = viewModel,
                            onBackToLogin = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(NavRoute.ChangePassword.route) {
                        ChangePasswordScreen(
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(NavRoute.UserProfile.route) {
                        UserProfileScreen(
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            },
                            onChangePassword = {
                                navController.navigate(NavRoute.ChangePassword.route)
                            },
                            onLogout = {
                                navController.navigate(NavRoute.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(NavRoute.NotificationCenter.route) {
                        NotificationCenterScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRoute = { route -> navController.navigate(route) }
                        )
                    }

                    composable(NavRoute.CollectorMap.route) {
                        CollectorMapScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onProcessPaymentForClient = { loanId ->
                                navController.navigate(NavRoute.ProcessPayment.createRoute(loanId))
                            }
                        )
                    }

                    composable(NavRoute.RouteManagement.route) {
                        RouteManagementScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NavRoute.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

                    composable(NavRoute.SupervisorDashboard.route) {
                        SupervisorDashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

                    composable(NavRoute.DailyTasks.route) {
                        DailyTasksScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

            composable(NavRoute.ClientList.route) {
                ClientListScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(NavRoute.NewClient.route) {
                NewClientScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoute.EditClient.route,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                NewClientScreen(
                    viewModel = viewModel,
                    editingClientId = clientId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoute.ClientDetail.route,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                ClientDetailScreen(
                    clientId = clientId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(NavRoute.LoanList.route) {
                LoanListScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(
                route = NavRoute.NewLoan.route,
                arguments = listOf(navArgument("clientId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val clientIdArg = backStackEntry.arguments?.getLong("clientId")
                val preselected = if (clientIdArg != null && clientIdArg > 0) clientIdArg else null
                NewLoanScreen(
                    preselectedClientId = preselected,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLoanCreated = { loanId ->
                        navController.popBackStack()
                        navController.navigate(NavRoute.LoanDetail.createRoute(loanId))
                    }
                )
            }

            composable(
                route = NavRoute.RenewalLoan.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                RenewalLoanScreen(
                    loanId = loanId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLoanRenewed = { newLoanId ->
                        navController.popBackStack()
                        navController.navigate(NavRoute.LoanDetail.createRoute(newLoanId))
                    }
                )
            }

            composable(
                route = NavRoute.LoanDetail.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                LoanDetailScreen(
                    loanId = loanId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(
                route = NavRoute.ProcessPayment.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                ProcessPaymentScreen(
                    loanId = loanId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onPaymentCompleted = {
                        navController.popBackStack()
                    }
                )
            }

            composable(NavRoute.CashRegister.route) {
                CashRegisterScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoute.CollectionVisits.route) {
                CollectionVisitsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoute.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoute.AuditLogs.route) {
                AuditLogsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToUserManagement = {
                        navController.navigate(NavRoute.UserManagement.route)
                    }
                )
            }

            composable(NavRoute.UserManagement.route) {
                UserManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoute.DocumentTypeConfig.route) {
                DocumentTypeConfigScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
}
