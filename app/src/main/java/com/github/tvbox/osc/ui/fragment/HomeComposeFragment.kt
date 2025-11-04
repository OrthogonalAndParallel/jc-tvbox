package com.github.tvbox.osc.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.api.ApiConfig.LoadConfigCallback
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.bean.AbsSortXml
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.ui.activity.*
import com.github.tvbox.osc.ui.dialog.LastViewedDialog
import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.ui.dialog.TipDialog
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectDialogInterface
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.github.tvbox.osc.server.ControlManager
import com.lxj.xpopup.XPopup
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.DiffUtil

class HomeComposeFragment : Fragment() {

    private var sourceViewModel: SourceViewModel? = null
    private val fragments: MutableList<BaseLazyFragment> = ArrayList()
    private val mHandler = Handler()

    private var mSortDataList: List<SortData> = ArrayList()
    private var dataInitOk = false
    private var jarInitOk = false

    var errorTipDialog: TipDialog? = null
    var onlyConfigChanged = false

    private var viewPager: ViewPager2? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ControlManager.get().startServer()
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    HomeScreen()
                }
            }
        }
    }

    @Composable
    private fun HomeScreen() {
        var currentTab by remember { mutableStateOf(0) }
        var tabTitles by remember { mutableStateOf(listOf<String>()) }
        var homeName by remember { mutableStateOf(getString(R.string.app_name)) }

        LaunchedEffect(Unit) {
            initViewModel(
                onTabsReady = { titles ->
                    tabTitles = titles
                },
                onHomeName = { name ->
                    homeName = name
                }
            )
            initData(onTabsChange = { titles ->
                tabTitles = titles
                // reset to first tab when data refresh
                currentTab = 0
                viewPager?.adapter?.notifyDataSetChanged()
                viewPager?.setCurrentItem(0, false)
            })
        }

        Scaffold(
            topBar = {
                TopBar(
                    title = homeName,
                    onTitleClick = {
                        if (dataInitOk && jarInitOk) showSiteSwitch() else ToastUtils.showShort("数据源未加载，长按刷新或切换订阅")
                    },
                    onTitleLongClick = { refreshHomeSources() },
                    onSearch = { startActivity(Intent(requireContext(), FastSearchActivity::class.java)) },
                    onHistory = { startActivity(Intent(requireContext(), HistoryActivity::class.java)) },
                    onCollect = { startActivity(Intent(requireContext(), CollectActivity::class.java)) }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (tabTitles.isNotEmpty()) {
                    ScrollableTabRow(selectedTabIndex = currentTab) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = currentTab == index,
                                onClick = {
                                    currentTab = index
                                    viewPager?.setCurrentItem(index, false)
                                },
                                text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            ViewPager2(context).apply {
                                viewPager = this
                                offscreenPageLimit = 3
                                adapter = object : FragmentStateAdapter(this@HomeComposeFragment) {
                                    override fun getItemCount(): Int = fragments.size
                                    override fun createFragment(position: Int) = fragments[position]
                                }
                                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        super.onPageSelected(position)
                                        currentTab = position
                                    }
                                })
                            }
                        },
                        update = { vp ->
                            if (vp.currentItem != currentTab) vp.setCurrentItem(currentTab, false)
                        }
                    )
                } else {
                    // Simple loading indicator
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopBar(
        title: String,
        onTitleClick: () -> Unit,
        onTitleLongClick: () -> Unit,
        onSearch: () -> Unit,
        onHistory: () -> Unit,
        onCollect: () -> Unit,
    ) {
        val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
        CenterAlignedTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = colors,
            title = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onTitleLongClick() },
                                onTap = { onTitleClick() }
                            )
                        },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            actions = {
                IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, contentDescription = "搜索") }
                IconButton(onClick = onHistory) { Icon(Icons.Outlined.History, contentDescription = "历史") }
                IconButton(onClick = onCollect) { Icon(Icons.Outlined.CollectionsBookmark, contentDescription = "收藏") }
            }
        )
    }

    private fun initViewModel(
        onTabsReady: (List<String>) -> Unit,
        onHomeName: (String) -> Unit
    ) {
        sourceViewModel?.sortResult?.observe(viewLifecycleOwner) { absXml: AbsSortXml? ->
            val titles = buildTabsAndFragments(absXml)
            onTabsReady(titles)
        }

        val home = ApiConfig.get().homeSourceBean
        if (home != null && !home.name.isNullOrEmpty()) {
            onHomeName(home.name)
        }
    }

    private fun initData(onTabsChange: (List<String>) -> Unit) {
        val mainActivity = activity as? MainActivity
        onlyConfigChanged = mainActivity?.useCacheConfig ?: false

        when {
            dataInitOk && jarInitOk -> {
                sourceViewModel?.getSort(ApiConfig.get().homeSourceBean.key)
            }
            dataInitOk && !jarInitOk -> {
                loadJar(onTabsChange)
            }
            else -> {
                loadConfig(onTabsChange)
            }
        }
    }

    private fun buildTabsAndFragments(absXml: AbsSortXml?): List<String> {
        val sortList = if (absXml?.classes != null && absXml.classes.sortList != null) {
            DefaultConfig.adjustSort(
                ApiConfig.get().homeSourceBean.key,
                absXml.classes.sortList,
                true
            )
        } else {
            DefaultConfig.adjustSort(ApiConfig.get().homeSourceBean.key, ArrayList(), true)
        }
        mSortDataList = sortList
        fragments.clear()
        val titles = ArrayList<String>()
        for (data in mSortDataList) {
            titles.add(data.name)
            if (data.id == "my0") {
                if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && absXml != null && absXml.videoList != null && absXml.videoList.size > 0) {
                    fragments.add(UserFragment.newInstance(absXml.videoList))
                } else {
                    fragments.add(UserFragment.newInstance(null))
                }
            } else {
                fragments.add(GridFragment.newInstance(data))
            }
        }
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 2 && titles.isNotEmpty()) {
            titles.removeAt(0)
            fragments.removeAt(0)
        }
        return titles
    }

    private fun loadConfig(onTabsChange: (List<String>) -> Unit) {
        ApiConfig.get().loadConfig(onlyConfigChanged, object : LoadConfigCallback {
            override fun retry() {
                mHandler.post { initData(onTabsChange) }
            }
            override fun success() {
                dataInitOk = true
                if (ApiConfig.get().spider.isEmpty()) {
                    jarInitOk = true
                }
                mHandler.postDelayed({ initData(onTabsChange) }, 50)
            }
            override fun error(msg: String) {
                if (msg.equals("-1", ignoreCase = true)) {
                    mHandler.post {
                        dataInitOk = true
                        jarInitOk = true
                        initData(onTabsChange)
                    }
                } else {
                    showTipDialog(msg)
                }
            }
        }, activity)
    }

    private fun loadJar(onTabsChange: (List<String>) -> Unit) {
        if (!ApiConfig.get().spider.isNullOrEmpty()) {
            ApiConfig.get().loadJar(
                onlyConfigChanged,
                ApiConfig.get().spider,
                object : LoadConfigCallback {
                    override fun success() {
                        jarInitOk = true
                        mHandler.postDelayed({
                            if (!onlyConfigChanged) {
                                queryHistory()
                            }
                            initData(onTabsChange)
                        }, 50)
                    }
                    override fun retry() {}
                    override fun error(msg: String) {
                        jarInitOk = true
                        mHandler.post {
                            ToastUtils.showShort("更新订阅失败")
                            initData(onTabsChange)
                        }
                    }
                })
        }
    }

    private fun showTipDialog(msg: String) {
        if (errorTipDialog == null) {
            errorTipDialog = TipDialog(requireActivity(), msg, "重试", "取消", object : TipDialog.OnListener {
                override fun left() {
                    mHandler.post {
                        initData { }
                        errorTipDialog?.hide()
                    }
                }
                override fun right() {
                    dataInitOk = true
                    jarInitOk = true
                    mHandler.post {
                        initData { }
                        errorTipDialog?.hide()
                    }
                }
                override fun cancel() {
                    dataInitOk = true
                    jarInitOk = true
                    mHandler.post {
                        initData { }
                        errorTipDialog?.hide()
                    }
                }
                override fun onTitleClick() {
                    errorTipDialog?.hide()
                    startActivity(Intent(requireContext(), SubscriptionActivity::class.java))
                }
            })
        }
        if (errorTipDialog?.isShowing != true) errorTipDialog?.show()
    }

    private fun refreshHomeSources() {
        val intent = Intent(App.getInstance(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        val bundle = Bundle()
        bundle.putBoolean(IntentKey.CACHE_CONFIG_CHANGED, true)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun showSiteSwitch() {
        val sites = ApiConfig.get().sourceBeanList
        if (sites.isNotEmpty()) {
            val dialog = SelectDialog<SourceBean>(requireActivity())
            val tvRecyclerView = dialog.findViewById<TvRecyclerView>(R.id.list)
            tvRecyclerView.layoutManager = V7GridLayoutManager(dialog.context, 2)
            dialog.setTip("请选择首页数据源")
            dialog.setAdapter(object : SelectDialogInterface<SourceBean?> {
                override fun click(value: SourceBean?, pos: Int) {
                    ApiConfig.get().setSourceBean(value)
                    refreshHomeSources()
                }
                override fun getDisplay(source: SourceBean?): String {
                    return source?.name ?: ""
                }
            }, object : DiffUtil.ItemCallback<SourceBean>() {
                override fun areItemsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                    return oldItem === newItem
                }
                override fun areContentsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                    return oldItem.key.contentEquals(newItem.key)
                }
            }, sites, sites.indexOf(ApiConfig.get().homeSourceBean))
            dialog.show()
        } else {
            ToastUtils.showLong("暂无可用数据源")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ControlManager.get().stopServer()
    }

    private fun queryHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val vodInfoList = withContext(Dispatchers.IO) {
                val allVodRecord = RoomDataManger.getAllVodRecord(100)
                val vodInfoList: MutableList<VodInfo?> = ArrayList()
                for (vodInfo in allVodRecord) {
                    if (vodInfo.playNote != null && vodInfo.playNote.isNotEmpty()) vodInfo.note =
                        vodInfo.playNote
                    vodInfoList.add(vodInfo)
                }
                vodInfoList
            }
            if (vodInfoList.isNotEmpty() && vodInfoList[0] != null) {
                XPopup.Builder(context)
                    .hasShadowBg(false)
                    .isDestroyOnDismiss(true)
                    .isCenterHorizontal(true)
                    .isTouchThrough(true)
                    .offsetY(ScreenUtils.getAppScreenHeight() - 360)
                    .asCustom(LastViewedDialog(requireContext(), vodInfoList[0]))
                    .show()
                    .delayDismiss(4000)
            }
        }
    }
}
