package gf.gab.upm.spv

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ota.OtaInfo
import gf.gab.upm.spv.ota.OtaVersion
import gf.gab.upm.spv.screens.DashboardRoute
import gf.gab.upm.spv.screens.DeviceDetailsRoute
import gf.gab.upm.spv.screens.OtaRoute
import gf.gab.upm.spv.screens.PairingRoute
import gf.gab.upm.spv.screens.PermissionRoute
import gf.gab.upm.spv.screens.historical.HistoricalDataScreen
import gf.gab.upm.spv.screens.main.DashboardScreen
import gf.gab.upm.spv.screens.ota.OtaScreen
import gf.gab.upm.spv.screens.setup.PairingScreen
import gf.gab.upm.spv.screens.setup.PermissionScreen
import gf.gab.upm.spv.ui.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationHost()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavigationHost() {
    val navController = rememberNavController()

    val permissions = rememberMultiplePermissionsState(listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    ))

    NavHost(
        navController = navController,
        startDestination = if(permissions.allPermissionsGranted) DashboardRoute else PermissionRoute
    ) {
        composable<PermissionRoute> {
            LaunchedEffect(permissions.allPermissionsGranted) {
                if (permissions.allPermissionsGranted) {
                    navController.navigate(DashboardRoute) {
                        popUpTo<PermissionRoute> {
                            inclusive = true
                        }
                    }
                }
            }
            PermissionScreen {
                permissions.launchMultiplePermissionRequest()
            }
        }

        composable<DashboardRoute> {
            DashboardScreen(
                onPairClicked =  {
                    navController.navigate(PairingRoute)
                },
                onDeviceDetailsClicked = { address ->
                    navController.navigate(DeviceDetailsRoute(address.toString()))
                }
            )
        }

        composable<DeviceDetailsRoute> { backStackEntry ->
            val data: DeviceDetailsRoute = backStackEntry.toRoute()
            HistoricalDataScreen(
                device = MacAddress.fromString(data.macAddress),
                onUpdateClicked = { device, otaInfo ->
                    navController.popBackStack(
                        route = DashboardRoute,
                        inclusive = false
                    )
                    navController.navigate(OtaRoute(
                        macAddress = device.toString(),
                        otaSize = otaInfo.sizeBytes,
                        otaUrl = otaInfo.url,
                        otaVersion = otaInfo.version.toString(),
                    ))
                }
            )
        }

        composable<OtaRoute> { backstackEntry ->
            val data: OtaRoute = backstackEntry.toRoute()
            OtaScreen(
                device = MacAddress.fromString(data.macAddress),
                otaInfo = OtaInfo(
                    version = OtaVersion.tryParseSemver(data.otaVersion)!!,
                    url = data.otaUrl,
                    sizeBytes = data.otaSize
                ),
                onDismiss = {
                    navController.popBackStack(
                        route = DashboardRoute,
                        inclusive = false
                    )
                }
            )
        }

        composable<PairingRoute> {
            PairingScreen {
                navController.popBackStack(
                    route = DashboardRoute,
                    inclusive = false
                )
            }
        }
    }
}
