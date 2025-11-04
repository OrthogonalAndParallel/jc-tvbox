@file:OptIn(ExperimentalMaterial3Api::class)
package com.github.tvbox.osc.ui.fragment

import android.app.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.activity.*
import com.github.tvbox.osc.util.Utils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup

/**
 * 我的
 */
class MyComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            var showAbout by remember { mutableStateOf(false) }
            var showAddr by remember { mutableStateOf(false) }
            var addrText by remember { mutableStateOf("") }
            Surface(color = MaterialTheme.colorScheme.background) {
                MyScaffold(
                    onSubscription = { jumpActivity(SubscriptionActivity::class.java) },
                    onHistory = { jumpActivity(HistoryActivity::class.java) },
                    onFavorite = { jumpActivity(CollectActivity::class.java) },
                    onLive = { jumpActivity(LiveActivity::class.java) },
                    onLocal = {
                        if (!XXPermissions.isGranted(requireContext(), Permission.MANAGE_EXTERNAL_STORAGE)) {
                            showPermissionTipPopup()
                        } else {
                            jumpActivity(MovieFoldersActivity::class.java)
                        }
                    },
                    onAddrPlay = {
                        addrText = ClipboardUtils.getText()?.toString()?.takeIf { isPush(it) } ?: ""
                        showAddr = true
                    },
                    onAbout = { showAbout = true },
                    onSetting = { jumpActivity(SettingComposeActivity::class.java) }
                )
                if (showAddr) {
                    AlertDialog(
                        onDismissRequest = { showAddr = false },
                        title = { Text("播放") },
                        text = {
                            TextField(
                                value = addrText,
                                onValueChange = { addrText = it },
                                placeholder = { Text("地址") },
                                singleLine = true,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val text = addrText
                                if (!text.isNullOrEmpty()) {
                                    val newIntent = Intent(requireContext(), DetailActivity::class.java)
                                    newIntent.putExtra("id", text)
                                    newIntent.putExtra("sourceKey", "push_agent")
                                    newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(newIntent)
                                    showAddr = false
                                }
                            }) { Text("确定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddr = false }) { Text("取消") }
                        }
                    )
                }
                if (showAbout) {
                    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
                    androidx.compose.material3.ModalBottomSheet(
                        onDismissRequest = { showAbout = false },
                        sheetState = sheetState
                    ) {
                        AboutBottomSheetContent(onClose = { showAbout = false })
                    }
                }
            }
        }
        return composeView
    }

    private fun jumpActivity(clazz: Class<out Activity>){
        val intent = Intent(requireContext(), clazz)
        startActivity(intent)
    }

    private fun showPermissionTipPopup(){
        XPopup.Builder(requireActivity())
            .isDarkTheme(Utils.isDarkTheme())
            .asConfirm("提示","为了播放视频、音频等,我们需要访问您设备文件的读写权限", {
                getPermission()
            }).show()
    }

    private fun getPermission(){
        XXPermissions.with(this)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        jumpActivity(MovieFoldersActivity::class.java)
                    } else {
                        ToastUtils.showLong("部分权限未正常授予,请授权")
                    }
                }
                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    if (never) {
                        ToastUtils.showLong("读写文件权限被永久拒绝，请手动授权")
                        XXPermissions.startPermissionActivity(requireActivity(), permissions)
                    } else {
                        ToastUtils.showShort("获取权限失败")
                        showPermissionTipPopup()
                    }
                }
            })
    }

    private fun isPush(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        return try {
            val scheme = Uri.parse(text).scheme
            listOf("smb", "http", "https", "thunder", "magnet", "ed2k", "mitv", "jianpian").contains(scheme)
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
private fun AboutBottomSheetContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "关于",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "JCBox - 开源影视聚合播放器",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "项目地址: https://github.com/OrthogonalAndParallel/TVBoxOS-Mobile",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = DividerDefaults.color)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "关闭",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onClose() }
            )
        }
    }
}

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
private fun MyScaffold(
    onSubscription: () -> Unit,
    onHistory: () -> Unit,
    onFavorite: () -> Unit,
    onLive: () -> Unit,
    onLocal: () -> Unit,
    onAddrPlay: () -> Unit,
    onAbout: () -> Unit,
    onSetting: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        text = "JCBox",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                }
            )
        }
    ) { innerPadding ->
        MyScreen(
            modifier = Modifier.padding(innerPadding),
            onSubscription = onSubscription,
            onHistory = onHistory,
            onFavorite = onFavorite,
            onLive = onLive,
            onLocal = onLocal,
            onAddrPlay = onAddrPlay,
            onAbout = onAbout,
            onSetting = onSetting
        )
    }
}

@Composable
private fun MyScreen(
    modifier: Modifier = Modifier,
    onSubscription: () -> Unit,
    onHistory: () -> Unit,
    onFavorite: () -> Unit,
    onLive: () -> Unit,
    onLocal: () -> Unit,
    onAddrPlay: () -> Unit,
    onAbout: () -> Unit,
    onSetting: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Content

        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp)
                .verticalScroll(scroll)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    MenuItem(
                        iconRes = R.drawable.ic_database_48,
                        title = "订阅管理",
                        onClick = onSubscription
                    )
                    HorizontalDivider(color = DividerDefaults.color)
                    MenuItem(
                        iconRes = R.drawable.ic_history_48,
                        title = "观看历史",
                        onClick = onHistory
                    )
                    HorizontalDivider(color = DividerDefaults.color)
                    MenuItem(
                        iconRes = R.drawable.ic_collect_48,
                        title = "收藏",
                        onClick = onFavorite
                    )
                    HorizontalDivider(color = DividerDefaults.color)
                    MenuItem(
                        iconRes = R.drawable.ic_live,
                        title = "直播",
                        onClick = onLive
                    )
                    HorizontalDivider(color = DividerDefaults.color)
                    MenuItem(
                        iconRes = R.drawable.ic_local_video_48,
                        title = "本地视频",
                        onClick = onLocal
                    )
                    HorizontalDivider(color = DividerDefaults.color)
                    MenuItem(
                        iconRes = R.drawable.ic_copyright_48,
                        title = "播放链接",
                        onClick = onAddrPlay
                    )
                    HorizontalDivider(color = DividerDefaults.color)
                    MenuItem(
                        iconRes = R.drawable.ic_about_48,
                        title = "关于",
                        onClick = onAbout
                    )
                    HorizontalDivider(color = DividerDefaults.color)
                    MenuItem(
                        iconRes = R.drawable.ic_settings,
                        title = "设置",
                        onClick = onSetting
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 20.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.icon_pre),
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .alpha(0.9f),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
