package com.github.tvbox.osc.ui.activity

import android.os.Process
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.databinding.ActivityMainBinding
import com.github.tvbox.osc.ui.fragment.GridFragment
import com.github.tvbox.osc.ui.fragment.HomeFragment
import com.github.tvbox.osc.ui.fragment.MyComposeFragment
import kotlin.system.exitProcess

// Compose imports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Person

/**
 * 主页、导航栏
 */
class MainActivity : BaseVbActivity<ActivityMainBinding>() {

    var fragments = listOf(HomeFragment(), MyComposeFragment())
    var useCacheConfig = false
    private var exitTime = 0L

    override fun init() {

        useCacheConfig = intent.extras?.getBoolean(IntentKey.CACHE_CONFIG_CHANGED, false)?:false

        mBinding.vp.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }
        }

        // Compose Bottom Navigation
        val composeView = mBinding.composeBottomNav
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            var current by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                current = mBinding.vp.currentItem
            }
            NavigationBar {
                NavigationBarItem(
                    selected = current == 0,
                    onClick = {
                        current = 0
                        mBinding.vp.setCurrentItem(0, false)
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = current == 1,
                    onClick = {
                        current = 1
                        mBinding.vp.setCurrentItem(1, false)
                    },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "我的") },
                    label = { Text("我的") }
                )
            }
        }

        mBinding.vp.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                // trigger recomposition by resetting compose content with new selected index
                mBinding.composeBottomNav.setContent {
                    var current by remember { mutableStateOf(position) }
                    NavigationBar {
                        NavigationBarItem(
                            selected = current == 0,
                            onClick = {
                                current = 0
                                mBinding.vp.setCurrentItem(0, false)
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                            label = { Text("首页") }
                        )
                        NavigationBarItem(
                            selected = current == 1,
                            onClick = {
                                current = 1
                                mBinding.vp.setCurrentItem(1, false)
                            },
                            icon = { Icon(Icons.Outlined.Person, contentDescription = "我的") },
                            label = { Text("我的") }
                        )
                    }
                }
            }
        })
    }

    override fun onBackPressed() {
        if (mBinding.vp.currentItem == 1) {
            mBinding.vp.currentItem = 0
            return
        }
        val homeFragment = fragments[0] as HomeFragment
        if (!homeFragment.isAdded) { // 资源不足销毁重建时未挂载到activity时getChildFragmentManager会崩溃
            confirmExit()
            return
        }
        val childFragments = homeFragment.allFragments
        if (childFragments.isEmpty()) { //加载中(没有tab)
            confirmExit()
            return
        }
        val fragment: Fragment = childFragments[homeFragment.tabIndex]
        if (fragment is GridFragment) { // 首页数据源动态加载的tab
            if (!fragment.restoreView()) { // 有回退的view,先回退(AList等文件夹列表),没有可回退的,返到主页tab
                if (!homeFragment.scrollToFirstTab()) {
                    confirmExit()
                }
            }
        } else {
            confirmExit()
        }
    }

    private fun confirmExit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            ToastUtils.showShort("再按一次退出程序")
            exitTime = System.currentTimeMillis()
        } else {
            ActivityUtils.finishAllActivities(true)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }
}