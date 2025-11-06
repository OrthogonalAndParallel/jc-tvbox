package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.cache.VodCollect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ImageView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.github.tvbox.osc.util.MD5
import com.github.tvbox.osc.picasso.RoundTransformation

class CollectActivity : BaseActivity() {

    private val tipVisible = mutableStateOf(false)
    private val collects = mutableStateListOf<VodCollect>()

    override fun init() {
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allVodRecord = RoomDataManger.getAllVodCollect()
            val list: MutableList<VodCollect> = ArrayList()
            for (vod in allVodRecord) list.add(vod)
            withContext(Dispatchers.Main) {
                collects.clear()
                collects.addAll(list)
                tipVisible.value = collects.isNotEmpty()
            }
        }
    }

    override fun getLayoutResID(): Int = -1

    @OptIn(ExperimentalMaterial3Api::class)
    override fun initVb() {
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val container = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = container,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
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

                    val showClear = remember { mutableStateOf(false) }
                    var showHelp by remember { mutableStateOf(false) }
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    if (showHelp) {
                        ModalBottomSheet(onDismissRequest = { showHelp = false }, sheetState = sheetState) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(text = "使用提示", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(text = "长按记录可逐条删除。")
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    if (showClear.value) {
                        AlertDialog(
                            onDismissRequest = { showClear.value = false },
                            title = { Text(text = "提示") },
                            text = { Text(text = "确定清空?") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    showClear.value = false
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        RoomDataManger.deleteVodCollectAll()
                                        withContext(Dispatchers.Main) {
                                            collects.clear()
                                            tipVisible.value = false
                                        }
                                    }
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showClear.value = false }) { Text("取消") }
                            }
                        )
                    }

                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                modifier = Modifier.statusBarsPadding(),
                                colors = colors,
                                title = { Text(text = "我的收藏") },
                                actions = {
                                    IconButton(onClick = { showHelp = true }) {
                                        Icon(Icons.Outlined.HelpOutline, contentDescription = "帮助")
                                    }
                                    IconButton(onClick = { showClear.value = true }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "清空")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Help tip moved to Modal Bottom Sheet, opened by the top-right '?' icon
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(collects) { index, vod ->
                                    CollectItem(
                                        vod = vod,
                                        onClick = {
                                            if (ApiConfig.get().getSource(vod.sourceKey) != null) {
                                                val bundle = Bundle()
                                                bundle.putString("id", vod.vodId)
                                                bundle.putString("sourceKey", vod.sourceKey)
                                                jumpActivity(DetailActivity::class.java, bundle)
                                            } else {
                                                val newIntent = Intent(mContext, FastSearchActivity::class.java)
                                                newIntent.putExtra("title", vod.name)
                                                newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                startActivity(newIntent)
                                            }
                                        },
                                        onLongPress = {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                RoomDataManger.deleteVodCollect(vod.id)
                                                withContext(Dispatchers.Main) {
                                                    collects.removeAt(index)
                                                    tipVisible.value = collects.isNotEmpty()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectItem(
    vod: VodCollect,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(110f / 160f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { iv ->
                    val pic = vod.pic
                    if (!pic.isNullOrEmpty()) {
                        com.squareup.picasso.Picasso.get()
                            .load(pic)
                            .transform(
                                RoundTransformation(
                                    MD5.string2MD5("${'$'}pic-collect")
                                ).centerCorp(true)
                                    .roundRadius(
                                        com.blankj.utilcode.util.ConvertUtils.dp2px(10f),
                                        RoundTransformation.RoundType.ALL
                                    )
                            )
                            .placeholder(com.github.tvbox.osc.R.drawable.img_loading_placeholder)
                            .error(com.github.tvbox.osc.R.drawable.img_loading_placeholder)
                            .into(iv)
                    } else {
                        iv.setImageResource(com.github.tvbox.osc.R.drawable.img_loading_placeholder)
                    }
                }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = vod.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}