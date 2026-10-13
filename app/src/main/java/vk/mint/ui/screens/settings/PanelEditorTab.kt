package vk.mint.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import vk.mint.VkMintApp
import vk.mint.data.local.VkMintPrefs
import vk.mint.ui.navigation.PanelItem
import vk.mint.ui.navigation.PanelItems
import vk.mint.ui.navigation.Screen

// ══════════════════════════════════════════════════════════════════════
//  Fix #337: «Редактор панелей» — настройка боковой и нижней панелей.
//
//  Пользователь может:
//   - Включать/выключать кнопки в обеих панелях.
//   - Менять порядок кнопок стрелками ↑/↓.
//
//  Ограничения для боковой панели (по требованию пользователя):
//   - «Выйти из приложения» — всегда внизу, не редактируется.
//   - «Настройки» — всегда над «Выйти», не редактируется.
//   - «Офлайн» — всегда над «Настройки», не редактируется.
//  Эти 3 кнопки НЕ входят в sidebarItemsOrder/Hidden — они рендерятся
//  отдельно в VkMintNavHost (фиксированный «хвост» drawer'а).
//
//  Нижняя панель — до 5 основных вкладок (как в оригинальном мобильном VK).
// ══════════════════════════════════════════════════════════════════════

private const val MAX_BOTTOM_BAR_TABS = 5

/**
 * Все редактируемые пункты боковой и нижней панелей.
 * #PANELEDIT (2026-10-02): единый канонический набор — PanelItems.all
 * (см. ui/navigation/PanelItems.kt). Раньше были два разнесённых Screen-списка
 * (SIDEBAR_EDITABLE_SCREENS / BOTTOMBAR_EDITABLE_SCREENS); теперь обе панели
 * используют ОДИН список из 14 пунктов по требованию пользователя, а фикси-
 * рованный хвост drawer (Офлайн/Настройки/Выйти) по-прежнему не редактируется.
 */
private val CANONICAL_PANEL_ITEMS: List<PanelItem> = PanelItems.all

// ── JSON helpers ────────────────────────────────────────────────────────

private fun parseRoutes(json: String): List<String> {
    return try {
        val arr = JSONArray(if (json.isBlank()) "[]" else json)
        buildList {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun List<String>.toJson(): String {
    val arr = JSONArray()
    forEach { arr.put(it) }
    return arr.toString()
}

/**
 * Нормализует order под canonical-список: убирает неизвестные keys,
 * добавляет недостающие (новые пункты после обновления) в конец.
 * Гарантирует, что в order есть ВСЕ пункты из [canonical] ровно по разу.
 */
private fun normalizeOrder(order: List<String>, canonical: List<PanelItem>): List<String> {
    val canonicalKeys = canonical.map { it.key }
    val seen = mutableSetOf<String>()
    val result = mutableListOf<String>()
    // Сохраняем порядок пользователя, фильтруя неизвестные/дубли.
    for (k in order) {
        if (k in canonicalKeys && k !in seen) {
            result.add(k)
            seen.add(k)
        }
    }
    // Добавляем новые пункты (которых не было в сохранённом order).
    for (item in canonical) {
        if (item.key !in seen) {
            result.add(item.key)
            seen.add(item.key)
        }
    }
    return result
}

// ── Tab composable ──────────────────────────────────────────────────────

@Composable
fun PanelEditorTab(
    s: VkMintPrefs.Snapshot,
    app: VkMintApp,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    var bottomOrder by remember(s.bottomBarItemsOrder) {
        mutableStateOf(normalizeOrder(parseRoutes(s.bottomBarItemsOrder), CANONICAL_PANEL_ITEMS))
    }
    var bottomHidden by remember(s.bottomBarItemsHidden) {
        mutableStateOf(parseRoutes(s.bottomBarItemsHidden).toSet())
    }

    fun commitBottom() {
        scope.launch {
            app.prefs.setBottomBarItemsOrder(bottomOrder.toJson())
            app.prefs.setBottomBarItemsHidden(bottomHidden.toList().toJson())
        }
    }

    val visibleCount = CANONICAL_PANEL_ITEMS.count { it.key !in bottomHidden }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader("Нижняя панель навигации ($visibleCount из $MAX_BOTTOM_BAR_TABS)") }
        item {
            Text(
                "Выберите до $MAX_BOTTOM_BAR_TABS основных разделов для быстрого доступа на нижней панели (как в приложении VK). " +
                    "Все остальные разделы всегда доступны во вкладке «Сервисы».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    // Сброс к стандартным 5 вкладкам оригинального VK:
                    // Главная (Лента), Сервисы, Мессенджер (Чаты), Клипы, Профиль.
                    val defaultVisible = listOf(
                        "feed", "services", "messenger", "clips", "profile",
                    )
                    val defaultOrder = defaultVisible +
                        CANONICAL_PANEL_ITEMS.filter { it.key !in defaultVisible }.map { it.key }
                    val defaultHidden = CANONICAL_PANEL_ITEMS
                        .filter { it.key !in defaultVisible }.map { it.key }.toSet()
                    bottomOrder = defaultOrder
                    bottomHidden = defaultHidden
                    commitBottom()
                }) {
                    Text("Сбросить по умолчанию")
                }
            }
        }
        item {
            ReorderableListCard(
                title = "Кнопки панели",
                orderedKeys = bottomOrder,
                hiddenKeys = bottomHidden,
                itemByKey = PanelItems.byKey,
                onMoveUp = { idx ->
                    if (idx > 0) {
                        bottomOrder = bottomOrder.toMutableList().apply {
                            add(idx - 1, removeAt(idx))
                        }
                        commitBottom()
                    }
                },
                onMoveDown = { idx ->
                    if (idx < bottomOrder.lastIndex) {
                        bottomOrder = bottomOrder.toMutableList().apply {
                            add(idx + 1, removeAt(idx))
                        }
                        commitBottom()
                    }
                },
                onToggleVisible = { key ->
                    if (key in bottomHidden) {
                        val currentVisible = CANONICAL_PANEL_ITEMS.count { it.key !in bottomHidden }
                        if (currentVisible >= MAX_BOTTOM_BAR_TABS) {
                            Toast.makeText(
                                context,
                                "На нижней панели может быть не более $MAX_BOTTOM_BAR_TABS вкладок (остальные доступны в «Сервисах»)",
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            bottomHidden = bottomHidden - key
                            commitBottom()
                        }
                    } else {
                        bottomHidden = bottomHidden + key
                        commitBottom()
                    }
                },
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Subcomponents ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun ReorderableListCard(
    title: String,
    orderedKeys: List<String>,
    hiddenKeys: Set<String>,
    itemByKey: Map<String, PanelItem>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onToggleVisible: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            orderedKeys.forEachIndexed { idx, key ->
                val item = itemByKey[key] ?: return@forEachIndexed
                val visible = key !in hiddenKeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (visible) MaterialTheme.colorScheme.surfaceContainerLow
                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    if (item.icon != null) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (visible) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (visible) FontWeight.Medium else FontWeight.Normal,
                        color = if (visible) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    // Visibility toggle
                    IconButton(
                        onClick = { onToggleVisible(key) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (visible) "Скрыть" else "Показать",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Move up
                    IconButton(
                        onClick = { onMoveUp(idx) },
                        enabled = idx > 0,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Вверх",
                            tint = if (idx > 0) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                    }
                    // Move down
                    IconButton(
                        onClick = { onMoveDown(idx) },
                        enabled = idx < orderedKeys.lastIndex,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Вниз",
                            tint = if (idx < orderedKeys.lastIndex) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}
