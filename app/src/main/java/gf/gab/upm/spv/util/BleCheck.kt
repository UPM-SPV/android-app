package gf.gab.upm.spv.util

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import gf.gab.upm.spv.ble.IBleClient

@Composable
fun bleCheck(
    bluetoothStatus: IBleClient.BleSupport,
    onEnable: () -> Unit,
) {
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            onEnable()
        }
    }

    LaunchedEffect(Unit) {

        when(bluetoothStatus) {
            IBleClient.BleSupport.SUPPORTED -> onEnable()
            IBleClient.BleSupport.DISABLED -> enableBluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
            else -> {}
        }
    }
}
