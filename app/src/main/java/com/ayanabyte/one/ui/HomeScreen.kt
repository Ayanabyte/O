package com.ayanabyte.one.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF000000)
private val Snow = Color(0xFFFFFFFF)
private val Sky = Color(0xFF4DDCF5)
private val Grey = Color(0xFFB0B0B0)

private data class Section(
    val title: String,
    val description: String
)

private val sections = listOf(
    Section("Televisión", "Canales TDT e IPTV"),
    Section("Radio y música", "Emisoras y servicios"),
    Section("Películas", "Biblioteca VOD"),
    Section("Series", "Episodios y progreso"),
    Section("Ajustes", "Fuentes y preferencias")
)

/** A navigable starter shell, not a simulated television player. */
@Composable
fun HomeScreen() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var sectionOpen by rememberSaveable { mutableStateOf(false) }
    val selectedTileFocus = remember { FocusRequester() }
    val backButtonFocus = remember { FocusRequester() }

    BackHandler(enabled = sectionOpen) { sectionOpen = false }
    LaunchedEffect(sectionOpen) {
        if (sectionOpen) backButtonFocus.requestFocus() else selectedTileFocus.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 54.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("O", color = Snow, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("!", color = Sky, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(22.dp))
            Text(
                text = "ONE ENTERTAINMENT",
                color = Snow,
                fontSize = 15.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(30.dp))

        if (!sectionOpen) {
            Text(
                text = "Todo lo que te gusta. Una sola pantalla.",
                color = Snow,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Base inicial de Android TV. Las fuentes se conectarán en las próximas fases.",
                color = Grey,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.widthIn(max = 698.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                sections.withIndex().chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for ((index, section) in row) {
                            val tile = Modifier.weight(1f)
                            SectionTile(
                                section = section,
                                modifier = if (index == selectedIndex) {
                                    tile.focusRequester(selectedTileFocus)
                                } else {
                                    tile
                                },
                                onClick = {
                                    selectedIndex = index
                                    sectionOpen = true
                                }
                            )
                        }
                        if (row.size == 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Text(
                text = sections[selectedIndex].title,
                color = Snow,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Esta sección aún no tiene fuentes conectadas. La integración real se implementará por etapas; no se mostrarán canales ficticios.",
                color = Grey,
                fontSize = 19.sp,
                modifier = Modifier.widthIn(max = 750.dp)
            )
            Spacer(Modifier.height(34.dp))
            FocusButton(
                text = "Volver al inicio",
                modifier = Modifier.focusRequester(backButtonFocus),
                onClick = { sectionOpen = false }
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "O! · 0.1.0-alpha01 · Estructura inicial",
            color = Color(0xFF777777),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SectionTile(section: Section, modifier: Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .height(112.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(if (isFocused) Color(0xFF1B3038) else Color(0xFF151515))
            .border(2.dp, if (isFocused) Sky else Color(0xFF333333), shape)
            .clickable(onClick = onClick)
            .padding(22.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = section.title,
            color = Snow,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = section.description,
            color = Grey,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FocusButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (isFocused) Sky else Color(0xFF272727))
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp)
    ) {
        Text(
            text = text,
            color = if (isFocused) Ink else Snow,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
