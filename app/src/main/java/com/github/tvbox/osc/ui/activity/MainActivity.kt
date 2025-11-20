package com.github.tvbox.osc.ui.activity

import android.R.attr.scaleX
import android.R.attr.scaleY
import androidx.fragment.app.Fragment
import com.github.tvbox.osc.base.BaseActivity
import com.gyf.immersionbar.ImmersionBar
import com.github.tvbox.osc.util.Utils
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.ui.fragment.HomeComposeFragment
import com.github.tvbox.osc.ui.fragment.MyComposeFragment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.layout.WindowInsets
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.tvbox.osc.util.Platform
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow

/**
 * 主页、导航栏
 */
class MainActivity : BaseActivity() {

    var fragments = listOf(HomeComposeFragment(), MyComposeFragment())

    var useCacheConfig = false

    var viewPager: ViewPager2? = null

    override fun init() {
        useCacheConfig = intent.extras?.getBoolean(IntentKey.CACHE_CONFIG_CHANGED, false)?:false
        // Make status bar transparent so first visible fragment can recolor immediately
        // Also set a neutral window background close to TopBar surface to avoid white first-frame
        val bgColor = if (!Utils.isDarkTheme()) 0xFFF5F5F5.toInt() else 0xFF121212.toInt()
        window.setBackgroundDrawable(ColorDrawable(bgColor))
        ImmersionBar.with(this)
            .transparentStatusBar()
            .statusBarDarkFont(!Utils.isDarkTheme())
            .init()
        if (Platform.useTvUi(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

@Composable
private fun TvBottomBar(
    current: Int,
    onSelect: (Int) -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(container)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvNavItem(
                text = "首页",
                selected = current == 0,
                onClick = { onSelect(0) }
            )
            Spacer(modifier = Modifier.width(24.dp))
            TvNavItem(
                text = "我的",
                selected = current == 1,
                onClick = { onSelect(1) }
            )
        }
    }
}

@Composable
private fun TvNavItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale = if (focused || selected) 1.08f else 1.0f
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    Surface(
        shape = MaterialTheme.shapes.small,
        color = background,
        tonalElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

    override fun getLayoutResID(): Int = -1

    override fun initVb() {
        // Use pure Compose content
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var current by remember { mutableStateOf(0) }
                    val isTv = remember { Platform.useTvUi(this@MainActivity) }
                    if (isTv) {
                        Scaffold(contentWindowInsets = WindowInsets(0)) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                verticalAlignment = Alignment.Top
                            ) {
                                NavigationRail(
                                    header = null,
                                    modifier = Modifier
                                        .width(88.dp)
                                        .fillMaxHeight()
                                ) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    NavigationRailItem(
                                        selected = current == 0,
                                        onClick = {
                                            current = 0
                                            viewPager?.setCurrentItem(0, false)
                                        },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                                        label = { Text("首页") }
                                    )
                                    NavigationRailItem(
                                        selected = current == 1,
                                        onClick = {
                                            current = 1
                                            viewPager?.setCurrentItem(1, false)
                                        },
                                        icon = { Icon(Icons.Outlined.Person, contentDescription = "我的") },
                                        label = { Text("我的") }
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                // Content area
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { context ->
                                        ViewPager2(context).apply {
                                            viewPager = this
                                            offscreenPageLimit = fragments.size
                                            adapter = object : FragmentStateAdapter(this@MainActivity) {
                                                override fun getItemCount(): Int = fragments.size
                                                override fun createFragment(position: Int): Fragment = fragments[position]
                                            }
                                            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                                                override fun onPageSelected(position: Int) {
                                                    super.onPageSelected(position)
                                                    current = position
                                                }
                                            })
                                        }
                                    },
                                    update = { vp ->
                                        if (vp.currentItem != current) vp.setCurrentItem(current, false)
                                    }
                                )
                            }
                        }
                    } else {
                        Scaffold(
                            contentWindowInsets = WindowInsets(0),
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier.fillMaxWidth(),
                                    windowInsets = WindowInsets(0)
                                ) {
                                    NavigationBarItem(
                                        selected = current == 0,
                                        onClick = {
                                            current = 0
                                            viewPager?.setCurrentItem(0, false)
                                        },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                                        label = { Text("首页") }
                                    )
                                    NavigationBarItem(
                                        selected = current == 1,
                                        onClick = {
                                            current = 1
                                            viewPager?.setCurrentItem(1, false)
                                        },
                                        icon = { Icon(Icons.Outlined.Person, contentDescription = "我的") },
                                        label = { Text("我的") }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                factory = { context ->
                                    ViewPager2(context).apply {
                                        viewPager = this
                                        offscreenPageLimit = fragments.size
                                        adapter = object : FragmentStateAdapter(this@MainActivity) {
                                            override fun getItemCount(): Int = fragments.size
                                            override fun createFragment(position: Int): Fragment = fragments[position]
                                        }
                                        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                                            override fun onPageSelected(position: Int) {
                                                super.onPageSelected(position)
                                                current = position
                                            }
                                        })
                                    }
                                },
                                update = { vp ->
                                    if (vp.currentItem != current) vp.setCurrentItem(current, false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}