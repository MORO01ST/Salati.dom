package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.content.DhikrCategory
import com.example.data.content.DuaCategory
import com.example.data.content.IslamicContentData
import com.example.data.content.SurahTafsirBundle
import com.example.data.model.Bookmark
import com.example.data.model.CustomSupplication
import com.example.data.model.DailyLog
import com.example.data.network.AlQuranSurahMeta
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.MedinaGold
import com.example.ui.theme.MedinaGoldLight
import com.example.ui.theme.SuccessJade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarQuranScreen(
    todayLog: DailyLog,
    bookmarks: List<Bookmark>,
    customSupplications: List<CustomSupplication>,
    selectedSurah: SurahTafsirBundle,
    remoteSurahs: List<AlQuranSurahMeta>,
    isLoadingSurah: Boolean,
    onIncrementTasbih: (Long) -> Unit,
    onToggleAdhkarPeriod: (Boolean) -> Unit,
    onToggleBookmark: (String, String, String, String) -> Unit,
    onAddCustomSupplication: (String, String, String, String, Long) -> Unit,
    onDeleteCustomSupplication: (String) -> Unit,
    onSelectBundledSurah: (SurahTafsirBundle) -> Unit,
    onLoadRemoteSurah: (Int) -> Unit,
    onNotify: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الأذكار", "الأدعية", "قائمتي", "التفسير")

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("adhkar_subtab_$index")
                )
            }
        }

        when (activeTab) {
            0 -> AdhkarAndTasbihContent(
                todayLog = todayLog,
                bookmarks = bookmarks,
                onIncrementTasbih = onIncrementTasbih,
                onToggleAdhkarPeriod = onToggleAdhkarPeriod,
                onToggleBookmark = onToggleBookmark,
                onNotify = onNotify
            )
            1 -> DiverseDuasContent(
                bookmarks = bookmarks,
                onToggleBookmark = onToggleBookmark,
                onNotify = onNotify
            )
            2 -> CustomFavoritesContent(
                bookmarks = bookmarks,
                customSupplications = customSupplications,
                onIncrementTasbih = onIncrementTasbih,
                onToggleBookmark = onToggleBookmark,
                onAddCustomSupplication = onAddCustomSupplication,
                onDeleteCustomSupplication = onDeleteCustomSupplication,
                onBrowseAdhkar = { activeTab = 0 },
                onBrowseDuas = { activeTab = 1 },
                onNotify = onNotify
            )
            else -> QuranTafsirContent(
                bookmarks = bookmarks,
                selectedSurah = selectedSurah,
                remoteSurahs = remoteSurahs,
                isLoadingSurah = isLoadingSurah,
                onToggleBookmark = onToggleBookmark,
                onSelectBundledSurah = onSelectBundledSurah,
                onLoadRemoteSurah = onLoadRemoteSurah
            )
        }
    }
}

@Composable
private fun AdhkarAndTasbihContent(
    todayLog: DailyLog,
    bookmarks: List<Bookmark>,
    onIncrementTasbih: (Long) -> Unit,
    onToggleAdhkarPeriod: (Boolean) -> Unit,
    onToggleBookmark: (String, String, String, String) -> Unit,
    onNotify: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedCategory by remember { mutableStateOf(DhikrCategory.MORNING) }
    var sessionCount by remember { mutableIntStateOf(0) }
    var tasbihTarget by remember { mutableIntStateOf(33) }
    val itemProgress = remember { mutableStateMapOf<String, Int>() }

    val savedDhikrKeys = remember(bookmarks) {
        bookmarks.filter { it.itemType == "DHIKR" }.map { it.referenceKey }.toSet()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Interactive Haptic Tasbih Counter
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("المسبحة الإلكترونية", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "مجموع تسبيحك اليوم: ${todayLog.dhikrCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { sessionCount = 0 }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "إعادة ضبط الجلسة")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(33, 99, 100).forEach { target ->
                            FilterChip(
                                selected = tasbihTarget == target,
                                onClick = { tasbihTarget = target },
                                label = { Text("الهدف $target") }
                            )
                        }
                    }

                    val progress = (sessionCount % tasbihTarget).toFloat() / tasbihTarget.toFloat()
                    Box(
                        modifier = Modifier
                            .size(164.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                sessionCount++
                                onIncrementTasbih(1L)
                            }
                            .testTag("tasbih_tap_circle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 14.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$sessionCount",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.TouchApp,
                                    contentDescription = null,
                                    tint = MedinaGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "اضغط للتسبيح",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = todayLog.morningAdhkarDone,
                            onClick = { onToggleAdhkarPeriod(true) },
                            leadingIcon = {
                                if (todayLog.morningAdhkarDone) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            },
                            label = { Text("أتممت أذكار الصباح") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = todayLog.eveningAdhkarDone,
                            onClick = { onToggleAdhkarPeriod(false) },
                            leadingIcon = {
                                if (todayLog.eveningAdhkarDone) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            },
                            label = { Text("أتممت أذكار المساء") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Category Chips (الصباح، المساء، بعد الصلاة، النوم، تسابيح)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DhikrCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.arabicLabel) },
                        modifier = Modifier.testTag("dhikr_cat_${cat.name.lowercase()}")
                    )
                }
            }
        }

        val filtered = IslamicContentData.adhkarList.filter { it.category == selectedCategory }
        items(filtered, key = { it.id }) { dhikr ->
            val count = itemProgress[dhikr.id] ?: 0
            val completed = count >= dhikr.targetCount
            val isSaved = savedDhikrKeys.contains(dhikr.id)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (completed) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (count < dhikr.targetCount) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            itemProgress[dhikr.id] = count + 1
                            onIncrementTasbih(1L)
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dhikr.category.arabicLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    onToggleBookmark(
                                        "DHIKR",
                                        dhikr.category.arabicLabel,
                                        dhikr.text.take(120),
                                        dhikr.id
                                    )
                                },
                                modifier = Modifier.testTag("bookmark_dhikr_${dhikr.id}")
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "إضافة الذكر إلى المفضلة",
                                    tint = if (isSaved) MedinaGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString("${dhikr.text}\n(${dhikr.virtue})"))
                                    onNotify("تم نسخ الذكر")
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الذكر")
                            }
                            IconButton(
                                onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${dhikr.category.arabicLabel}\n\n${dhikr.text}\n\n(${dhikr.virtue})\nعبر تطبيق صلاتي"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(share, "مشاركة الذكر"))
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "مشاركة الذكر")
                            }
                        }
                    }

                    Text(
                        text = dhikr.text,
                        fontFamily = AmiriFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dhikr.virtue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (completed) SuccessJade else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (completed) "تم الذكر ✓" else "$count / ${dhikr.targetCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (completed) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        if (count > 0) {
                            IconButton(onClick = { itemProgress[dhikr.id] = 0 }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "إعادة", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiverseDuasContent(
    bookmarks: List<Bookmark>,
    onToggleBookmark: (String, String, String, String) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedCategory by remember { mutableStateOf<DuaCategory?>(null) }
    var showSavedOnly by remember { mutableStateOf(false) }

    val savedKeys = remember(bookmarks) {
        bookmarks.filter { it.itemType == "DUA" }.map { it.referenceKey }.toSet()
    }

    val filtered = remember(selectedCategory, showSavedOnly, savedKeys) {
        IslamicContentData.duasList.filter { dua ->
            val catMatch = selectedCategory == null || dua.category == selectedCategory
            val favMatch = !showSavedOnly || savedKeys.contains(dua.id)
            catMatch && favMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = !showSavedOnly && selectedCategory == null,
                        onClick = {
                            showSavedOnly = false
                            selectedCategory = null
                        },
                        label = { Text("الكل") }
                    )
                }
                item {
                    FilterChip(
                        selected = showSavedOnly,
                        onClick = { showSavedOnly = !showSavedOnly },
                        label = { Text("المحفوظة (${savedKeys.size})") }
                    )
                }
                items(DuaCategory.entries) { cat ->
                    FilterChip(
                        selected = !showSavedOnly && selectedCategory == cat,
                        onClick = {
                            showSavedOnly = false
                            selectedCategory = cat
                        },
                        label = { Text(cat.arabicLabel) },
                        modifier = Modifier.testTag("dua_cat_${cat.name.lowercase()}")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.BookmarkBorder, contentDescription = null, modifier = Modifier.size(36.dp), tint = MedinaGold)
                        Text("لا توجد أدعية محفوظة بعد", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "اضغط على أيقونة الحفظ بجانب أي دعاء للرجوع إليه في قائمتك المفضلة",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { dua ->
                val isSaved = savedKeys.contains(dua.id)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dua.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${dua.category.arabicLabel} • ${dua.source}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        onToggleBookmark("DUA", dua.title, dua.source, dua.id)
                                    },
                                    modifier = Modifier.testTag("bookmark_dua_${dua.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "حفظ الدعاء في المفضلة",
                                        tint = if (isSaved) MedinaGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("${dua.arabicText}\n(${dua.source})"))
                                        onNotify("تم نسخ الدعاء")
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الدعاء")
                                }
                                IconButton(
                                    onClick = {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${dua.title}\n\n${dua.arabicText}\n\n(${dua.source})\nعبر تطبيق صلاتي")
                                        }
                                        context.startActivity(Intent.createChooser(share, "مشاركة الدعاء"))
                                    }
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "مشاركة الدعاء")
                                }
                            }
                        }

                        Text(
                            text = dua.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 21.sp,
                            lineHeight = 36.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomFavoritesContent(
    bookmarks: List<Bookmark>,
    customSupplications: List<CustomSupplication>,
    onIncrementTasbih: (Long) -> Unit,
    onToggleBookmark: (String, String, String, String) -> Unit,
    onAddCustomSupplication: (String, String, String, String, Long) -> Unit,
    onDeleteCustomSupplication: (String) -> Unit,
    onBrowseAdhkar: () -> Unit,
    onBrowseDuas: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var showAddDialog by remember { mutableStateOf(false) }
    val customCounts = remember { mutableStateMapOf<String, Int>() }

    val favoriteAdhkar = remember(bookmarks) {
        val keys = bookmarks.filter { it.itemType == "DHIKR" }.map { it.referenceKey }.toSet()
        IslamicContentData.adhkarList.filter { keys.contains(it.id) }
    }
    val favoriteDuas = remember(bookmarks) {
        val keys = bookmarks.filter { it.itemType == "DUA" }.map { it.referenceKey }.toSet()
        IslamicContentData.duasList.filter { keys.contains(it.id) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MedinaGold
                        )
                        Text(
                            text = "قائمتي المفضلة للأذكار والأدعية",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "اجمع أذكارك وأدعيتك الخاصة أو احفظ ما تختاره من الأذكار وأدعية المناسبات للوصول السريع في أي وقت.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_custom_supplication_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("إضافة ذكر أو دعاء خاص")
                    }
                }
            }
        }

        if (customSupplications.isNotEmpty()) {
            item {
                Text(
                    text = "أذكاري وأدعيتي الخاصة (${customSupplications.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(customSupplications, key = { it.id }) { item ->
                val count = customCounts[item.id] ?: 0
                val target = item.targetCount.toInt().coerceAtLeast(1)
                val completed = count >= target
                val typeLabel = if (item.supplicationType == "DHIKR") "ذكر خاص" else "دعاء خاص"

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (completed) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (count < target) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                customCounts[item.id] = count + 1
                                onIncrementTasbih(1L)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val subtitle = if (item.occasionNote.isNotBlank()) {
                                    "$typeLabel • ${item.occasionNote}"
                                } else {
                                    typeLabel
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("${item.title}\n${item.arabicText}"))
                                        onNotify("تم النسخ")
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ")
                                }
                                IconButton(
                                    onClick = {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.arabicText}\n\nعبر تطبيق صلاتي")
                                        }
                                        context.startActivity(Intent.createChooser(share, "مشاركة"))
                                    }
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "مشاركة")
                                }
                                IconButton(
                                    onClick = { onDeleteCustomSupplication(item.id) },
                                    modifier = Modifier.testTag("delete_custom_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "حذف من القائمة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Text(
                            text = item.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 21.sp,
                            lineHeight = 35.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (completed) SuccessJade else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (completed) "أُنجز ✓" else "$count / $target",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (completed) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                            if (count > 0) {
                                IconButton(onClick = { customCounts[item.id] = 0 }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "إعادة العداد", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (favoriteAdhkar.isNotEmpty()) {
            item {
                Text(
                    text = "أذكار مفضلة (${favoriteAdhkar.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(favoriteAdhkar, key = { "fav_dhikr_${it.id}" }) { dhikr ->
                val count = customCounts["dhikr_${dhikr.id}"] ?: 0
                val completed = count >= dhikr.targetCount
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (count < dhikr.targetCount) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                customCounts["dhikr_${dhikr.id}"] = count + 1
                                onIncrementTasbih(1L)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dhikr.category.arabicLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        onToggleBookmark("DHIKR", dhikr.category.arabicLabel, dhikr.text.take(120), dhikr.id)
                                    }
                                ) {
                                    Icon(Icons.Filled.Bookmark, contentDescription = "إزالة من المفضلة", tint = MedinaGold)
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("${dhikr.text}\n(${dhikr.virtue})"))
                                        onNotify("تم نسخ الذكر")
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ")
                                }
                            }
                        }
                        Text(
                            text = dhikr.text,
                            fontFamily = AmiriFontFamily,
                            fontSize = 20.sp,
                            lineHeight = 34.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (completed) SuccessJade else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (completed) "تم الذكر ✓" else "$count / ${dhikr.targetCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (completed) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (favoriteDuas.isNotEmpty()) {
            item {
                Text(
                    text = "أدعية مفضلة (${favoriteDuas.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(favoriteDuas, key = { "fav_dua_${it.id}" }) { dua ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dua.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${dua.category.arabicLabel} • ${dua.source}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { onToggleBookmark("DUA", dua.title, dua.source, dua.id) }
                                ) {
                                    Icon(Icons.Filled.Bookmark, contentDescription = "إزالة من المفضلة", tint = MedinaGold)
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("${dua.arabicText}\n(${dua.source})"))
                                        onNotify("تم نسخ الدعاء")
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الدعاء")
                                }
                            }
                        }
                        Text(
                            text = dua.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 20.sp,
                            lineHeight = 34.sp
                        )
                    }
                }
            }
        }

        if (customSupplications.isEmpty() && favoriteAdhkar.isEmpty() && favoriteDuas.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            tint = MedinaGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("قائمتك المفضلة فارغة حالياً", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "يمكنك إضافة ذكر أو دعاء خاص بك، أو الضغط على أيقونة الحفظ بجانب أذكار الصباح والمساء وبعد الصلاة وأدعية المناسبات.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBrowseAdhkar,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تصفح الأذكار")
                            }
                            OutlinedButton(
                                onClick = onBrowseDuas,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تصفح الأدعية")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomSupplicationDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, title, text, occasion, target ->
                onAddCustomSupplication(type, title, text, occasion, target)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomSupplicationDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long) -> Unit
) {
    var selectedType by remember { mutableStateOf("DUA") }
    var title by remember { mutableStateOf("") }
    var arabicText by remember { mutableStateOf("") }
    var occasionNote by remember { mutableStateOf("") }
    var targetCount by remember { mutableStateOf(1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة إلى قائمتي المفضلة") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == "DUA",
                        onClick = { selectedType = "DUA" },
                        label = { Text("دعاء خاص") },
                        modifier = Modifier.testTag("custom_type_dua")
                    )
                    FilterChip(
                        selected = selectedType == "DHIKR",
                        onClick = { selectedType = "DHIKR" },
                        label = { Text("ذكر خاص") },
                        modifier = Modifier.testTag("custom_type_dhikr")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("العنوان") },
                    placeholder = { Text("مثال: دعاء لوالدي أو ورد الاستغفار") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_supplication_title_input")
                )

                OutlinedTextField(
                    value = arabicText,
                    onValueChange = { arabicText = it },
                    label = { Text("نص الذكر أو الدعاء") },
                    placeholder = { Text("اكتب النص هنا...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_supplication_text_input")
                )

                OutlinedTextField(
                    value = occasionNote,
                    onValueChange = { occasionNote = it },
                    label = { Text("المناسبة أو الملاحظة (اختياري)") },
                    placeholder = { Text("مثال: بعد الفجر، في السفر، عند المطر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("عدد التكرار المطلوب:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(1L, 3L, 7L, 33L, 100L)) { count ->
                        FilterChip(
                            selected = targetCount == count,
                            onClick = { targetCount = count },
                            label = { Text("$count") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedType, title, arabicText, occasionNote, targetCount) },
                enabled = title.isNotBlank() && arabicText.isNotBlank(),
                modifier = Modifier.testTag("save_custom_supplication_button")
            ) {
                Text("حفظ في المفضلة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuranTafsirContent(
    bookmarks: List<Bookmark>,
    selectedSurah: SurahTafsirBundle,
    remoteSurahs: List<AlQuranSurahMeta>,
    isLoadingSurah: Boolean,
    onToggleBookmark: (String, String, String, String) -> Unit,
    onSelectBundledSurah: (SurahTafsirBundle) -> Unit,
    onLoadRemoteSurah: (Int) -> Unit
) {
    var showSurahPicker by remember { mutableStateOf(false) }
    var fontScaleSp by remember { mutableFloatStateOf(22f) }
    val savedTafsirKeys = remember(bookmarks) {
        bookmarks.filter { it.itemType == "TAFSIR" }.map { it.referenceKey }.toSet()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quran Illuminated Header
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_quran_banner),
                        contentDescription = "تفسير القرآن الكريم",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x66062E24), Color(0xDD041C16))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedSurah.nameArabic,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedSurah.revelationType} • ${selectedSurah.totalAyahs} آية • التفسير الميسر",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MedinaGoldLight
                                )
                            }
                            Button(
                                onClick = { showSurahPicker = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("pick_surah_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("اختر السورة")
                            }
                        }

                        Text(
                            text = selectedSurah.themeSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Quick Bundled Surahs + Font Slider
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IslamicContentData.bundledSurahs, key = { it.surahNumber }) { surah ->
                        FilterChip(
                            selected = selectedSurah.surahNumber == surah.surahNumber,
                            onClick = { onSelectBundledSurah(surah) },
                            label = { Text(surah.nameArabic) }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("حجم خط الآية:", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = fontScaleSp,
                        onValueChange = { fontScaleSp = it },
                        valueRange = 18f..32f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${fontScaleSp.toInt()}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (isLoadingSurah) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(selectedSurah.ayahs, key = { "${selectedSurah.surahNumber}_${it.numberInSurah}" }) { ayah ->
                val refKey = "s${selectedSurah.surahNumber}_a${ayah.numberInSurah}"
                val isBookmarked = savedTafsirKeys.contains(refKey)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "آية ${ayah.numberInSurah}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    onToggleBookmark(
                                        "TAFSIR",
                                        "${selectedSurah.nameArabic} - آية ${ayah.numberInSurah}",
                                        ayah.arabicText.take(100),
                                        refKey
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "حفظ الآية والتفسير",
                                    tint = if (isBookmarked) MedinaGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = ayah.arabicText,
                            fontFamily = AmiriFontFamily,
                            fontSize = fontScaleSp.sp,
                            lineHeight = (fontScaleSp * 1.65f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "التفسير الميسر",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = ayah.tafsirMuyassar.ifBlank { "التفسير غير متوفر لهذه الآية حالياً." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSurahPicker) {
        ModalBottomSheet(onDismissRequest = { showSurahPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("فهرس سور القرآن الكريم", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "اختر أي سورة لقراءة الآيات مع التفسير الميسر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (remoteSurahs.isNotEmpty()) {
                        items(remoteSurahs, key = { it.number }) { meta ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLoadRemoteSurah(meta.number)
                                        showSurahPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${meta.number}. ${meta.name}", style = MaterialTheme.typography.titleMedium)
                                    Text("${meta.numberOfAyahs} آية", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    } else {
                        items(IslamicContentData.bundledSurahs, key = { it.surahNumber }) { surah ->
                            OutlinedButton(
                                onClick = {
                                    onSelectBundledSurah(surah)
                                    showSurahPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("${surah.surahNumber}. ${surah.nameArabic} (${surah.totalAyahs} آيات)")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
