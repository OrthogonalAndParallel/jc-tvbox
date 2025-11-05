package com.github.tvbox.osc.ui.activity

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.bean.ParseBean

interface OnIndexClick { fun onClick(index: Int) }

fun setDetailListsContent(
    view: ComposeView,
    flags: List<VodInfo.VodSeriesFlag>?,
    currentFlagName: String?,
    episodes: List<VodInfo.VodSeries>?,
    onFlagClick: OnIndexClick,
    onEpisodeClick: OnIndexClick
) {
    view.setContent {
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                DetailLists(
                    flags = flags ?: emptyList(),
                    currentFlagName = currentFlagName,
                    episodes = episodes ?: emptyList(),
                    onFlagClick = onFlagClick,
                    onEpisodeClick = onEpisodeClick
                )
            }
        }
    }
}

@Composable
fun ActionRow(
    isCollected: Boolean,
    onCast: () -> Unit,
    onCollect: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "投屏",
            modifier = Modifier.clickable { onCast() }.padding(10.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (isCollected) "取消收藏" else "收藏",
            modifier = Modifier.clickable { onCollect() }.padding(10.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "下载",
            modifier = Modifier.clickable { onDownload() }.padding(10.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ParseRow(
    items: List<ParseBean>,
    defaultIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "解析",
            modifier = Modifier.padding(start = 14.dp, top = 8.dp),
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            itemsIndexed(items) { idx, bean ->
                val selected = idx == defaultIndex || bean.isDefault
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .padding(vertical = 2.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelect(idx) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bean.name ?: "",
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun setActionRowContent(view: ComposeView, isCollected: Boolean, onCast: Runnable, onCollect: Runnable, onDownload: Runnable) {
    view.setContent {
        MaterialTheme {
            ActionRow(
                isCollected = isCollected,
                onCast = { onCast.run() },
                onCollect = { onCollect.run() },
                onDownload = { onDownload.run() }
            )
        }
    }
}

fun setParseRowContent(view: ComposeView, items: List<ParseBean>, defaultIndex: Int, onSelect: (Int) -> Unit) {
    view.setContent {
        MaterialTheme {
            ParseRow(items = items, defaultIndex = defaultIndex, onSelect = onSelect)
        }
    }
}

// Java-friendly overload
fun setParseRowContent(view: ComposeView, items: List<ParseBean>, defaultIndex: Int, onSelect: OnIndexClick) {
    view.setContent {
        MaterialTheme {
            ParseRow(items = items, defaultIndex = defaultIndex, onSelect = { idx -> onSelect.onClick(idx) })
        }
    }
}

@Composable
fun DetailScreen(
    name: String?,
    siteName: String?,
    isCollected: Boolean,
    flags: List<VodInfo.VodSeriesFlag>,
    currentFlagName: String?,
    episodes: List<VodInfo.VodSeries>,
    parseItems: List<ParseBean>,
    defaultParseIndex: Int,
    onCast: () -> Unit,
    onCollect: () -> Unit,
    onDownload: () -> Unit,
    onFlagClick: OnIndexClick,
    onEpisodeClick: OnIndexClick,
    onParseSelect: OnIndexClick
) {
    Column(modifier = Modifier.fillMaxSize().padding(bottom = 0.dp)) {
        // Player container (FrameLayout with fixed height)
        AndroidView(
            factory = { ctx ->
                android.widget.FrameLayout(ctx).apply {
                    id = com.github.tvbox.osc.R.id.previewPlayer
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        (250.dp.value * ctx.resources.displayMetrics.density).toInt()
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(250.dp)
        )
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            // Title
            if (!name.isNullOrEmpty()) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            // Site row
            if (!siteName.isNullOrEmpty()) {
                Text(
                    text = "来源：$siteName",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // Actions
            ActionRow(
                isCollected = isCollected,
                onCast = onCast,
                onCollect = onCollect,
                onDownload = onDownload
            )

            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

            // Parse row
            if (parseItems.isNotEmpty()) {
                ParseRow(items = parseItems, defaultIndex = defaultParseIndex, onSelect = { idx -> onParseSelect.onClick(idx) })
            }

            // Line header spacer
            Spacer(Modifier.height(8.dp))

            // Lists (flags + episodes)
            DetailLists(
                flags = flags,
                currentFlagName = currentFlagName,
                episodes = episodes,
                onFlagClick = onFlagClick,
                onEpisodeClick = onEpisodeClick
            )
        }
    }
}

fun setFullDetailContent(
    view: ComposeView,
    name: String?,
    siteName: String?,
    isCollected: Boolean,
    flags: List<VodInfo.VodSeriesFlag>,
    currentFlagName: String?,
    episodes: List<VodInfo.VodSeries>,
    parseItems: List<ParseBean>,
    defaultParseIndex: Int,
    onCast: Runnable,
    onCollect: Runnable,
    onDownload: Runnable,
    onFlagClick: OnIndexClick,
    onEpisodeClick: OnIndexClick,
    onParseSelect: OnIndexClick
) {
    view.setContent {
        MaterialTheme {
            DetailScreen(
                name = name,
                siteName = siteName,
                isCollected = isCollected,
                flags = flags,
                currentFlagName = currentFlagName,
                episodes = episodes,
                parseItems = parseItems,
                defaultParseIndex = defaultParseIndex,
                onCast = { onCast.run() },
                onCollect = { onCollect.run() },
                onDownload = { onDownload.run() },
                onFlagClick = onFlagClick,
                onEpisodeClick = onEpisodeClick,
                onParseSelect = onParseSelect
            )
        }
    }
}

fun createDetailListsComposeView(
    context: Context,
    flags: List<VodInfo.VodSeriesFlag>?,
    currentFlagName: String?,
    episodes: List<VodInfo.VodSeries>?,
    onFlagClick: OnIndexClick,
    onEpisodeClick: OnIndexClick
): ComposeView {
    return ComposeView(context).apply {
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DetailLists(flags = flags ?: emptyList(), currentFlagName = currentFlagName, episodes = episodes ?: emptyList(), onFlagClick = onFlagClick, onEpisodeClick = onEpisodeClick)
                }
            }
        }
    }
}

@Composable
private fun DetailLists(
    flags: List<VodInfo.VodSeriesFlag>,
    currentFlagName: String?,
    episodes: List<VodInfo.VodSeries>,
    onFlagClick: OnIndexClick,
    onEpisodeClick: OnIndexClick
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        // Flags
        if (flags.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                itemsIndexed(flags) { index, flag ->
                    val selected = flag.name == currentFlagName || flag.selected
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                            )
                            .clickable { onFlagClick.onClick(index) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = flag.name ?: "",
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        // Episodes (horizontal list to avoid vertical scroll inside parent ScrollView)
        if (episodes.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
            ) {
                itemsIndexed(episodes) { index, ep ->
                    val selected = ep.selected
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 96.dp)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                            )
                            .clickable { onEpisodeClick.onClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ep.name ?: "",
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
