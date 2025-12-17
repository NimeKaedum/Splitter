package com.example.livesplitlike.ui.screens.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.systemBarsPadding
import com.example.livesplitlike.R

@Composable
fun HelpScreen(
    navController: NavController
) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()                          // padding top/bottom seguro
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        // --- Header ---
        Text(
            text = "Ayuda & Tutorial",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Volver
        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Volver")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Sección: ¿Qué es LiveSplitLike? ---
        SectionTitle(title = "¿Qué es LiveSplitLike?")
        Text(
            text = "LiveSplitLike es una app para cronometrar speedruns desde tu móvil. " +
                    "Te permite crear grupos de splits, ejecutar runs, comparar tiempos (PB, Best Possible) y compartir tus mejores marcas. " +
                    "Diseñada para ser rápida y fácil: menos configuración, más práctica.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

// --- Sección: ¿Qué es un speedrun? ---
        SectionTitle(title = "¿Qué es un speedrun?")
        Text(
            text = "Un speedrun consiste en terminar un juego lo más rápido posible siguiendo las reglas de una categoría. " +
                    "No es solo correr: implica planear rutas, aprender atajos y repetir secciones hasta mejorar la ejecución.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

// --- Sección: ¿Para qué quiero un cronómetro en un speedrun? ---
        SectionTitle(title = "¿Para qué quiero un cronómetro en un speedrun?")
        Text(
            text = "Un cronómetro te da información precisa para mejorar: marca splits (segmentos del run), compara tu tiempo actual con tu PB (Mejor tiempo personal) y con el Best Possible (Mejor tiempo posible), " +
                    "y te muestra exactamente en qué split ganas o pierdes tiempo. También facilita guardar y compartir runs para recibir feedback.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

// --- Sección: Funciones clave que debes conocer ---
        SectionTitle(title = "Funciones clave que debes conocer")
        Text(
            text = "Comparación de splits: compara tu tiempo actual con PB y con runs anteriores.\n" +
                    "Best Possible: calcula el tiempo teórico si juntas tus mejores segmentos.\n" +
                    "Overlay: muestra el cronómetro sobre el juego para poder ver en todo momento tu tiempo.\n" +
                    "Guardado en la nube: sincroniza grupos, splits y runs entre dispositivos iniciando sesión.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

// --- Sección: ¿Dónde encuentro info sobre qué juegos correr? ---
        SectionTitle(title = "¿Dónde encuentro qué juegos correr?")
        Text(
            text = "Visita Speedrun.com para ver juegos, categorías y reglas. Un mismo juego puede tener varias categorías (any%, 100%, glitchless, etc.), " +
                    "y cada categoría define qué está permitido. Antes de competir o subir un run, lee las reglas de la categoría.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Link con Intent
        Text(
            text = "Abrir Speedrun.com",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.speedrun.com"))
                    ctx.startActivity(intent)
                }
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))


// --- Sección: Primeros pasos para empezar a speedrunear ---
        SectionTitle(title = "Primeros pasos para empezar a speedrunear")
        Text(
            text = "1. Busca la categoría en Speedrun.com y aprende sus reglas.\n" +
                    "2. Crea un grupo de splits en la app (nombre + número de splits).\n" +
                    "3. Añade los splits más comunes de la categoría (niveles, jefes, checkpoints).\n" +
                    "4. Practica runs cortos y segmentos sueltos antes de intentar runs completos.\n" +
                    "5. Usa el overlay o la app móvil si no tienes LiveSplit en PC (útil para consola).\n" +
                    "6. Guarda tus runs en la nube, compara PB y BPT, ajusta rutas y repite.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

// --- Sección: ¿Qué es el overlay y por qué usarlo? ---
        SectionTitle(title = "¿Qué es el overlay y por qué usarlo?")
        Text(
            text = "El overlay muestra el cronómetro encima del juego para que no tengas que cambiar de ventana. " +
                    "Es ideal si quieres realizar speedruns de juegos de celular, o usar un emulador y hacer speedrun con la ventaja de la mobilidad del celular!.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

// --- Sección: Guardado en la nube ---
        SectionTitle(title = "Guardado en la nube")
        Text(
            text = "Inicia sesión (por ejemplo con Google) para subir y descargar tus datos: grupos, splits y runs. " +
                    "Así no pierdes tu progreso al cambiar de dispositivo o reinstalar la app.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Sección: Buenas prácticas / Tips ---
        SectionTitle(title = "Consejos rápidos")
        Text(
            text = "• Practica segmentos: trabaja un split a la vez.\n" +
                    "• Guarda tus PB y compártelos.\n" +
                    "• Participa en la comunidad para aprender rutas y técnicas.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))


        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}
