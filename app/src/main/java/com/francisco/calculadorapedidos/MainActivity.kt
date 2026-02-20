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
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos DataStore y Canal de Notificaciones
        val dataStore = FuxionDataStore(applicationContext)
        FuxionNotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            CalculadoraPedidosTheme {
                val navController = rememberNavController()

                // Leemos la fecha ancla (Inicio de Año)
                val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = -1L)

                if (anchorDate == -1L) {
                    // Cargando... (Pantalla en blanco temporal)
                } else {
                    // Si es null, vamos a Onboarding. Si tiene fecha, vamos al Dashboard.
                    val startDest = if (anchorDate == null) "onboarding" else "year_overview"

                    LaunchedEffect(anchorDate) {
                        if (anchorDate != null) {
                            val status = FuxionCalendarLogic.calculateStatus(Date(anchorDate!!))
                            FuxionNotificationHelper.checkAndNotify(applicationContext, status)
                        }
                    }

                    // --- MAPA DE NAVEGACIÓN ---
                    NavHost(navController = navController, startDestination = startDest) {

                        // RUTA 1: ONBOARDING
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

                        // RUTA 2: DASHBOARD (Resumen del Año)
                        composable("year_overview") {
                            YearOverviewScreen(
                                dataStore = dataStore,
                                onPeriodClick = { periodId ->
                                    navController.navigate("period_detail/$periodId")
                                },
                                // 1. CLIC EN ENGRANAJE -> Va a Configuración (Calendario)
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                // 2. CLIC EN PERSONAS -> Va a Lista de Clientes
                                onClientsClick = {
                                    navController.navigate("client_list")
                                }
                            )
                        }

                        // RUTA 3: DETALLE DEL PERIODO (Semanas)
                        composable(
                            route = "period_detail/{periodId}",
                            arguments = listOf(navArgument("periodId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 1
                            PeriodDetailScreen(
                                periodId = periodId,
                                dataStore = dataStore,
                                onBack = { navController.popBackStack() },
                                // ALERTA: Ahora al tocar una semana vamos a la GESTIÓN, no directo a la orden
                                onWeekClick = { weekId, goal ->
                                    navController.navigate("week_management/$periodId/$weekId/$goal")
                                }
                            )
                        }

                        // RUTA 4: GESTIÓN DE SEMANA (Clientes y Estrategia)
                        composable(
                            route = "week_management/{periodId}/{weekId}/{goal}",
                            arguments = listOf(
                                navArgument("periodId") { type = NavType.IntType },
                                navArgument("weekId") { type = NavType.IntType },
                                navArgument("goal") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 0
                            val weekId = backStackEntry.arguments?.getInt("weekId") ?: 0
                            val goal = backStackEntry.arguments?.getInt("goal") ?: 540

                            WeekManagementScreen(
                                periodId = periodId,
                                weekId = weekId,
                                targetGoal = goal,
                                onBack = { navController.popBackStack() },
                                onNavigateToOrder = { clientId, targetPoints ->
                                    // CORRECCIÓN: Usamos 'targetPoints'.
                                    // Si targetPoints es 0 (Comodín), pasamos un valor referencial (ej. 100) o lo dejamos en 0
                                    // y la UI lo manejará, pero para fijos pasará 120, 180, etc.
                                    val finalTarget = if (targetPoints > 0) targetPoints else 60 // Default para comodines si quieres
                                    navController.navigate("order_screen/$finalTarget/true/$periodId/$weekId/$clientId")
                                },
                                onNavigateToClientList = {
                                    navController.navigate("client_list")
                                }
                            )
                        }

                        // RUTA 5: CALCULADORA DE PEDIDOS (Ahora con clientId)
                        composable(
                            route = "order_screen/{targetGoal}/{isWeeklyMode}/{periodId}/{weekId}/{clientId}",
                            arguments = listOf(
                                navArgument("targetGoal") { type = NavType.IntType },
                                navArgument("isWeeklyMode") { type = NavType.BoolType },
                                navArgument("periodId") { type = NavType.IntType; defaultValue = 0 },
                                navArgument("weekId") { type = NavType.IntType; defaultValue = 0 },
                                navArgument("clientId") { type = NavType.StringType; defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val targetGoal = backStackEntry.arguments?.getInt("targetGoal") ?: 0
                            val isWeeklyMode = backStackEntry.arguments?.getBoolean("isWeeklyMode") ?: false
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 0
                            val weekId = backStackEntry.arguments?.getInt("weekId") ?: 0
                            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                            OrderScreen(
                                targetGoal = targetGoal,
                                isWeeklyMode = isWeeklyMode,
                                periodId = periodId,
                                weekId = weekId,
                                clientId = clientId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // RUTA 6: LISTA DE CLIENTES (Configuración)
                        composable("client_list") {
                            ClientListScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // RUTA 7: CONFIGURACIÓN GENERAL (Si la usas aún)
                        composable("settings") {
                            SettingsScreen(
                                dataStore = dataStore,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}