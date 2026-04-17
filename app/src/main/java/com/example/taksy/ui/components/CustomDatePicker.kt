package com.example.taksy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Calendario personalizado que muestra lunes a domingo
 */
@Composable
fun CustomDatePicker(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Configurar el mes actual con la fecha seleccionada
    LaunchedEffect(selectedDate) {
        currentMonth = Calendar.getInstance().apply {
            time = selectedDate
        }
    }
    
    val calendar = remember(currentMonth) {
        Calendar.getInstance().apply {
            time = currentMonth.time
            set(Calendar.DAY_OF_MONTH, 1) // Primer día del mes
        }
    }
    
    val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(calendar.time)
    
    // Ajustar para que lunes sea el primer día (Calendar.MONDAY = 2)
    val firstMonday = if (firstDayOfMonth == Calendar.SUNDAY) 1 else (firstDayOfMonth - Calendar.MONDAY + 1)
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Header con navegación
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    calendar.add(Calendar.MONTH, -1)
                    currentMonth = calendar.clone() as Calendar
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Mes anterior",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            IconButton(
                onClick = {
                    calendar.add(Calendar.MONTH, 1)
                    currentMonth = calendar.clone() as Calendar
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Mes siguiente",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Días de la semana
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        // Grid del calendario
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Calcular las semanas del mes
            val weeks = mutableListOf<MutableList<Int>>()
            var currentWeek = mutableListOf<Int>()
            
            // Añadir espacios vacíos antes del primer día
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val emptyDays = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
            
            repeat(emptyDays) {
                currentWeek.add(0) // 0 = día vacío
            }
            
            // Añadir todos los días del mes
            for (day in 1..daysInMonth) {
                currentWeek.add(day)
                
                // Si la semana está completa (7 días), empezar nueva semana
                if (currentWeek.size == 7) {
                    weeks.add(currentWeek)
                    currentWeek = mutableListOf()
                }
            }
            
            // Si queda una semana incompleta, rellenarla con espacios vacíos
            if (currentWeek.isNotEmpty()) {
                while (currentWeek.size < 7) {
                    currentWeek.add(0) // 0 = día vacío
                }
                weeks.add(currentWeek)
            }
            
            // Renderizar las semanas
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    week.forEach { day ->
                        val isSelected = day != 0 && isSameDay(selectedDate, day, calendar)
                        val isToday = day != 0 && isToday(day, calendar)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    enabled = day != 0
                                ) {
                                    if (day != 0) {
                                        val newDate = Calendar.getInstance().apply {
                                            time = calendar.time
                                            set(Calendar.DAY_OF_MONTH, day)
                                        }.time
                                        onDateSelected(newDate)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != 0) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isSameDay(selectedDate: Date, day: Int, calendar: Calendar): Boolean {
    val selectedCalendar = Calendar.getInstance().apply { time = selectedDate }
    return selectedCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            selectedCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
            selectedCalendar.get(Calendar.DAY_OF_MONTH) == day
}

private fun isToday(day: Int, calendar: Calendar): Boolean {
    val today = Calendar.getInstance()
    return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
            today.get(Calendar.DAY_OF_MONTH) == day
}
