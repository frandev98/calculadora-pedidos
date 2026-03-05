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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataStore = FuxionDataStore(applicationContext)
        FuxionNotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            CalculadoraPedidosTheme {
                val navController = rememberNavController()
                val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = -1L)

                if (anchorDate == -1L) {
                    // Estado de hidratación inicial retenido
                } else {
                    val startDest = if (anchorDate == null) "onboarding" else "year_overview"

                    LaunchedEffect(anchorDate) {
                        if (anchorDate != null) {
                            val status = FuxionCalendarLogic.calculateStatus(Date(anchorDate!!))
                            FuxionNotificationHelper.checkAndNotify(applicationContext, status)
                        }
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

                        // HOMOLOGACIÓN DE NODO 4: Eliminación de parámetros obsoletos y corrección de URL
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
                                dataStore = dataStore, // <-- INYECCIÓN DE INSTANCIA PRE-EXISTENTE
                                onBack = { navController.popBackStack() },
                                onNavigateToOrder = { clientId, targetPoints ->
                                    // ELIMINACIÓN DE BARRERA: Se permite el paso estricto del 0 para comodines
                                    navController.navigate("order_screen/$year/$periodId/$weekId/$clientId/$targetPoints")
                                }
                            )
                        }

                        // HOMOLOGACIÓN DE NODO 5: Extracción de variables 4D completas
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

                        // INYECCIÓN DE NODO FALTANTE: Planificación algorítmica
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

                        // RUTA 7: CONFIGURACIÓN GENERAL
                        composable("settings") {
                            SettingsScreen(
                                dataStore = dataStore,
                                onBack = { navController.popBackStack() },
                                // INYECCIÓN: Ruta directa hacia la Orden Fantasma (Semana 1)
                                onNavigateToAffiliation = { year, startPeriod ->
                                    navController.navigate("order_screen/$year/$startPeriod/1/AFFILIATION_GHOST/40")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}