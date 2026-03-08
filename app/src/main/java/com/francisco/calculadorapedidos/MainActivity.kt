package com.francisco.calculadorapedidos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.francisco.calculadorapedidos.data.FuxionDataStore
import com.francisco.calculadorapedidos.logic.FuxionCalendarLogic
import com.francisco.calculadorapedidos.logic.FuxionNotificationHelper
import com.francisco.calculadorapedidos.ui.theme.CalculadoraPedidosTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // INYECCIÓN DE CAMPO HILT: Elimina instanciación manual
    @Inject lateinit var dataStore: FuxionDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FuxionNotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            CalculadoraPedidosTheme {
                val navController = rememberNavController()
                // MUTACIÓN: Observar Nombre
                val userName by dataStore.userNameFlow.collectAsState(initial = "LOADING")

                if (userName == "LOADING") {
                    // Estado de retención
                } else {
                    val startDest = if (userName == null) "onboarding" else "year_overview"

                    LaunchedEffect(Unit) {
                        // Purga del parámetro anchorDate
                        val status = FuxionCalendarLogic.calculateStatus()
                        FuxionNotificationHelper.checkAndNotify(applicationContext, status)
                    }

                    NavHost(navController = navController, startDestination = startDest) {

                        composable("onboarding") {
                            OnboardingScreen(
                                dataStore = dataStore,
                                onFinish = {
                                    navController.navigate("year_overview") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("year_overview") {
                            YearOverviewScreen(
                                dataStore = dataStore,
                                onPeriodClick = { year, periodId ->
                                    navController.navigate("period_detail/$year/$periodId")
                                },
                                onSettingsClick = { navController.navigate("settings") },
                                onClientsClick = { navController.navigate("client_list") }
                            )
                        }

                        composable(
                            route = "period_detail/{year}/{periodId}",
                            arguments = listOf(
                                navArgument("year") { type = NavType.IntType },
                                navArgument("periodId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val year = backStackEntry.arguments?.getInt("year") ?: 2026
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 1

                            PeriodDetailScreen(
                                year = year,
                                periodId = periodId,
                                dataStore = dataStore,
                                onBack = { navController.popBackStack() },
                                onNavigateToWeek = { weekId ->
                                    navController.navigate("week_management/$year/$periodId/$weekId")
                                },
                                onNavigateToFullPlan = {
                                    navController.navigate("full_plan_distribution/$year/$periodId/500")
                                }
                            )
                        }

                        composable(
                            route = "week_management/{year}/{periodId}/{weekId}",
                            arguments = listOf(
                                navArgument("year") { type = NavType.IntType },
                                navArgument("periodId") { type = NavType.IntType },
                                navArgument("weekId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val year = backStackEntry.arguments?.getInt("year") ?: 2026
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 1
                            val weekId = backStackEntry.arguments?.getInt("weekId") ?: 1

                            WeekManagementScreen(
                                year = year,
                                periodId = periodId,
                                weekId = weekId,
                                dataStore = dataStore,
                                onBack = { navController.popBackStack() },
                                onNavigateToOrder = { clientId, targetPoints ->
                                    navController.navigate("order_screen/$year/$periodId/$weekId/$clientId/$targetPoints")
                                }
                            )
                        }

                        composable(
                            route = "order_screen/{year}/{periodId}/{weekId}/{clientId}/{goal}",
                            arguments = listOf(
                                navArgument("year") { type = NavType.IntType },
                                navArgument("periodId") { type = NavType.IntType },
                                navArgument("weekId") { type = NavType.IntType },
                                navArgument("clientId") { type = NavType.StringType },
                                navArgument("goal") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val year = backStackEntry.arguments?.getInt("year") ?: 2026
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 1
                            val weekId = backStackEntry.arguments?.getInt("weekId") ?: 1
                            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                            val goal = backStackEntry.arguments?.getInt("goal") ?: 60

                            OrderScreen(
                                year = year,
                                targetGoal = goal,
                                isWeeklyMode = true,
                                periodId = periodId,
                                weekId = weekId,
                                clientId = clientId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "full_plan_distribution/{year}/{periodId}/{goal}",
                            arguments = listOf(
                                navArgument("year") { type = NavType.IntType },
                                navArgument("periodId") { type = NavType.IntType },
                                navArgument("goal") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val year = backStackEntry.arguments?.getInt("year") ?: 2026
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 1
                            val goal = backStackEntry.arguments?.getInt("goal") ?: 500

                            OrderScreen(
                                year = year,
                                targetGoal = goal,
                                isWeeklyMode = false,
                                periodId = periodId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("client_list") {
                            ClientListScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                // MUTACIÓN: Se recibe también la semana calculada y se inyecta
                                onNavigateToAffiliation = { year, startPeriod, startWeek ->
                                    navController.navigate("order_screen/$year/$startPeriod/$startWeek/AFFILIATION_GHOST/40")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}