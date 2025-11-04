package com.github.tvbox.osc.ui.activity

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.VideoInfo
import com.github.tvbox.osc.constant.CacheConst
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.adapter.LocalVideoAdapter
import com.github.tvbox.osc.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

class VideoListActivity : BaseActivity() {
    private var mBucketDisplayName = ""
    private var mLocalVideoAdapter = LocalVideoAdapter()
    private var mSelectedCount = 0

    override fun init() {
        mBucketDisplayName = intent.extras?.getString("bucketDisplayName") ?: ""

        mLocalVideoAdapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { adapter: BaseQuickAdapter<*, *>, view: View?, position: Int ->
                val videoInfo = adapter.getItem(position) as VideoInfo?
                if (mLocalVideoAdapter.isSelectMode) {
                    videoInfo!!.isChecked = !videoInfo.isChecked
                    mLocalVideoAdapter.notifyDataSetChanged()
                } else {
                    val bundle = Bundle()
                    bundle.putString("videoList", GsonUtils.toJson(mLocalVideoAdapter.data))
                    bundle.putInt("position", position)
                    jumpActivity(LocalPlayActivity::class.java, bundle)
                }
            }
        mLocalVideoAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>, view: View?, position: Int ->
                toggleListSelectMode(true)
                val videoInfo = adapter.getItem(position) as VideoInfo?
                videoInfo!!.isChecked = true
                mLocalVideoAdapter.notifyDataSetChanged()
                true
            }
        mLocalVideoAdapter.setOnSelectCountListener { count: Int ->
            mSelectedCount = count
        }
    }

    private fun toggleListSelectMode(open: Boolean) {
        mLocalVideoAdapter.setSelectMode(open)
        if (!open) {
            mLocalVideoAdapter.notifyDataSetChanged()
        }
    }

    private fun cancelAll() {
        for (item in mLocalVideoAdapter.data) {
            item.isChecked = false
        }
        mLocalVideoAdapter.notifyDataSetChanged()
    }

    override fun refresh(event: RefreshEvent) {
        Handler().postDelayed({ groupVideos() }, 1000)
    }

    override fun onResume() {
        super.onResume()
        groupVideos()
    }

    private fun groupVideos() {
        val videoList = Utils.getVideoList()
        val collect = videoList.stream()
            .filter { videoInfo: VideoInfo -> videoInfo.bucketDisplayName == mBucketDisplayName }
            .collect(Collectors.toList())
        mLocalVideoAdapter.setNewData(collect)
    }

    override fun onBackPressed() {
        if (mLocalVideoAdapter.isSelectMode) {
            if (mSelectedCount > 0) {
                cancelAll()
            } else {
                toggleListSelectMode(false)
            }
        } else {
            super.onBackPressed()
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

                    val showDelete = remember { mutableStateOf(false) }

                    if (showDelete.value) {
                        AlertDialog(
                            onDismissRequest = { showDelete.value = false },
                            title = { Text("提示") },
                            text = { Text("确定删除所选视频吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDelete.value = false
                                    showLoadingDialog()
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val data = mLocalVideoAdapter.data
                                        val deleteList: MutableList<VideoInfo> = ArrayList()
                                        for (item in data) {
                                            if (item.isChecked) {
                                                deleteList.add(item)
                                                if (FileUtils.delete(item.path)) {
                                                    SPUtils.getInstance(CacheConst.VIDEO_DURATION_SP).remove(item.path)
                                                    SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).remove(item.path)
                                                    FileUtils.notifySystemToScan(FileUtils.getDirName(item.path))
                                                }
                                            }
                                        }
                                        data.removeAll(deleteList)
                                        withContext(Dispatchers.Main) {
                                            dismissLoadingDialog()
                                            mLocalVideoAdapter.notifyDataSetChanged()
                                            toggleListSelectMode(false)
                                        }
                                    }
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDelete.value = false }) { Text("取消") }
                            }
                        )
                    }

                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                modifier = Modifier.statusBarsPadding(),
                                colors = colors,
                                title = { Text(text = mBucketDisplayName) }
                            )
                        },
                        bottomBar = {
                            if (mLocalVideoAdapter.isSelectMode) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = {
                                        for (item in mLocalVideoAdapter.data) item.isChecked = true
                                        mLocalVideoAdapter.notifyDataSetChanged()
                                    }) { Text("全选") }
                                    TextButton(onClick = {
                                        cancelAll()
                                    }) { Text("取消全选") }
                                    TextButton(onClick = {
                                        if (mSelectedCount > 0) showDelete.value = true
                                    }, enabled = mSelectedCount > 0) { Text("删除") }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    androidx.recyclerview.widget.RecyclerView(ctx).apply {
                                        layoutManager = LinearLayoutManager(ctx)
                                        adapter = mLocalVideoAdapter
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