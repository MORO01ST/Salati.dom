package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CircleMember
import com.example.data.model.DailyLog
import com.example.data.model.MemorizationCircle
import com.example.ui.theme.MedinaGold
import com.example.ui.theme.SuccessJade

@Composable
fun HalaqatScreen(
    currentUserId: String,
    isGuestMode: Boolean,
    todayLog: DailyLog,
    myCircles: List<MemorizationCircle>,
    publicCircles: List<MemorizationCircle>,
    selectedCircle: MemorizationCircle?,
    circleMembers: List<CircleMember>,
    onUpdateWird: (Long, Long) -> Unit,
    onSelectCircle: (MemorizationCircle?) -> Unit,
    onCreateCircle: (String, String, Long) -> Unit,
    onJoinCircle: (MemorizationCircle) -> Unit,
    onLogMemorizedVerses: (MemorizationCircle, Long, String) -> Unit,
    onShareWirdAchievement: (Long) -> Unit,
    onRequestGoogleSignIn: () -> Unit
) {
    if (selectedCircle != null) {
        CircleDetailView(
            currentUserId = currentUserId,
            circle = selectedCircle,
            members = circleMembers,
            onBack = { onSelectCircle(null) },
            onJoin = { onJoinCircle(selectedCircle) },
            onLogVerses = { added, note -> onLogMemorizedVerses(selectedCircle, added, note) }
        )
        return
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var circleFilterMine by remember { mutableStateOf(false) }

    val displayedCircles = remember(circleFilterMine, myCircles, publicCircles) {
        if (circleFilterMine) myCircles else publicCircles
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Wird Tracker Card
        item {
            val done = todayLog.wirdPagesDone
            val target = todayLog.wirdTargetPages.coerceAtLeast(1L)
            val ratio = (done.toFloat() / target.toFloat()).coerceIn(0f, 1f)
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تتبع الورد اليومي", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "الهدف اليومي: $target صفحات",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showTargetDialog = true }) {
                            Text("تعديل الهدف")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 11.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = if (done >= target) SuccessJade else primaryColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * ratio,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$done / $target",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("صفحة", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (done > 0L) onUpdateWird(done - 1L, target)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "إنقاص صفحة")
                                    }
                                }
                                Button(
                                    onClick = { onUpdateWird(done + 1L, target) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("add_wird_page_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("أتممت صفحة")
                                }
                            }

                            if (done >= target) {
                                OutlinedButton(
                                    onClick = { onShareWirdAchievement(done) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MedinaGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("مشاركة إتمام الورد")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Group Memorization Header & Actions
        if (isGuestMode) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "حلقات التحفيظ الجماعي السحابية",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Text(
                            text = "تتبع الورد اليومي يعمل بالكامل دون تسجيل دخول أو إنترنت. للانضمام إلى حلقات التحفيظ الجماعية والمزامنة مع الأصدقاء، يمكنك تسجيل الدخول بحساب Google.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Button(
                            onClick = onRequestGoogleSignIn,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("halaqat_guest_signin_button")
                        ) {
                            Text("تسجيل الدخول بحساب Google")
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("التحفيظ الجماعي (الحلقات)", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "تنافس مع إخوانك في حفظ كتاب الله",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("create_circle_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("حلقة جديدة")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !circleFilterMine,
                        onClick = { circleFilterMine = false },
                        label = { Text("الحلقات المتاحة (${publicCircles.size})") }
                    )
                    FilterChip(
                        selected = circleFilterMine,
                        onClick = { circleFilterMine = true },
                        label = { Text("حلقاتي (${myCircles.size})") }
                    )
                }
            }

            if (displayedCircles.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("لا توجد حلقات حالياً في هذه القائمة", style = MaterialTheme.typography.titleMedium)
                            Text("أنشئ أول حلقة تحفيظ جماعي وادعُ أصدقاءك!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(displayedCircles, key = { it.id }) { circle ->
                    val isMember = circle.memberUids.contains(currentUserId)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCircle(circle) }
                            .testTag("circle_card_${circle.id}")
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
                                    Text(circle.title, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "${circle.surahName} • الهدف ${circle.targetVerses} آية",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isMember) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "منضم ✓",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(onClick = { onJoinCircle(circle) }) {
                                        Text("انضمام")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الأعضاء: ${circle.memberCount}", style = MaterialTheme.typography.labelMedium)
                                Text("إجمالي المحفوظ: ${circle.totalMemorizedVerses} آية", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var surah by remember { mutableStateOf("سورة الملك") }
        var target by remember { mutableStateOf("30") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("إنشاء حلقة تحفيظ جماعي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم الحلقة (مثال: حلقة الفجر)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = surah,
                        onValueChange = { surah = it },
                        label = { Text("السورة أو المقرر") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter { c -> c.isDigit() } },
                        label = { Text("عدد الآيات المستهدف") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val verses = target.toLongOrNull()?.coerceIn(1L, 6236L) ?: 30L
                        if (title.trim().length >= 2 && surah.isNotBlank()) {
                            onCreateCircle(title.trim(), surah.trim(), verses)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("إنشاء الحلقة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showTargetDialog) {
        var newTarget by remember { mutableStateOf(todayLog.wirdTargetPages.toString()) }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("تعديل هدف الورد اليومي") },
            text = {
                OutlinedTextField(
                    value = newTarget,
                    onValueChange = { newTarget = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد الصفحات يومياً (١ - ٦٠٤)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val t = newTarget.toLongOrNull()?.coerceIn(1L, 604L) ?: 4L
                    onUpdateWird(todayLog.wirdPagesDone, t)
                    showTargetDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun CircleDetailView(
    currentUserId: String,
    circle: MemorizationCircle,
    members: List<CircleMember>,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onLogVerses: (Long, String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isMember = circle.memberUids.contains(currentUserId)
    var showLogDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                    Text(circle.title, style = MaterialTheme.typography.headlineSmall)
                }
                IconButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "انضم إليّ في حلقة التحفيظ «${circle.title}» (${circle.surahName}) على تطبيق صلاتي! رمز الدعوة: ${circle.inviteCode}"
                        )
                    }
                    context.startActivity(Intent.createChooser(share, "دعوة للحلقة"))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "مشاركة دعوة الحلقة")
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("${circle.surahName} • رمز الدعوة: ${circle.inviteCode}", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("إجمالي الآيات المحفوظة في الحلقة: ${circle.totalMemorizedVerses} آية • الأعضاء: ${circle.memberCount}", style = MaterialTheme.typography.bodyMedium)
                    if (isMember) {
                        Button(
                            onClick = { showLogDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تسجيل حفظي اليوم")
                        }
                    } else {
                        Button(
                            onClick = onJoin,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("انضمام إلى الحلقة")
                        }
                    }
                }
            }
        }

        item {
            Text("ترتيب الأعضاء في الحلقة", style = MaterialTheme.typography.titleLarge)
        }

        items(members, key = { it.userId }) { member ->
            val progress = (member.memorizedVerses.toFloat() / circle.targetVerses.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (member.userId == currentUserId) "${member.displayName} (أنت)" else member.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${member.memorizedVerses} / ${circle.targetVerses} آية",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    if (member.lastSurahNote.isNotBlank()) {
                        Text(member.lastSurahNote, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        var countText by remember { mutableStateOf("5") }
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("تسجيل آيات جديدة محفوظة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { c -> c.isDigit() } },
                        label = { Text("عدد الآيات المحفوظة اليوم") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("ملاحظة (مثال: الآيات ١ - ١٠)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val added = countText.toLongOrNull()?.coerceIn(1L, 500L) ?: 5L
                    onLogVerses(added, noteText.ifBlank { "تم حفظ $added آية جديدة" })
                    showLogDialog = false
                }) { Text("تسجيل") }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
