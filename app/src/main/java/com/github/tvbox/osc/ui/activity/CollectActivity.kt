package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.cache.VodCollect
import com.github.tvbox.osc.ui.adapter.CollectAdapter
import com.github.tvbox.osc.util.FastClickCheckUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectActivity : BaseActivity() {

    private var collectAdapter  = CollectAdapter()
    private val tipVisible = mutableStateOf(false)

    override fun init() {
        collectAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { _: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
                val vodInfo = collectAdapter.data[position]
                if (vodInfo != null) {
                    collectAdapter.remove(position)
                    RoomDataManger.deleteVodCollect(vodInfo.id)
                }
                tipVisible.value = collectAdapter.data.isNotEmpty()
                true
            }
        collectAdapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { _, view, position ->
                FastClickCheckUtil.check(view)
                val vodInfo = collectAdapter.data[position]
                if (vodInfo != null) {
                    if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                        val bundle = Bundle()
                        bundle.putString("id", vodInfo.vodId)
                        bundle.putString("sourceKey", vodInfo.sourceKey)
                        jumpActivity(DetailActivity::class.java, bundle)
                    } else {
                        val newIntent = Intent(mContext, FastSearchActivity::class.java)
                        newIntent.putExtra("title", vodInfo.name)
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(newIntent)
                    }
                }
            }
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allVodRecord = RoomDataManger.getAllVodCollect()
            val vodInfoList: MutableList<VodCollect> = ArrayList()
            for (vodInfo in allVodRecord) {
                vodInfoList.add(vodInfo)
            }
            withContext(Dispatchers.Main) {
                collectAdapter.setNewData(vodInfoList)
                tipVisible.value = vodInfoList.isNotEmpty()
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
                                            collectAdapter.setNewData(ArrayList())
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
                                    IconButton(onClick = { showClear.value = true }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "清空")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                        ) {
                            if (tipVisible.value) {
                                androidx.compose.material3.Text(
                                    text = "长按记录逐条删除",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .padding(vertical = 10.dp)
                                )
                            }
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    androidx.recyclerview.widget.RecyclerView(ctx).apply {
                                        setHasFixedSize(true)
                                        layoutManager = GridLayoutManager(ctx, 3)
                                        adapter = collectAdapter
                                        setPadding(10.dp.value.toInt(), 10.dp.value.toInt(), 10.dp.value.toInt(), 0)
                                        clipToPadding = false
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