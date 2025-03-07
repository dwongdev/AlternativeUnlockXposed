package com.leohearts.alternativeUnlockHook

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.leohearts.alternativeUnlockHook.ui.theme.AlternativeUnlockXposedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

val TAG: String = "alternativeUnlockHook"
val CONFIG_PATH: String = "/data/local/tmp/alternativePass.properties"
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Runtime.getRuntime().exec("su -c 'id > /data/local/tmp/qwq'")
            migrateOldConfig()
            AlternativeUnlockXposedTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsBase()
                }
            }
        }
    }
}

fun sudoFileExists(path: String): Boolean {
    try {
        return BufferedReader(InputStreamReader(sudo("cat " + path).inputStream)).readLine()
            .chars().count() > 0
    }
    catch (e: Exception) {
        return false;
    }
}

fun migrateOldConfig() {
    if (
        (sudoFileExists("/data/data/com.android.systemui/alternativePass.properties"))
        and
        (! sudoFileExists(CONFIG_PATH))

    ) {
        Log.w(TAG, "migrateOldConfig: migrating from old config file")
        sudo("mv /data/data/com.android.systemui/alternativePass.properties ${CONFIG_PATH}")
        setPermission()
    }
}

fun sudo(cmd: String): Process {
    return Runtime.getRuntime().exec(listOf<String>("su", "-c", cmd).toTypedArray())
}

@Composable
fun smallTitle(text: String): Unit {
    return Text(text, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 2.5.em, fontWeight = FontWeight.Medium)
}

@Composable
fun listDivider(): Unit {
    return Divider(modifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(vertical = 24.dp))
}
fun setPermission() {
//    sudo("chown system:system /data/data/com.android.systemui/alternativePass.properties;setenforce 0;chcon u:object_r:platform_app:s0 /data/data/com.android.systemui/alternativePass.properties;setenforce 1")
    sudo("chown `stat /data/data/com.android.systemui/ -c %u`:0 ${CONFIG_PATH}; chmod 770 ${CONFIG_PATH}")
}
fun saveConfig(config: Properties, scope: CoroutineScope, snackbarHostState: SnackbarHostState) {
    config.store(sudo("cat > ${CONFIG_PATH}").outputStream, "")
    setPermission()
    scope.launch {
        snackbarHostState.showSnackbar(
            "Saved to config file"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBase( modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(
            title = { Text("Alternative Unlock Settings")},
            actions = {},
            modifier = modifier.padding(vertical = 10.dp)
        ) }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding
        ) {
            item {
                val config = Properties();
                config.load(sudo("cat ${CONFIG_PATH}").inputStream)

                val openDialog = remember { mutableStateOf(false) }
                val setTitle = rememberSaveable { mutableStateOf("") }
                val setKey = rememberSaveable { mutableStateOf("") }
                val setHint = rememberSaveable { mutableStateOf("") }

                var dynamicLoadchecked by remember {
                    mutableStateOf(
                        config.getProperty(
                            "dynamicLoad",
                            "false"
                        )
                    )
                }
                var hideUIPassword by remember {
                    mutableStateOf(
                        config.getProperty(
                            "hideUIPassword",
                            "false"
                        )
                    )
                }

                smallTitle("Password")
                Surface(onClick = {
                    openDialog.value = true
                    setTitle.value = "Fake Password"
                    setKey.value = "fakePassword"
                    setHint.value = ""
                }) {
                    ListItem(
                        headlineContent = { Text("Fake Password") },
                        supportingContent = { Text(config.getProperty("fakePassword", "Not set")) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Face,
                                contentDescription = "Localized description",
                            )
                        }
                    )
                }

                Surface(onClick = {
                    openDialog.value = true
                    setTitle.value = "Real Password"
                    setKey.value = "realPassword"
                    setHint.value = ""
                }) {
                    ListItem(
                        headlineContent = { Text("Real Password") },
                        supportingContent = { Text(if (hideUIPassword == "true") "******"  else config.getProperty("realPassword", "Not set")) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = "Localized description",
                            )
                        }
                    )
                }

                listDivider()

                smallTitle(text = "Action")
                Surface(onClick = {
                    openDialog.value = true
                    setTitle.value = "When fake password provided"
                    setKey.value = "actionType"
                    setHint.value =
                        "Available options: \nsh: run command with SystemUI permission (platform_app)\nsudo: run command with root"
                }) {
                    ListItem(
                        headlineContent = { Text("When fake password provided") },
                        supportingContent = { Text(config.getProperty("actionType", "sh")) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Localized description",
                            )
                        }
                    )
                }

                Surface(onClick = {
                    openDialog.value = true
                    setTitle.value = "Command"
                    setKey.value = "actionCommand"
                    setHint.value = "Command to execute."
                }) {
                    ListItem(
                        headlineContent = { Text("Command") },
                        supportingContent = { Text(config.getProperty("actionCommand",  "whoami")) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.ArrowForward,
                                contentDescription = "Localized description",
                            )
                        }
                    )
                }

                listDivider()
                smallTitle(text = "Debugging")

                Surface(onClick = {
                    dynamicLoadchecked = if (dynamicLoadchecked == "false") "true" else "false"
                    config.setProperty("dynamicLoad", dynamicLoadchecked)
                    saveConfig(config, scope, snackbarHostState)
                }) {
                    ListItem(
                        headlineContent = { Text("Dynamic config load") },
                        supportingContent = { Text("Load config every time your phone unlocks. Otherwise, you'll need to restart SystemUI to apply changes.") },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = "Localized description",
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = (dynamicLoadchecked == "true"),
                                onCheckedChange = {
                                    dynamicLoadchecked = if (it) "true" else "false"
                                    config.setProperty("dynamicLoad", dynamicLoadchecked)
                                    saveConfig(config, scope, snackbarHostState)
                                }
                            )
                        }
                    )
                }
                Surface(onClick = {
                    sudo("killall com.android.systemui")
                }) {
                    ListItem(
                        headlineContent = { Text("Restart SystemUI") },
                        supportingContent = { Text("Run killall com.android.systemui") },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Localized description",
                            )
                        }
                    )
                }
                listDivider()
                smallTitle(text = "Interface")

                Surface(onClick = {
                    hideUIPassword = if (hideUIPassword == "false") "true" else "false"
                    config.setProperty("hideUIPassword", hideUIPassword)
                    saveConfig(config, scope, snackbarHostState)
                }) {
                    ListItem(
                        headlineContent = { Text("Hide real password from UI") },
                        supportingContent = { Text("Add some protection to shoulder surfing") },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = "icon",
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = (hideUIPassword == "true"),
                                onCheckedChange = {
                                    hideUIPassword = if (it) "true" else "false"
                                    config.setProperty("hideUIPassword", hideUIPassword)
                                    saveConfig(config, scope, snackbarHostState)
                                }
                            )
                        }
                    )
                }

                // config part end
                listDivider()

                if (openDialog.value) {
                    AlertDialog(
                        onDismissRequest = {
                            // Dismiss the dialog when the user clicks outside the dialog or on the back
                            // button. If you want to disable that functionality, simply use an empty
                            // onDismissRequest.
                            openDialog.value = false
                        },
                        title = {
                            Text(text = setTitle.value)
                        },
                        text = {
                            val value = rememberSaveable {
                                mutableStateOf(
                                    config.getProperty(
                                        setKey.value,
                                        ""
                                    )
                                )
                            }
                            LazyColumn {
                                item {
                                    OutlinedTextField(
                                        label = { Text(setTitle.value) },
                                        value = value.value,
                                        onValueChange = {
                                            config.setProperty(setKey.value, it)
                                            value.value = it
                                        })
                                    Text(setHint.value)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    saveConfig(config, scope, snackbarHostState)
                                    openDialog.value = false
                                }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    openDialog.value = false
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AlternativeUnlockXposedTheme {
        SettingsBase()
    }
}