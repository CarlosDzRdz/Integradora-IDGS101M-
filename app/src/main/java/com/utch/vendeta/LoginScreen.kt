package com.utch.vendeta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginView(viewModel: VendetaViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading
    val error by viewModel.loginError

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VendetaHeader(title = "NEURO", subtitle = "LAB")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isRegisterMode) "PROTOCOLO: REGISTRO DE AGENTE" else "PROTOCOLO: ACCESO RESTRINGIDO",
            color = Cyan.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("ID DE AGENTE (EMAIL)", color = Cyan.copy(0.6f)) },
            modifier = Modifier.fillMaxWidth(), enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("CLAVE DE ACCESO", color = Cyan.copy(0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
        )

        if (error != null) {
            Text(
                text = "ALERTA: $error",
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Cyan)
        } else {
            // Nota: NeonButton debe ser público en TerminalScreen.kt
            NeonButton(
                text = if (isRegisterMode) "[ CREAR CUENTA ]" else "[ ACCEDER A LA RED ]",
                onClick = {
                    if (isRegisterMode) viewModel.registerWithFirebase(email, pass)
                    else viewModel.loginWithFirebase(email, pass)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isRegisterMode) "¿Ya posee autorización? Inicie sesión" else "¿Es un agente nuevo? Registre su terminal",
                color = DimW, fontSize = 12.sp, textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { isRegisterMode = !isRegisterMode }
            )
        }
    }
}

@Composable
fun VendetaHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(buildAnnotatedString {
            withStyle(SpanStyle(
                brush = Brush.horizontalGradient(listOf(Cyan, White)),
                fontSize = 42.sp, fontWeight = FontWeight.ExtraBold
            )) { append(title) }
            withStyle(SpanStyle(color = White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)) { append(subtitle) }
        })
        Text("SISTEMA DE SEGURIDAD NEUROLAB", color = Cyan.copy(0.5f), fontSize = 10.sp, letterSpacing = 4.sp)
    }
}

/*Pal andres*/