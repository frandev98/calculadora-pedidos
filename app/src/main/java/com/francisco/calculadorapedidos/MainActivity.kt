package com.francisco.calculadorapedidos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect // <--- FALTABA ESTO
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
// --- IMPORTANTE: FALTABA ESTE IMPORT ---
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
                // initial = -1L nos sirve para saber que está cargando
                val anchorDate by dataStore.anchorDateFlow.collectAsState(initial = -1L)

                if (anchorDate == -1L) {
                    // Estado de Carga (Pantalla blanca o Splash mientras lee disco)
                } else {
                    // Si es null, vamos a Onboarding. Si tiene fecha, vamos al Dashboard.
                    val startDest = if (anchorDate == null) "onboarding" else "year_overview"

                    // --- CORRECCIÓN DE RENDIMIENTO ---
                    // Usamos LaunchedEffect para que la notificación no se dispare
                    // cada vez que la pantalla parpadea o se redibuja.
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
                                }
                            )
                        }

                        // RUTA 3: DETALLE DEL PERIODO
                        composable(
                            route = "period_detail/{periodId}",
                            arguments = listOf(navArgument("periodId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val periodId = backStackEntry.arguments?.getInt("periodId") ?: 1
                            PeriodDetailScreen(
                                periodId = periodId,
                                dataStore = dataStore,
                                onBack = { navController.popBackStack() },
                                onWeekClick = { weekId, target ->
                                    // Lógica de Meta: Si el target semanal es alto (180), la meta global es 645.
                                    val goal = if (target >= 180) 645 else 540
                                    navController.navigate("calculator/$goal")
                                }
                            )
                        }

                        // RUTA 4: CALCULADORA (Tu OrderScreen)
                        composable(
                            route = "calculator/{goal}",
                            arguments = listOf(navArgument("goal") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val goal = backStackEntry.arguments?.getInt("goal") ?: 540

                            OrderScreen(
                                targetGoal = goal,
                                onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}