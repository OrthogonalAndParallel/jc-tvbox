package com.github.tvbox.osc.ui.activity

import androidx.fragment.app.Fragment
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.ui.fragment.HomeComposeFragment
import com.github.tvbox.osc.ui.fragment.MyComposeFragment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.setContent
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 主页、导航栏
 */
class MainActivity : BaseActivity() {

    var fragments = listOf(HomeComposeFragment(), MyComposeFragment())

    var useCacheConfig = false

    private var viewPager: ViewPager2? = null

    override fun init() {
        useCacheConfig = intent.extras?.getBoolean(IntentKey.CACHE_CONFIG_CHANGED, false)?:false
    }

    override fun getLayoutResID(): Int = -1

    override fun initVb() {
        // Use pure Compose content
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var current by remember { mutableStateOf(0) }
                    Scaffold(
                        bottomBar = {
                            NavigationBar(modifier = Modifier.fillMaxWidth()) {
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