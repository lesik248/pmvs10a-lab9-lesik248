package com.example.weather.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.data.model.Weather
import com.example.weather.platform.PlatformType
import com.example.weather.platform.currentPlatform

@Composable
fun WeatherScreen(vm: WeatherViewModel) {
    val state by vm.state.collectAsState()
    val platform = currentPlatform().type

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValuesCompat())
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Погода",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Платформа: ${currentPlatform().name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            SearchBlock(
                query = state.query,
                onQueryChange = vm::updateQuery,
                onSearch = { vm.search() },
                platform = platform
            )

            if (state.savedCities.size > 1) {
                Spacer(Modifier.height(12.dp))
                CitiesSelector(
                    cities = state.savedCities.map { it.city },
                    selected = state.selectedCity,
                    onSelect = vm::selectCity,
                    platform = platform
                )
            }

            state.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.offlineNotice) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Показаны кешированные данные (офлайн)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            when (platform) {
                PlatformType.Web -> WebGrid(
                    current = state.current,
                    saved = state.savedCities,
                    onSelect = vm::selectCity
                )
                else -> {
                    state.current?.let { w ->
                        WeatherCardForPlatform(w, platform)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBlock(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    platform: PlatformType
) {
    val shape = when (platform) {
        PlatformType.Android -> RoundedCornerShape(28.dp)
        PlatformType.Ios -> RoundedCornerShape(12.dp)
        PlatformType.Desktop -> RoundedCornerShape(4.dp)
        PlatformType.Web -> RoundedCornerShape(8.dp)
    }
    val placeholder = when (platform) {
        PlatformType.Ios -> "Search city"
        else -> "Введите город"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 200.dp),
            singleLine = true,
            shape = shape,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Spacer(Modifier.size(12.dp))
        Button(onClick = onSearch) { Text("Найти") }
    }
}

@Composable
private fun CitiesSelector(
    cities: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    platform: PlatformType
) {
    when (platform) {
        PlatformType.Ios -> SegmentedControl(cities, selected, onSelect)
        else -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            cities.forEach { c ->
                TextButton(
                    onClick = { onSelect(c) },
                    modifier = Modifier.border(
                        BorderStroke(
                            if (c.equals(selected, ignoreCase = true)) 2.dp else 1.dp,
                            if (c.equals(selected, ignoreCase = true))
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        ),
                        RoundedCornerShape(8.dp)
                    )
                ) { Text(c) }
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    items: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        items.forEach { item ->
            val isSelected = item.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onSelect(item) }) {
                    Text(
                        item,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun WebGrid(
    current: Weather?,
    saved: List<Weather>,
    onSelect: (String) -> Unit
) {
    val cells = GridCells.Adaptive(minSize = 280.dp)
    val list = buildList {
        current?.let { add(it) }
        addAll(saved.filter { current == null || !it.city.equals(current.city, ignoreCase = true) })
    }
    if (list.isEmpty()) return
    LazyVerticalGrid(
        columns = cells,
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height((((list.size + 2) / 3) * 240).dp.coerceAtLeast(240.dp))
    ) {
        items(list) { w ->
            WeatherCardForPlatform(w, PlatformType.Web, onClick = { onSelect(w.city) })
        }
    }
}

@Composable
private fun WeatherCardForPlatform(
    weather: Weather,
    platform: PlatformType,
    onClick: (() -> Unit)? = null
) {
    when (platform) {
        PlatformType.Android -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(20.dp)
            ) { WeatherCardContent(weather) }
        }
        PlatformType.Ios -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) { WeatherCardContent(weather) }
        }
        PlatformType.Desktop -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        RoundedCornerShape(4.dp)
                    ),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface
            ) { WeatherCardContent(weather) }
        }
        PlatformType.Web -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) { WeatherCardContent(weather) }
        }
    }
}

@Composable
private fun WeatherCardContent(weather: Weather) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = weather.city,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = weather.description.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${weather.temperature.roundToInt1()}°C",
                style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Light)
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            InfoCell("Влажность", "${weather.humidity}%")
            InfoCell("Ветер", "${weather.windSpeed} м/с")
            InfoCell("Иконка", weather.iconCode)
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun Double.roundToInt1(): Int = (this + if (this >= 0) 0.5 else -0.5).toInt()

@Composable
private fun WindowInsets.asPaddingValuesCompat(): PaddingValues {
    return PaddingValues(0.dp)
}
