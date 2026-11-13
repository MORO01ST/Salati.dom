package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementPost
import com.example.data.model.UserProfile
import com.example.ui.theme.MedinaGold
import com.example.ui.theme.SuccessJade
import com.example.ui.viewmodel.SpiritualAnalytics

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressSocialScreen(
    isGuestMode: Boolean,
    userProfile: UserProfile?,
    analytics: SpiritualAnalytics,
    achievements: List<AchievementPost>,
    onShareAchievement: (String, String, String) -> Unit,
    onSendAmeen: (String) -> Unit,
    onRequestGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Header & Sign Out / Guest Upgrade
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.displayName ?: if (isGuestMode) "زائر صلاتي" else "مسلم",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = if (isGuestMode) {
                                    "وضع الزائر • البيانات محفوظة محلياً على جهازك"
                                } else {
                                    "رمز الصداقة: ${userProfile?.friendCode ?: "..."}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "إحصائياتي الروحية في تطبيق صلاتي:\n• نسبة الالتزام بالصلاة: ${analytics.weeklyConsistencyPercent}%\n• سلسلة الأيام: ${analytics.currentStreakDays} يوم\n• صفحات القرآن: ${analytics.totalWirdPages} صفحة"
                                    )
                                }
                                context.startActivity(Intent.createChooser(share, "مشاركة تقدمي الروحي"))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "مشاركة بطاقة الإحصائيات")
                            }
                            if (!isGuestMode) {
                                IconButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "تسجيل الخروج")
                                }
                            }
                        }
                    }

                    if (isGuestMode) {
                        Button(
                            onClick = onRequestGoogleSignIn,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("progress_guest_signin_button")
                        ) {
                            Text("ربط بحساب Google للمزامنة والمنافسة")
                        }
                    }
                }
            }
        }

        // Spiritual Analytics KPI Grid
        item {
            Text("إحصائيات التقدم الروحي", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "الالتزام الأسبوعي",
                    value = "${analytics.weeklyConsistencyPercent}%",
                    icon = Icons.Filled.AutoGraph,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "سلسلة الالتزام",
                    value = "${analytics.currentStreakDays} يوم",
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "إجمالي الصلوات",
                    value = "${analytics.totalPrayers}",
                    icon = Icons.Filled.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "صفحات الورد",
                    value = "${analytics.totalWirdPages}",
                    icon = Icons.Filled.WorkspacePremium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Performance Canvas Chart
        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            val goldColor = MedinaGold
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تحليل الأداء الأسبوعي (الصلوات والورد)", style = MaterialTheme.typography.titleMedium)
                        Text("أفضل سلسلة: ${analytics.bestStreakDays} ي", style = MaterialTheme.typography.labelMedium, color = MedinaGold)
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("weekly_analytics_chart")
                    ) {
                        val bars = analytics.weeklyBars
                        if (bars.isNotEmpty()) {
                            val groupWidth = size.width / bars.size
                            val barWidth = (groupWidth * 0.28f).coerceAtLeast(10f)
                            bars.forEachIndexed { idx, point ->
                                val centerX = idx * groupWidth + groupWidth / 2f
                                val prayerRatio = (point.prayersCompleted / 5f).coerceIn(0f, 1f)
                                val wirdRatio = (point.wirdPages / 6f).coerceIn(0f, 1f)

                                // Prayer track + fill
                                drawRoundRect(
                                    color = trackColor,
                                    topLeft = Offset(centerX - barWidth - 3f, 8f),
                                    size = Size(barWidth, size.height - 16f),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                                val pHeight = (size.height - 16f) * prayerRatio
                                if (pHeight > 0f) {
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(centerX - barWidth - 3f, size.height - 8f - pHeight),
                                        size = Size(barWidth, pHeight),
                                        cornerRadius = CornerRadius(10f, 10f)
                                    )
                                }

                                // Wird track + fill
                                drawRoundRect(
                                    color = trackColor,
                                    topLeft = Offset(centerX + 3f, 8f),
                                    size = Size(barWidth, size.height - 16f),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                                val wHeight = (size.height - 16f) * wirdRatio
                                if (wHeight > 0f) {
                                    drawRoundRect(
                                        color = goldColor,
                                        topLeft = Offset(centerX + 3f, size.height - 8f - wHeight),
                                        size = Size(barWidth, wHeight),
                                        cornerRadius = CornerRadius(10f, 10f)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        analytics.weeklyBars.forEach { bar ->
                            Text(bar.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("■ الصلوات (من ٥)", style = MaterialTheme.typography.labelSmall, color = primaryColor)
                        Text("■ صفحات الورد", style = MaterialTheme.typography.labelSmall, color = goldColor)
                    }
                }
            }
        }

        // Spiritual Badges
        if (analytics.earnedBadges.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الأوسمة المكتسبة", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        analytics.earnedBadges.forEach { badge ->
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MedinaGold, modifier = Modifier.size(18.dp))
                                },
                                label = { Text(badge) }
                            )
                        }
                    }
                }
            }
        }

        // Social Achievements Header
        if (isGuestMode) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("ساحة الإنجازات والأصدقاء", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "تظهر لك جميع إحصائياتك الأسبوعية وأوسمتك أعلاه دون الحاجة لتسجيل الدخول. سجل بحساب Google لمشاركة إنجازاتك في الساحة العامة مع أصدقائك.",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        Text("ساحة الإنجازات والأصدقاء", style = MaterialTheme.typography.titleLarge)
                        Text("شارك إنجازك وشجّع إخوانك بالدعاء", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showShareDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("share_achievement_button")
                    ) {
                        Text("نشر إنجاز")
                    }
                }
            }

            if (achievements.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MedinaGold, modifier = Modifier.size(36.dp))
                            Text("كن أول من يشارك إنجازه اليوم!", style = MaterialTheme.typography.titleMedium)
                            Text("شارك إتمام وردك أو محافظتك على صلواتك لتحفيز أصدقائك.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
            items(achievements, key = { it.id }) { post ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(post.authorName, style = MaterialTheme.typography.titleMedium)
                                Text(post.badgeTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            if (post.streakDays > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "سلسلة ${post.streakDays} ي",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(post.description, style = MaterialTheme.typography.bodyLarge)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSendAmeen(post.id) },
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = SuccessJade, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("بارك الله فيك (${post.ameenCount})")
                            }
                            IconButton(onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${post.badgeTitle}: ${post.description} — تطبيق صلاتي")
                                }
                                context.startActivity(Intent.createChooser(share, "مشاركة الإنجاز"))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "مشاركة")
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showShareDialog) {
        var title by remember { mutableStateOf("أتممت صلواتي ووردي اليوم") }
        var desc by remember { mutableStateOf("الحمد لله الذي بنعمته تتم الصالحات، حافظت اليوم على الصلوات الخمس والورد القرآني.") }
        var category by remember { mutableStateOf("PRAYER") }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("مشاركة إنجاز روحي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PRAYER" to "صلاة", "QURAN" to "قرآن", "DHIKR" to "ذكر", "STREAK" to "سلسلة").forEach { (cat, label) ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الإنجاز") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("رسالة ملهمة لأصدقائك") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && desc.isNotBlank()) {
                        onShareAchievement(title.trim(), desc.trim(), category)
                        showShareDialog = false
                    }
                }) { Text("نشر الآن") }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = MedinaGold, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
