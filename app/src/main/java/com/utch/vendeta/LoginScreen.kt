/*
package com.utch.vendeta

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * LoginView: Pantalla de autenticación.
 *
 * Los estados de campo (user, pass) SÍ pueden vivir con `remember` aquí
 * porque son efímeros — si el usuario gira el teléfono en el login no
 * hay progreso crítico que perder.
 *
 * El estado de "está logueado" vive en el ViewModel (via onLoginSuccess).
 *
 * 🔥 [FIREBASE — futuro] Reemplazar la validación estática por
 *    signInWithEmailAndPassword(auth, user, pass)
 */
@Composable
fun LoginView(onLoginSuccess: () -> Unit) {

    var user     by remember { mutableStateOf("") }
    var pass     by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    val context  = LocalContext.current

    val inf  = rememberInfiniteTransition(label = "pulse")
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(BgMid, BgDeep), radius = 1400f))
    ) {
        LoginCyberGrid()

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoginLogo(glow = glow)

            Spacer(Modifier.height(48.dp))

            NeonTextField(
                value         = user,
                onValueChange = { user = it; hasError = false },
                label         = "USUARIO"
            )

            Spacer(Modifier.height(16.dp))

            NeonTextField(
                value         = pass,
                onValueChange = { pass = it; hasError = false },
                label         = "CONTRASEÑA",
                isPassword    = true
            )

            if (hasError) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "✕  Credenciales incorrectas. Acceso denegado.",
                    color         = ErrorRed,
                    fontSize      = 11.sp,
                    textAlign     = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    // 🔥 [FIREBASE — futuro] signInWithEmailAndPassword(auth, user, pass)
                    if (user == "admin" && pass == "1234") {
                        onLoginSuccess()
                    } else {
                        hasError = true
                        Toast.makeText(context, "ACCESO DENEGADO", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.5.dp, Cyan, RoundedCornerShape(8.dp)),
                shape     = RoundedCornerShape(8.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = Cyan,
                    contentColor   = BgDeep
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    "INICIAR PROTOCOLO",
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "SISTEMA VENDETA  ·  v1.0  ·  PROTOTIPO",
                color         = DimW.copy(.5f),
                fontSize      = 9.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

// ── Logo ──────────────────────────────────────────────────────────────────────
@Composable
private fun LoginLogo(glow: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(.65f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan.copy(.5f)))))
            Spacer(Modifier.width(8.dp))
            Text("◆", color = Cyan.copy(.6f), fontSize = 8.sp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Cyan.copy(.5f), Color.Transparent))))
        }

        Spacer(Modifier.height(12.dp))

        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(
                    brush         = Brush.horizontalGradient(listOf(Cyan, CyanDim, White)),
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )) { append("VEN") }
                withStyle(SpanStyle(
                    color         = White,
                    fontSize      = 46.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    shadow        = Shadow(color = Cyan.copy(glow * .4f), blurRadius = 24f)
                )) { append("DETA") }
            }
        )

        Spacer(Modifier.height(4.dp))
        Text("AUTENTICACIÓN REQUERIDA", color = Cyan.copy(.55f), fontSize = 9.sp, letterSpacing = 5.sp)
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier.fillMaxWidth(.45f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(
                    Color.Transparent, Cyan.copy(glow * .9f), Color.Transparent
                )))
        )
    }
}

// ── Campo de texto neón ───────────────────────────────────────────────────────
@Composable
private fun NeonTextField(
    value        : String,
    onValueChange: (String) -> Unit,
    label        : String,
    isPassword   : Boolean = false
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label, color = Cyan.copy(.7f), fontSize = 11.sp, letterSpacing = 2.sp) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier             = Modifier.fillMaxWidth(),
        shape                = RoundedCornerShape(8.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = White,
            unfocusedTextColor      = SoftW,
            focusedBorderColor      = Cyan,
            unfocusedBorderColor    = BgBorder,
            cursorColor             = Cyan,
            focusedContainerColor   = BgSurface,
            unfocusedContainerColor = BgSurface
        ),
        singleLine = true
    )
}

// ── Grid decorativo ───────────────────────────────────────────────────────────
@Composable
private fun LoginCyberGrid() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color(0xFF0A1E3A); val s = 56.dp.toPx()
        var x = 0f; while (x < size.width)  { drawLine(c, Offset(x, 0f), Offset(x, size.height), .6f); x += s }
        var y = 0f; while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y),  .6f); y += s }
    }
}

 */
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