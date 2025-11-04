package com.github.tvbox.osc.ui.activity

import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.AppUtils
import android.os.Environment
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import android.content.SharedPreferences
import androidx.lifecycle.lifecycleScope
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.util.*
import com.github.tvbox.osc.api.ApiConfig
import kotlinx.coroutines.launch
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl

/**
 * 设置组件
 */
class SettingComposeActivity : BaseActivity() {

    override fun getLayoutResID(): Int = -1

    @OptIn(ExperimentalMaterial3Api::class)
    override fun initVb() {
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val showClear = remember { mutableStateOf(false) }
                    val showLive = remember { mutableStateOf(false) }
                    val liveText = remember { mutableStateOf(Hawk.get(HawkConfig.LIVE_URL, "")) }
                    val showLiveHistory = remember { mutableStateOf(false) }
                    val showBackup = remember { mutableStateOf(false) }
                    val backups = remember { mutableStateOf(listBackups()) }
                    // Generic selection sheet config
                    data class SelectConfig(
                        val title: String,
                        val items: List<String>,
                        val defaultIndex: Int = 0,
                        val onSelect: (Int) -> Unit
                    )
                    val selectConfig = remember { mutableStateOf<SelectConfig?>(null) }

                    val handlers = SettingHandlers(
                        onTogglePrivateBrowsing = { newValue -> Hawk.put(HawkConfig.PRIVATE_BROWSING, newValue) },
                        onToggleVideoPurify = { newValue -> Hawk.put(HawkConfig.VIDEO_PURIFY, newValue) },
                        onToggleIjkCache = { newValue -> Hawk.put(HawkConfig.IJK_CACHE_PLAY, newValue) },
                        onClickLiveApi = {
                            showLive.value = true
                        },
                        onClickBackgroundPlayType = {
                            val types = arrayListOf("关闭", "开启", "画中画")
                            val defaultPos = Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0)
                            selectConfig.value = SelectConfig("请选择", types, defaultPos) { pos ->
                                Hawk.put(HawkConfig.BACKGROUND_PLAY_TYPE, pos)
                            }
                        },
                        onClickDns = {
                            val dohUrl = Hawk.get(HawkConfig.DOH_URL, 0)
                            val list = OkGoHelper.dnsHttpsList
                            selectConfig.value = SelectConfig("请选择安全DNS", list, dohUrl) { pos ->
                                Hawk.put(HawkConfig.DOH_URL, pos)
                                val url = OkGoHelper.getDohUrl(pos)
                                OkGoHelper.dnsOverHttps.setUrl(if (url.isEmpty()) null else HttpUrl.get(url))
                            }
                        },
                        onClickPlayType = {
                            val playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
                            val players = PlayerHelper.getExistPlayerTypes()
                            var defaultPos = players.indexOf(playerType).let { if (it < 0) 0 else it }
                            val names = players.map { PlayerHelper.getPlayerName(it) }
                            selectConfig.value = SelectConfig("请选择默认播放器", names, defaultPos) { pos ->
                                val thisPlayerType = players[pos]
                                Hawk.put(HawkConfig.PLAY_TYPE, thisPlayerType)
                                PlayerHelper.init()
                            }
                        },
                        onClickRender = {
                            val defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0)
                            val options = listOf(0, 1)
                            val names = options.map { PlayerHelper.getRenderName(it) }
                            selectConfig.value = SelectConfig("请选择默认渲染方式", names, defaultPos) { pos ->
                                Hawk.put(HawkConfig.PLAY_RENDER, pos)
                                PlayerHelper.init()
                            }
                        },
                        onClickScale = {
                            val defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0)
                            val options = listOf(0, 1, 2, 3, 4, 5)
                            val names = options.map { PlayerHelper.getScaleName(it) }
                            selectConfig.value = SelectConfig("请选择画面缩放", names, defaultPos) { pos ->
                                Hawk.put(HawkConfig.PLAY_SCALE, pos)
                            }
                        },
                        onClickHistoryNum = {
                            val defaultPos = Hawk.get(HawkConfig.HISTORY_NUM, 0)
                            val options = listOf(0, 1, 2)
                            val names = options.map { HistoryHelper.getHistoryNumName(it) }
                            selectConfig.value = SelectConfig("保留历史记录数量", names, defaultPos) { pos ->
                                Hawk.put(HawkConfig.HISTORY_NUM, pos)
                            }
                        },
                        onClickMediaCodec = {
                            val ijkCodes = ApiConfig.get().ijkCodes ?: return@SettingHandlers
                            if (ijkCodes.isEmpty()) return@SettingHandlers
                            var defaultPos = 0
                            val ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "")
                            for (j in ijkCodes.indices) {
                                if (ijkSel == ijkCodes[j].name) { defaultPos = j; break }
                            }
                            val names = ijkCodes.map { it.name }
                            selectConfig.value = SelectConfig("请选择IJK解码", names, defaultPos) { pos ->
                                val value = ijkCodes[pos]
                                value.selected(true)
                                Hawk.put(HawkConfig.IJK_CODEC, value.name)
                            }
                        },
                        onClickPressSpeed = {
                            val types = arrayListOf("2.0", "3.0", "4.0", "5.0", "6.0", "8.0", "10.0")
                            val defaultPos = types.indexOf(Hawk.get(HawkConfig.VIDEO_SPEED, 2.0f).toString()).let { if (it < 0) 0 else it }
                            selectConfig.value = SelectConfig("请选择", types, defaultPos) { pos ->
                                Hawk.put(HawkConfig.VIDEO_SPEED, types[pos].toFloat())
                            }
                        },
                        onClickBackup = {
                            if (XXPermissions.isGranted(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
                                backups.value = listBackups()
                                showBackup.value = true
                            } else {
                                XXPermissions.with(this)
                                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                                    .request(object : OnPermissionCallback {
                                        override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                                            if (all) {
                                                backups.value = listBackups()
                                                showBackup.value = true
                                            }
                                        }
                                        override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                                            if (never) {
                                                ToastUtils.showLong("获取存储权限失败,请在系统设置中开启")
                                                XXPermissions.startPermissionActivity(this@SettingComposeActivity, permissions)
                                            } else {
                                                ToastUtils.showShort("获取存储权限失败")
                                            }
                                        }
                                    })
                            }
                        },
                        onClickClearCache = { showClear.value = true },
                        onClickTheme = {
                            val oldTheme = Hawk.get(HawkConfig.THEME_TAG, 0)
                            val themes = arrayOf("跟随系统", "浅色", "深色")
                            val types = listOf(0, 1, 2)
                            selectConfig.value = SelectConfig("请选择", themes.toList(), oldTheme) { pos ->
                                if (oldTheme != pos) {
                                    Hawk.put(HawkConfig.THEME_TAG, pos)
                                    Utils.initTheme()
                                    jumpActivity(MainActivity::class.java)
                                }
                            }
                        }
                    )

                    // Dialogs
                    if (showClear.value) {
                        AlertDialog(
                            onDismissRequest = { showClear.value = false },
                            title = { Text("提示") },
                            text = { Text("确定清空吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showClear.value = false
                                    onClickClearCache()
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClear.value = false }) { Text("取消") }
                            }
                        )
                    }

                    if (showLive.value) {
                        AlertDialog(
                            onDismissRequest = { showLive.value = false },
                            title = { Text("直播源") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = liveText.value,
                                        onValueChange = { liveText.value = it },
                                        singleLine = true,
                                        label = { Text("地址") }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        val history = Hawk.get(HawkConfig.LIVE_HISTORY, ArrayList<String>())
                                        if (history.isEmpty()) {
                                            ToastUtils.showShort("暂无历史记录")
                                        } else {
                                            showLiveHistory.value = true
                                        }
                                    }) { Text("历史记录") }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val newLive = liveText.value.trim()
                                    Hawk.put(HawkConfig.LIVE_URL, newLive)
                                    if (newLive.isNotEmpty()) {
                                        val list = Hawk.get(HawkConfig.LIVE_HISTORY, ArrayList<String>())
                                        if (!list.contains(newLive)) list.add(0, newLive)
                                        if (list.size > 20) list.removeAt(20)
                                        Hawk.put(HawkConfig.LIVE_HISTORY, list)
                                    }
                                    ToastUtils.showShort("设置成功")
                                    showLive.value = false
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLive.value = false }) { Text("取消") }
                            }
                        )
                    }

                    if (showLiveHistory.value) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showLiveHistory.value = false },
                            sheetState = sheetState
                        ) {
                            val history = Hawk.get(HawkConfig.LIVE_HISTORY, ArrayList<String>())
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text(text = "选择历史记录", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn {
                                    itemsIndexed(history) { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp)
                                                .clickable {
                                                    liveText.value = item
                                                    showLiveHistory.value = false
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = item, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (index < history.lastIndex) {
                                            HorizontalDivider(color = DividerDefaults.color)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showBackup.value) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showBackup.value = false },
                            sheetState = sheetState
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text(text = "数据备份还原", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val ok = backupNow()
                                        withContext(Dispatchers.Main) {
                                            backups.value = listBackups()
                                        }
                                    }
                                }) { Text("立即备份") }
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn {
                                    itemsIndexed(backups.value) { index, name ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp)
                                                .clickable { restoreBackup(name) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            TextButton(onClick = {
                                                deleteBackup(name)
                                                backups.value = listBackups()
                                            }) { Text("删除") }
                                        }
                                        if (index < backups.value.lastIndex) {
                                            HorizontalDivider(color = DividerDefaults.color)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val currentSelect = selectConfig.value
                    if (currentSelect != null) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { selectConfig.value = null },
                            sheetState = sheetState
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text(text = currentSelect.title, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn {
                                    itemsIndexed(currentSelect.items) { index, label ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 14.dp)
                                                .clickable {
                                                    currentSelect.onSelect(index)
                                                    selectConfig.value = null
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        if (index < currentSelect.items.lastIndex) {
                                            HorizontalDivider(color = DividerDefaults.color)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingScreen(
                        onBack = { finish() },
                        state = rememberSettingState(),
                        handlers = handlers
                    )
                }
            }
        }
    }

    override fun init() {
        // Compose handles UI
    }

    @Suppress("DEPRECATION")
    private fun onClickClearCache() {
        val cachePath = FileUtils.getCachePath()
        val cacheDir = java.io.File(cachePath)
        if (!cacheDir.exists()) return
        Thread {
            try {
                FileUtils.cleanDirectory(cacheDir)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        ToastUtils.showLong("缓存已清空")
    }
}

// ---------- State & Handlers ----------

@Composable
private fun rememberSettingState(): SettingState {
    // read once on composition; simple approach, can be enhanced to observe changes
    val homeRec = remember { mutableStateOf(getHomeRecName(Hawk.get(HawkConfig.HOME_REC, 0))) }
    val theme = remember { mutableStateOf(themeName(Hawk.get(HawkConfig.THEME_TAG, 0))) }
    val dns = remember { mutableStateOf(OkGoHelper.dnsHttpsList[Hawk.get(HawkConfig.DOH_URL, 0)]) }
    val player = remember { mutableStateOf(PlayerHelper.getPlayerName(Hawk.get(HawkConfig.PLAY_TYPE, 0))) }
    val render = remember { mutableStateOf(PlayerHelper.getRenderName(Hawk.get(HawkConfig.PLAY_RENDER, 0))) }
    val scale = remember { mutableStateOf(PlayerHelper.getScaleName(Hawk.get(HawkConfig.PLAY_SCALE, 0))) }
    val mediaCodec = remember { mutableStateOf(Hawk.get(HawkConfig.IJK_CODEC, "")) }
    val speed = remember { mutableStateOf(Hawk.get(HawkConfig.VIDEO_SPEED, 2.0f).toString()) }
    val historyNum = remember { mutableStateOf(HistoryHelper.getHistoryNumName(Hawk.get(HawkConfig.HISTORY_NUM, 0))) }
    val backgroundPlay = remember { mutableStateOf(backgroundPlayName(Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0))) }
    val privateBrowsing = remember { mutableStateOf(Hawk.get(HawkConfig.PRIVATE_BROWSING, false)) }
    val videoPurify = remember { mutableStateOf(Hawk.get(HawkConfig.VIDEO_PURIFY, true)) }
    val ijkCachePlay = remember { mutableStateOf(Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)) }

    return SettingState(
        homeRec, theme, dns, player, render, scale, mediaCodec, speed, historyNum, backgroundPlay,
        privateBrowsing, videoPurify, ijkCachePlay
    )
}

private fun getHomeRecName(type: Int): String = when (type) {
    0 -> "豆瓣热播"
    1 -> "站点推荐"
    else -> "关闭"
}

private fun themeName(oldTheme: Int): String = arrayOf("跟随系统", "浅色", "深色")[oldTheme]
private fun backgroundPlayName(type: Int): String = arrayOf("关闭", "开启", "画中画")[type]

data class SettingState(
    val homeRec: State<String>,
    val theme: State<String>,
    val dns: State<String>,
    val player: State<String>,
    val render: State<String>,
    val scale: State<String>,
    val mediaCodec: State<String>,
    val speed: State<String>,
    val historyNum: State<String>,
    val backgroundPlay: State<String>,
    val privateBrowsing: State<Boolean>,
    val videoPurify: State<Boolean>,
    val ijkCachePlay: State<Boolean>,
)

data class SettingHandlers(
    val onTogglePrivateBrowsing: (Boolean) -> Unit,
    val onToggleVideoPurify: (Boolean) -> Unit,
    val onToggleIjkCache: (Boolean) -> Unit,
    val onClickLiveApi: () -> Unit,
    val onClickBackgroundPlayType: (String) -> Unit,
    val onClickDns: () -> Unit,
    val onClickPlayType: () -> Unit,
    val onClickRender: () -> Unit,
    val onClickScale: () -> Unit,
    val onClickHistoryNum: () -> Unit,
    val onClickMediaCodec: () -> Unit,
    val onClickPressSpeed: () -> Unit,
    val onClickBackup: () -> Unit,
    val onClickClearCache: () -> Unit,
    val onClickTheme: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingScreen(
    onBack: () -> Unit,
    state: SettingState,
    handlers: SettingHandlers
) {
    val container = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = container,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
    )

    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            window.statusBarColor = container.toArgb()
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = container.luminance() > 0.5f
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = colors,
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                }
            )
        }
    ) { innerPadding ->
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            SettingsCard {
                RowItem(label = "主页内容", value = state.homeRec.value) { /* future: selection */ }
                DividerLine()
                RowItem(label = "主题颜色", value = state.theme.value, onClick = handlers.onClickTheme)
                DividerLine()
                RowItem(label = "直播源", value = "", onClick = handlers.onClickLiveApi)
                DividerLine()
                SwitchItem(label = "无痕浏览", checked = state.privateBrowsing.value) { checked ->
                    handlers.onTogglePrivateBrowsing(checked)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            SettingsCard {
                RowItem(label = "播放器", value = state.player.value, onClick = handlers.onClickPlayType)
                DividerLine()
                SwitchItem(label = "广告过滤", checked = state.videoPurify.value) { checked ->
                    handlers.onToggleVideoPurify(checked)
                }
                DividerLine()
                RowItem(label = "长按倍速", value = state.speed.value, onClick = handlers.onClickPressSpeed)
                DividerLine()
                RowItem(label = "后台播放", value = state.backgroundPlay.value, onClick = { handlers.onClickBackgroundPlayType(state.backgroundPlay.value) })
                DividerLine()
                RowItem(label = "IJK解码方式", value = state.mediaCodec.value, onClick = handlers.onClickMediaCodec)
                DividerLine()
                RowItem(label = "渲染方式", value = state.render.value, onClick = handlers.onClickRender)
                DividerLine()
                RowItem(label = "画面缩放", value = state.scale.value, onClick = handlers.onClickScale)
                DividerLine()
                SwitchItem(label = "IJK缓存", checked = state.ijkCachePlay.value) { checked ->
                    handlers.onToggleIjkCache(checked)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            SettingsCard {
                RowItem(label = "数据备份还原", value = "", onClick = handlers.onClickBackup)
                DividerLine()
                RowItem(label = "安全DNS", value = state.dns.value, onClick = handlers.onClickDns)
                DividerLine()
                RowItem(label = "历史记录", value = state.historyNum.value, onClick = handlers.onClickHistoryNum)
                DividerLine()
                RowItem(label = "清空缓存", value = "", onClick = handlers.onClickClearCache)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
private fun DividerLine() {
    HorizontalDivider(color = DividerDefaults.color)
}

@Composable
private fun RowItem(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.icon_pre),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(26.dp)
                .alpha(0.9f)
        )
    }
}

@Composable
private fun SwitchItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var internalChecked by remember { mutableStateOf(checked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = internalChecked,
            onCheckedChange = {
                internalChecked = it
                onCheckedChange(it)
            }
        )
    }
}

private fun listBackups(): List<String> {
    val result = ArrayList<String>()
    try {
        val root = android.os.Environment.getExternalStorageDirectory().absolutePath
        val file = java.io.File("$root/tvbox_backup/")
        if (file.exists()) {
            val list = file.listFiles()?.sortedWith(compareBy<java.io.File> { it.isFile }.thenByDescending { it.name }) ?: emptyList()
            for (f in list) {
                if (result.size > 10) {
                    com.github.tvbox.osc.util.FileUtils.recursiveDelete(f)
                    continue
                }
                if (f.isDirectory) result.add(f.name)
            }
        }
    } catch (e: Throwable) { e.printStackTrace() }
    return result
}

private fun restoreBackup(dir: String) {
    try {
        val root = android.os.Environment.getExternalStorageDirectory().absolutePath
        val backup = java.io.File("$root/tvbox_backup/$dir")
        if (backup.exists()) {
            val db = java.io.File(backup, "sqlite")
            if (com.github.tvbox.osc.data.AppDataManager.restore(db)) {
                val data = com.github.tvbox.osc.util.FileUtils.readSimple(java.io.File(backup, "hawk"))
                if (data != null) {
                    val hawkJson = String(data, Charsets.UTF_8)
                    val jsonObject = org.json.JSONObject(hawkJson)
                    val it = jsonObject.keys()
                    var shared: android.content.SharedPreferences = com.github.tvbox.osc.base.App.getInstance().getSharedPreferences("Hawk2", android.content.Context.MODE_PRIVATE)
                    while (it.hasNext()) {
                        val key = it.next()
                        val value = jsonObject.getString(key)
                        if (key == "cipher_key") {
                            com.github.tvbox.osc.base.App.getInstance().getSharedPreferences("crypto.KEY_256", android.content.Context.MODE_PRIVATE).edit().putString(key, value).commit()
                        } else {
                            shared.edit().putString(key, value).commit()
                        }
                    }
                    com.blankj.utilcode.util.ToastUtils.showShort("恢复成功,即将重启应用!")
                    android.os.Handler().postDelayed({ com.blankj.utilcode.util.AppUtils.relaunchApp(true) }, 2000)
                } else {
                    com.blankj.utilcode.util.ToastUtils.showShort("Hawk恢复失败!")
                }
            } else {
                com.blankj.utilcode.util.ToastUtils.showShort("DB文件恢复失败!")
            }
        }
    } catch (e: Throwable) { e.printStackTrace() }
}

private fun backupNow(): Boolean {
    return try {
        val root = android.os.Environment.getExternalStorageDirectory().absolutePath
        val file = java.io.File("$root/tvbox_backup/")
        if (!file.exists()) file.mkdirs()
        val now = java.util.Date()
        val f = java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss")
        val backup = java.io.File(file, f.format(now))
        backup.mkdirs()
        val db = java.io.File(backup, "sqlite")
        if (com.github.tvbox.osc.data.AppDataManager.backup(db)) {
            var sp: android.content.SharedPreferences = com.github.tvbox.osc.base.App.getInstance().getSharedPreferences("Hawk2", android.content.Context.MODE_PRIVATE)
            val json = org.json.JSONObject()
            for (key in sp.all.keys) json.put(key, sp.getString(key, ""))
            sp = com.github.tvbox.osc.base.App.getInstance().getSharedPreferences("crypto.KEY_256", android.content.Context.MODE_PRIVATE)
            for (key in sp.all.keys) json.put(key, sp.getString(key, ""))
            if (!com.github.tvbox.osc.util.FileUtils.writeSimple(json.toString().toByteArray(Charsets.UTF_8), java.io.File(backup, "hawk"))) {
                backup.delete()
                com.blankj.utilcode.util.ToastUtils.showShort("备份Hawk失败!")
            } else {
                com.blankj.utilcode.util.ToastUtils.showShort("备份成功!")
            }
        } else {
            com.blankj.utilcode.util.ToastUtils.showShort("DB文件不存在!")
            backup.delete()
        }
        true
    } catch (e: Throwable) {
        e.printStackTrace()
        com.blankj.utilcode.util.ToastUtils.showShort("备份失败!")
        false
    }
}

private fun deleteBackup(dir: String) {
    try {
        val root = android.os.Environment.getExternalStorageDirectory().absolutePath
        val backup = java.io.File("$root/tvbox_backup/$dir")
        com.github.tvbox.osc.util.FileUtils.recursiveDelete(backup)
        com.blankj.utilcode.util.ToastUtils.showShort("删除成功")
    } catch (e: Throwable) { e.printStackTrace() }
}
