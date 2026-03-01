package com.berat.sakus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import com.berat.sakus.theme.ThemeManager
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.berat.sakus.ui.screens.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.berat.sakus.data.HatBilgisi
import com.berat.sakus.data.NewsItem
import com.berat.sakus.data.Duyuru
import com.berat.sakus.data.DurakBilgisi
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.berat.sakus.ui.screens.SakusHomeScreen
import com.berat.sakus.ui.screens.SplashScreen
import com.berat.sakus.ui.theme.SAKUSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Hook into the native SharedPreferences wrapper mimicking the Flutter ValueNotifier theme
            val themeManager = remember { ThemeManager.getInstance(applicationContext) }
            val isDarkTheme by themeManager.isDarkTheme.collectAsState()
            SAKUSTheme(darkTheme = isDarkTheme) {
                MyApp()
            }
        }
    }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val launchRoute = remember {
        (context as? ComponentActivity)?.intent?.getStringExtra("navigate_to") ?: "home"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController, 
            startDestination = "splash",
        enterTransition = { 
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = { 
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = { 
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = { 
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToHome = {
                    // Bildirimden gelindiyse haberler sayfasına git
                    navController.navigate(launchRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            SakusHomeScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
        composable("transportation") {
            TransportationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateRouteMap = { hat ->
                    val hatJson = Gson().toJson(hat)
                    val encodedJson = URLEncoder.encode(hatJson, "UTF-8")
                    navController.navigate("map/$encodedJson")
                }
            )
        }
        composable("hat_saatleri") {
            TransportationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateRouteMap = { },
                onNavigateToSchedule = { hat ->
                    val hatJson = Gson().toJson(hat)
                    val encodedJson = URLEncoder.encode(hatJson, "UTF-8")
                    navController.navigate("hat_sefer_saatleri/$encodedJson")
                }
            )
        }
        composable(
            route = "hat_sefer_saatleri/{hatJson}",
            arguments = listOf(navArgument("hatJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val hatJsonEncoded = backStackEntry.arguments?.getString("hatJson")
            if (hatJsonEncoded != null) {
                val hatJson = URLDecoder.decode(hatJsonEncoded, "UTF-8")
                val hat = Gson().fromJson(hatJson, HatBilgisi::class.java)
                HatSeferSaatleriScreen(
                    hat = hat,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = "map/{hatJson}",
            arguments = listOf(navArgument("hatJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val hatJsonEncoded = backStackEntry.arguments?.getString("hatJson")
            if (hatJsonEncoded != null) {
                val hatJson = URLDecoder.decode(hatJsonEncoded, "UTF-8")
                val hat = Gson().fromJson(hatJson, HatBilgisi::class.java)
                LineMapScreen(
                    hat = hat,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateDuyuruDetail = { duyuru ->
                        val duyuruJson = Gson().toJson(duyuru)
                        val encodedJson = URLEncoder.encode(duyuruJson, "UTF-8")
                        navController.navigate("duyuru_detay/$encodedJson")
                    },
                    onNavigateSeferSaatleri = { selectedHat ->
                        val hatJsonForSchedule = Gson().toJson(selectedHat)
                        val encodedHatJson = URLEncoder.encode(hatJsonForSchedule, "UTF-8")
                        navController.navigate("hat_sefer_saatleri/$encodedHatJson")
                    }
                )
            }
        }
        composable("haberler") {
            NewsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { haber ->
                    val haberJson = Gson().toJson(haber)
                    val encodedJson = URLEncoder.encode(haberJson, "UTF-8")
                    navController.navigate("haber_detay/$encodedJson")
                }
            )
        }
        composable(
            route = "haber_detay/{haberJson}",
            arguments = listOf(navArgument("haberJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val haberJsonEncoded = backStackEntry.arguments?.getString("haberJson")
            if (haberJsonEncoded != null) {
                val haberJson = URLDecoder.decode(haberJsonEncoded, "UTF-8")
                val haber = Gson().fromJson(haberJson, NewsItem::class.java)
                NewsDetailScreen(
                    haber = haber,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable("duyurular") {
            DuyurularScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { duyuru ->
                    val duyuruJson = Gson().toJson(duyuru)
                    val encodedJson = URLEncoder.encode(duyuruJson, "UTF-8")
                    navController.navigate("duyuru_detay/$encodedJson")
                }
            )
        }
        composable(
            route = "duyuru_detay/{duyuruJson}",
            arguments = listOf(navArgument("duyuruJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val duyuruJsonEncoded = backStackEntry.arguments?.getString("duyuruJson")
            if (duyuruJsonEncoded != null) {
                val duyuruJson = URLDecoder.decode(duyuruJsonEncoded, "UTF-8")
                val duyuru = Gson().fromJson(duyuruJson, Duyuru::class.java)
                DuyuruDetailScreen(
                    duyuru = duyuru,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable("duraklar") {
            DuraklarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { durak ->
                    val durakJson = Gson().toJson(durak)
                    val encodedJson = URLEncoder.encode(durakJson, "UTF-8")
                    navController.navigate("durak_detay/$encodedJson")
                }
            )
        }
        composable(
            route = "durak_detay/{durakJson}",
            arguments = listOf(navArgument("durakJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val durakJsonEncoded = backStackEntry.arguments?.getString("durakJson")
            if (durakJsonEncoded != null) {
                val durakJson = URLDecoder.decode(durakJsonEncoded, "UTF-8")
                val durak = Gson().fromJson(durakJson, DurakBilgisi::class.java)
                DurakDetailScreen(
                    durak = durak,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable("kayip_esya") {
            LostPropertyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("hakkimizda") {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("sss") {
            FaqScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("ayarlar") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // Placeholders for non-converted screens
        composable("iletisim") {
            // Placeholder: Not implemented yet
            SakusHomeScreen(onNavigate = { navController.navigate(it) })
        }
        composable("favoriler") {
            // Placeholder: Not implemented yet
            SakusHomeScreen(onNavigate = { navController.navigate(it) })
        }
        composable("profil") {
            // Placeholder: Not implemented yet
            SakusHomeScreen(onNavigate = { navController.navigate(it) })
        }
    }
    }
}