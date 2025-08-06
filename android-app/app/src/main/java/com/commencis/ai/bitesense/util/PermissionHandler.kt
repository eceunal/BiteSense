package com.commencis.ai.bitesense.util

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

data class PermissionHandler(
    val isGranted: Boolean,
    val shouldShowRationale: Boolean,
    val requestPermission: () -> Unit,
    val handlePermanentlyDenied: () -> Unit
)

@Composable
fun rememberPermissionHandler(
    permission: String,
    onPermissionGrant: (() -> Unit)? = null
): PermissionHandler {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }
    var hasRequestedPermission by remember { mutableStateOf(false) }

    // Check current permission status
    val isGranted = remember(permission) {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Check if we should show rationale
    val shouldShowRationale = remember(permission, hasRequestedPermission) {
        activity?.shouldShowRequestPermissionRationale(permission) ?: false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedPermission = true
        if (granted) {
            onPermissionGrant?.invoke()
        } else {
            // Check if permanently denied
            val currentShouldShowRationale = activity?.shouldShowRequestPermissionRationale(permission) ?: false
            if (!currentShouldShowRationale && hasRequestedPermission) {
                showPermanentlyDeniedDialog = true
            }
        }
    }

    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = { Text("Camera Permission Required") },
            text = { Text("This app needs camera permission to scan bites. Please enable it in settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermanentlyDeniedDialog = false
                        // Open app settings
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentlyDeniedDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    return PermissionHandler(
        isGranted = isGranted,
        shouldShowRationale = shouldShowRationale,
        requestPermission = {
            permissionLauncher.launch(permission)
        },
        handlePermanentlyDenied = {
            showPermanentlyDeniedDialog = true
        }
    )
}