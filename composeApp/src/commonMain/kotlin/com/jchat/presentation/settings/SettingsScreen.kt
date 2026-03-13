package com.jchat.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToReportProblem: () -> Unit,
    onThemeChanged: (ThemeOption) -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(SettingsIntent.DismissError)
        }
    }

    // Propagate theme changes to the root
    LaunchedEffect(state.themeOption) {
        onThemeChanged(state.themeOption)
    }

    if (state.showDeleteAccountDialog) {
        DeleteAccountDialog(
            onConfirm = { viewModel.onIntent(SettingsIntent.ConfirmDeleteAccount) },
            onDismiss = { viewModel.onIntent(SettingsIntent.DismissDeleteAccountDialog) },
        )
    }

    if (state.showThemeDialog) {
        ThemePickerDialog(
            currentTheme = state.themeOption,
            onSelect = { viewModel.onIntent(SettingsIntent.SetTheme(it)) },
            onDismiss = { viewModel.onIntent(SettingsIntent.DismissThemeDialog) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ─── Profile Header ───────────────────────────────────────────────
            item {
                ProfileHeader(
                    profile = state.profile,
                    isLoading = state.isLoading,
                    onClick = onNavigateToProfile,
                )
            }

            // ─── Personalización ──────────────────────────────────────────────
            item { SettingsSectionHeader(title = "Personalización") }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    subtitle = "Recibir alertas de nuevos mensajes",
                    checked = state.notificationsEnabled,
                    onCheckedChange = {
                        viewModel.onIntent(SettingsIntent.SetNotificationsEnabled(it))
                    },
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Palette,
                    title = "Apariencia",
                    subtitle = "Tema: ${state.themeOption.label}",
                    onClick = { viewModel.onIntent(SettingsIntent.ShowThemeDialog) },
                )
            }

            // ─── Privacidad ───────────────────────────────────────────────────
            item { SettingsSectionHeader(title = "Privacidad") }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Visibility,
                    title = "Mostrar presencia",
                    subtitle = "Permite que otros vean cuando estás en línea",
                    checked = state.sharePresence,
                    onCheckedChange = {
                        viewModel.onIntent(SettingsIntent.SetSharePresence(it))
                    },
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Block,
                    title = "Usuarios bloqueados",
                    subtitle = "Gestiona los usuarios que has bloqueado",
                    onClick = onNavigateToBlockedUsers,
                )
            }

            // ─── Cuenta ───────────────────────────────────────────────────────
            item { SettingsSectionHeader(title = "Cuenta") }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Editar perfil",
                    subtitle = "Nombre, avatar y datos de usuario",
                    onClick = onNavigateToProfile,
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Lock,
                    title = "Cambiar contraseña",
                    subtitle = "Actualiza tu contraseña de acceso",
                    onClick = onNavigateToChangePassword,
                )
            }

            // ─── Información ──────────────────────────────────────────────────
            item { SettingsSectionHeader(title = "Información") }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de JChat",
                    subtitle = "Versión 1.0.0 · Licencias · Fuente",
                    onClick = onNavigateToAbout,
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.BugReport,
                    title = "Reportar un problema",
                    subtitle = "Envía un informe de error al equipo",
                    onClick = onNavigateToReportProblem,
                )
            }

            // ─── Zona de peligro ─────────────────────────────────────────────
            item { SettingsSectionHeader(title = "Sesión") }

            item {
                SettingsDangerItem(
                    icon = Icons.Default.Logout,
                    title = if (state.isSigningOut) "Cerrando sesión…" else "Cerrar sesión",
                    onClick = { viewModel.onIntent(SettingsIntent.SignOut) },
                    enabled = !state.isSigningOut,
                )
            }

            item {
                SettingsDangerItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Eliminar cuenta",
                    onClick = { viewModel.onIntent(SettingsIntent.ShowDeleteAccountDialog) },
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ─── Profile Header ──────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    profile: com.jchat.domain.model.Profile?,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = (profile?.displayName ?: profile?.username ?: "?")
                                .take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .fillMaxWidth(0.5f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small,
                            ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.35f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small,
                            ),
                    )
                } else {
                    Text(
                        text = profile?.displayName ?: "Usuario",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "@${profile?.username ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    HorizontalDivider()
}

// ─── Section Header ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

// ─── Clickable Item ──────────────────────────────────────────────────────────

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

// ─── Switch Item ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

// ─── Danger Item ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsDangerItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = if (enabled) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

// ─── Theme Picker Dialog ─────────────────────────────────────────────────────

@Composable
private fun ThemePickerDialog(
    currentTheme: ThemeOption,
    onSelect: (ThemeOption) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apariencia") },
        text = {
            Column {
                ThemeOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentTheme == option,
                            onClick = { onSelect(option) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

// ─── Delete Account Dialog ───────────────────────────────────────────────────

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("¿Eliminar cuenta?") },
        text = {
            Text(
                "Esta acción es permanente e irreversible. Todos tus mensajes y " +
                    "conversaciones serán eliminados. ¿Estás seguro?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
