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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter
import com.github.tvbox.osc.ui.dialog.BackupDialog
import com.github.tvbox.osc.ui.dialog.LiveApiDialog
import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.util.*
import com.github.tvbox.osc.api.ApiConfig
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup
import com.orhanobut.hawk.Hawk
import okhttp3.HttpUrl

/**
 * 设置组件
 */
class SettingComposeActivity : BaseActivity() {

    override fun getLayoutResID(): Int = -1

    override fun initVb() {
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SettingScreen(
                        onBack = { finish() },
                        state = rememberSettingState(),
                        handlers = provideHandlers()
                    )
                }
            }
        }
    }

    override fun init() {
        // Compose handles UI
    }

    private fun provideHandlers(): SettingHandlers = SettingHandlers(
        onTogglePrivateBrowsing = { newValue ->
            Hawk.put(HawkConfig.PRIVATE_BROWSING, newValue)
        },
        onToggleVideoPurify = { newValue ->
            Hawk.put(HawkConfig.VIDEO_PURIFY, newValue)
        },
        onToggleIjkCache = { newValue ->
            Hawk.put(HawkConfig.IJK_CACHE_PLAY, newValue)
        },
        onClickLiveApi = {
            XPopup.Builder(this)
                .autoFocusEditText(false)
                .asCustom(LiveApiDialog(this))
                .show()
        },
        onClickBackgroundPlayType = { current ->
            val types = arrayListOf("关闭", "开启", "画中画")
            val defaultPos = Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0)
            val dialog = SelectDialog<Int>(this)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.BACKGROUND_PLAY_TYPE, pos)
                }
                override fun getDisplay(value: Int?): String = types[value ?: 0]
            }, object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
                override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            }, (0..2).toList(), defaultPos)
            dialog.show()
        },
        onClickDns = {
            val dohUrl = Hawk.get(HawkConfig.DOH_URL, 0)
            val dialog = SelectDialog<String>(this)
            dialog.setTip("请选择安全DNS")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<String?> {
                override fun click(value: String?, pos: Int) {
                    Hawk.put(HawkConfig.DOH_URL, pos)
                    val url = OkGoHelper.getDohUrl(pos)
                    OkGoHelper.dnsOverHttps.setUrl(if (url.isEmpty()) null else HttpUrl.get(url))
                }
                override fun getDisplay(name: String?): String = name ?: ""
            }, SelectDialogAdapter.stringDiff, OkGoHelper.dnsHttpsList, dohUrl)
            dialog.show()
        },
        onClickPlayType = {
            val playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
            var defaultPos = 0
            val players = PlayerHelper.getExistPlayerTypes()
            val renders = ArrayList<Int>()
            for (p in players.indices) {
                renders.add(p)
                if (players[p] == playerType) defaultPos = p
            }
            val dialog = SelectDialog<Int>(this)
            dialog.setTip("请选择默认播放器")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    val thisPlayerType = players[pos]
                    Hawk.put(HawkConfig.PLAY_TYPE, thisPlayerType)
                    PlayerHelper.getPlayerName(thisPlayerType)
                    PlayerHelper.init()
                }
                override fun getDisplay(value: Int?): String = PlayerHelper.getPlayerName(players[value ?: 0])
            }, object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
                override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            }, renders, defaultPos)
            dialog.show()
        },
        onClickRender = {
            val defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0)
            val renders = arrayListOf(0, 1)
            val dialog = SelectDialog<Int>(this)
            dialog.setTip("请选择默认渲染方式")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_RENDER, pos)
                    PlayerHelper.init()
                }
                override fun getDisplay(value: Int?): String = PlayerHelper.getRenderName(value ?: 0)
            }, object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
                override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            }, renders, defaultPos)
            dialog.show()
        },
        onClickScale = {
            val defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0)
            val options = arrayListOf(0, 1, 2, 3, 4, 5)
            val dialog = SelectDialog<Int>(this)
            dialog.setTip("请选择画面缩放")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_SCALE, pos)
                }
                override fun getDisplay(value: Int?): String = PlayerHelper.getScaleName(value ?: 0)
            }, object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
                override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            }, options, defaultPos)
            dialog.show()
        },
        onClickHistoryNum = {
            val defaultPos = Hawk.get(HawkConfig.HISTORY_NUM, 0)
            val types = arrayListOf(0, 1, 2)
            val dialog = SelectDialog<Int>(this)
            dialog.setTip("保留历史记录数量")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.HISTORY_NUM, pos)
                }
                override fun getDisplay(value: Int?): String = HistoryHelper.getHistoryNumName(value ?: 0)
            }, object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
                override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            }, types, defaultPos)
            dialog.show()
        },
        onClickMediaCodec = {
            val ijkCodes = ApiConfig.get().ijkCodes ?: return@SettingHandlers
            if (ijkCodes.isEmpty()) return@SettingHandlers
            var defaultPos = 0
            val ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "")
            for (j in ijkCodes.indices) {
                if (ijkSel == ijkCodes[j].name) {
                    defaultPos = j
                    break
                }
            }
            val dialog = SelectDialog<com.github.tvbox.osc.bean.IJKCode>(this)
            dialog.setTip("请选择IJK解码")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<com.github.tvbox.osc.bean.IJKCode?> {
                override fun click(value: com.github.tvbox.osc.bean.IJKCode?, pos: Int) {
                    value?.selected(true)
                    Hawk.put(HawkConfig.IJK_CODEC, value?.name)
                }
                override fun getDisplay(code: com.github.tvbox.osc.bean.IJKCode?): String = code?.name ?: ""
            }, object : androidx.recyclerview.widget.DiffUtil.ItemCallback<com.github.tvbox.osc.bean.IJKCode>() {
                override fun areItemsTheSame(oldItem: com.github.tvbox.osc.bean.IJKCode, newItem: com.github.tvbox.osc.bean.IJKCode) = oldItem === newItem
                override fun areContentsTheSame(oldItem: com.github.tvbox.osc.bean.IJKCode, newItem: com.github.tvbox.osc.bean.IJKCode) = oldItem.name.contentEquals(newItem.name)
            }, ijkCodes, defaultPos)
            dialog.show()
        },
        onClickPressSpeed = {
            val types = arrayListOf("2.0", "3.0", "4.0", "5.0", "6.0", "8.0", "10.0")
            val defaultPos = types.indexOf(Hawk.get(HawkConfig.VIDEO_SPEED, 2.0f).toString())
            val dialog = SelectDialog<String>(this)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<String?> {
                override fun click(value: String?, pos: Int) {
                    Hawk.put(HawkConfig.VIDEO_SPEED, value?.toFloat())
                }
                override fun getDisplay(name: String?): String = name ?: ""
            }, SelectDialogAdapter.stringDiff, types, defaultPos)
            dialog.show()
        },
        onClickBackup = {
            if (XXPermissions.isGranted(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
                BackupDialog(this).show()
            } else {
                XXPermissions.with(this)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                            if (all) BackupDialog(this@SettingComposeActivity).show()
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
        onClickClearCache = {
            XPopup.Builder(this)
                .isDarkTheme(Utils.isDarkTheme())
                .asConfirm("提示", "确定清空吗？") { onClickClearCache() }
                .show()
        },
        onClickTheme = {
            val oldTheme = Hawk.get(HawkConfig.THEME_TAG, 0)
            val themes = arrayOf("跟随系统", "浅色", "深色")
            val types = arrayListOf(0, 1, 2)
            val dialog = SelectDialog<Int>(this)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.THEME_TAG, pos)
                }
                override fun getDisplay(value: Int?): String = themes[value ?: 0]
            }, object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
                override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            }, types, oldTheme)
            dialog.setOnDismissListener {
                if (oldTheme != Hawk.get(HawkConfig.THEME_TAG, 0)) {
                    Utils.initTheme()
                    jumpActivity(MainActivity::class.java)
                }
            }
            dialog.show()
        }
    )

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
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(title = { Text("设置", fontSize = 18.sp, fontWeight = FontWeight.Bold) })
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
private fun DividerLine() {
    Divider(color = Color(0x22000000))
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
        Text(text = label, fontSize = 16.sp, color = Color(0xFF222222))
        Spacer(modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(text = value, fontSize = 14.sp, color = Color(0xFF666666))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.icon_pre),
            contentDescription = null,
            tint = Color(0xFF888888)
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
        Text(text = label, fontSize = 16.sp, color = Color(0xFF222222))
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
