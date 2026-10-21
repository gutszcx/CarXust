package com.carxust.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.carxust.core.UserConfig
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted

@Composable
fun PhoneScreen(
    config: UserConfig,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }

    fun call() {
        if (number.isBlank()) return
        val canCall = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val intent = Intent(
            if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.parse("tel:$number")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                Toast.makeText(context, "Nenhum discador disponível", Toast.LENGTH_SHORT).show()
            }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CarBackground)
            .padding(22.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CarCard(modifier = Modifier.weight(0.34f).fillMaxHeight(), accent = accent) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = config.driverName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = CarText
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = CarCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Telefone conectado",
                                style = MaterialTheme.typography.labelSmall,
                                color = CarCyan
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                SectionLabel("Favoritos")
                Spacer(Modifier.height(10.dp))
                ContactRow("Ana", "Celular", "11 99999-1234", accent) {
                    number = "11999991234"
                }
                Spacer(Modifier.height(8.dp))
                ContactRow("Oficina", "Trabalho", "11 3333-9876", accent) {
                    number = "1133339876"
                }
                Spacer(Modifier.height(8.dp))
                ContactRow("Emergência", "Serviço", "192", accent) {
                    number = "192"
                }
                Spacer(Modifier.weight(1f))
                CarCard(accent = accent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SectionLabel("Assistente")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "\"Ligar para Ana\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = CarText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }

        CarCard(modifier = Modifier.weight(0.36f).fillMaxHeight()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = number.ifBlank { "Digite um número" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = if (number.isBlank()) CarTextMuted else CarText,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(CarSurface)
                                        .border(1.dp, CarBorder, RoundedCornerShape(16.dp))
                                        .clickable { number += key },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Light,
                                        color = CarText
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CarSurface)
                            .border(1.dp, CarBorder, CircleShape)
                            .clickable {
                                if (number.isNotEmpty()) number = number.dropLast(1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Backspace,
                            contentDescription = "Apagar",
                            tint = CarTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CarGreen)
                            .clickable { call() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Ligar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Ligar",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        CarCard(modifier = Modifier.weight(0.3f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize()) {
                SectionLabel("Chamadas recentes")
                Spacer(Modifier.height(10.dp))
                RecentRow(Icons.Rounded.Phone, "Ana", "Hoje • 14:02", true, accent)
                Spacer(Modifier.height(8.dp))
                RecentRow(Icons.Rounded.Phone, "Oficina", "Ontem • 09:30", true, accent)
                Spacer(Modifier.height(8.dp))
                RecentRow(Icons.Rounded.Phone, "Desconhecido", "Ontem • 08:12", false, accent)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CarBackground)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Viva-voz",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarTextMuted
                    )
                    Text(
                        text = "Ativado",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    name: String,
    relation: String,
    number: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CarBackground)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = CarText
            )
            Text(
                text = "$relation • $number",
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
        }
        Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = null,
            tint = CarGreen,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RecentRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    detail: String,
    incoming: Boolean,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarBackground)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (incoming) CarGreen else accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = CarText
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
        }
    }
}
