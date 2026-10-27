package com.example.frontend.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.ui.EstadoLogin
import com.example.frontend.ui.theme.AppColors

// Inicio de sesion y registro contra /api/v1/auth (JWT). Sin recuperacion de contrasena.
@Composable
fun PantallaLogin(
    estado: EstadoLogin,
    hayConexion: Boolean,
    alEntrar: (email: String, password: String, registrando: Boolean) -> Unit,
    alEditar: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var registrando by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Validacion local antes de gastar una peticion: el backend pide email valido y minimo 8 caracteres al registrarse.
    val emailValido = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())
    val passwordValida = if (registrando) password.length >= 8 else password.isNotEmpty()
    val puedeEnviar = emailValido && passwordValida && !estado.cargando && hayConexion

    Scaffold(modifier = modifier, containerColor = AppColors.cream) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.espresso)
                }
                Text(
                    if (registrando) "Create account" else "Sign in",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                )
            }
            Text(
                "Your account lets us skip places you've already tried and keeps your reports linked to you.",
                fontSize = 13.sp,
                color = AppColors.muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )

            // Conectividad eventual: el login necesita red; se avisa aqui, no con un mensaje general de la app.
            if (!hayConexion) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AppColors.tomato, modifier = Modifier.size(16.dp))
                    Text(
                        "  You're offline. Sign in when you reconnect; the rest of the app keeps working.",
                        fontSize = 12.sp,
                        color = AppColors.tomato,
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; alEditar() },
                label = { Text("University email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = email.isNotEmpty() && !emailValido,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.tomato, cursorColor = AppColors.tomato),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; alEditar() },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = { if (registrando) Text("At least 8 characters") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.tomato, cursorColor = AppColors.tomato),
                modifier = Modifier.fillMaxWidth(),
            )

            if (estado.error != null) {
                Text(estado.error, fontSize = 13.sp, color = AppColors.tomato, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { alEntrar(email, password, registrando) },
                enabled = puedeEnviar,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.tomato, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(if (registrando) "Create account" else "Sign in", fontWeight = FontWeight.ExtraBold)
                }
            }
            TextButton(
                onClick = { registrando = !registrando; alEditar() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    if (registrando) "I already have an account" else "New here? Create an account",
                    color = AppColors.tomato,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
