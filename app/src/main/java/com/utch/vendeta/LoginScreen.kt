package com.utch.vendeta

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Clase encargada de la lógica y diseño del Login.
 * Nota: En Compose usamos funciones, pero la encapsulamos
 * para que actúe como un módulo independiente.
 */
@Composable
fun LoginView(onLoginSuccess: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Usamos buildAnnotatedString para combinar diferentes estilos en un solo texto
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Cyan, fontWeight = FontWeight.ExtraBold)) {
                    append("NEURO")
                }
                withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                    append("LAB")
                }
            },
            fontSize = 42.sp // El tamaño de fuente se aplica a todo el texto
        )
        Text("AUTENTICACIÓN REQUERIDA", color = Color.White, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("USUARIO", color = Cyan) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("CONTRASEÑA", color = Cyan) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Validación con credenciales fijas
                if (user == "admin" && pass == "1234") {
                    onLoginSuccess()
                } else {
                    Toast.makeText(context, "ACCESO DENEGADO", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = BgDeep)
        ) {
            Text("INICIAR PROTOCOLO", fontWeight = FontWeight.Bold)
        }
    }
}