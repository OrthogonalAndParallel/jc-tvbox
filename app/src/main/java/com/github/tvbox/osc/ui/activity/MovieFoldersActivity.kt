package com.github.tvbox.osc.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.VideoFolder
import com.github.tvbox.osc.bean.VideoInfo
import com.github.tvbox.osc.ui.adapter.FolderAdapter
import com.github.tvbox.osc.util.Utils
import java.util.function.Function
import java.util.stream.Collectors

class MovieFoldersActivity : BaseActivity() {

    private var mFolderAdapter = FolderAdapter()
    override fun init() {
        mFolderAdapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                val videoFolder = adapter.getItem(position) as VideoFolder?
                if (videoFolder != null) {
                    val bundle = Bundle()
                    bundle.putString("bucketDisplayName", videoFolder.name)
                    jumpActivity(VideoListActivity::class.java, bundle)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        groupVideos()
    }

    /**
     * 按文件夹名字分组视频
     */
    private fun groupVideos() {
        val videoList = Utils.getVideoList()
        val videoMap = videoList.stream()
            .collect(
                Collectors.groupingBy { obj: VideoInfo -> obj.bucketDisplayName }
            )
        val videoFolders: MutableList<VideoFolder> = ArrayList()
        videoMap.forEach { (key: String?, value: List<VideoInfo>?) ->
            val videoFolder = VideoFolder(key, value)
            videoFolders.add(videoFolder)
        }
        mFolderAdapter.setNewData(videoFolders)
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
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                modifier = Modifier.statusBarsPadding(),
                                colors = colors,
                                title = { Text(text = "本地视频") }
                            )
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
                                        adapter = mFolderAdapter
                                        setPadding(
                                            (10.dp.value).toInt(),
                                            (10.dp.value).toInt(),
                                            (10.dp.value).toInt(),
                                            (10.dp.value).toInt()
                                        )
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