@file:OptIn(ExperimentalMaterial3Api::class)
package com.github.tvbox.osc.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.angcyo.tablayout.DslTabLayout
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.catvod.crawler.JsLoader
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.event.ServerEvent
import com.github.tvbox.osc.ui.adapter.FastSearchAdapter
import com.github.tvbox.osc.ui.dialog.SearchCheckboxDialog
import com.github.tvbox.osc.ui.dialog.SearchSuggestionsDialog
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.SearchHelper
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.orhanobut.hawk.Hawk
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import com.owen.tvrecyclerview.widget.TvRecyclerView
import android.widget.ImageView
import android.view.ViewGroup
import com.zhy.view.flowlayout.TagFlowLayout
import okhttp3.Response
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class FastSearchActivity : BaseActivity(), TextWatcher {

    companion object {
        private var mCheckSources: HashMap<String, String>? = null
        fun setCheckedSourcesForSearch(checkedSources: HashMap<String, String>?) {
            mCheckSources = checkedSources
        }

    @Composable
    private fun FastSearchTopBar(
        onBack: () -> Unit,
        onFilter: () -> Unit,
        onSearch: (String) -> Unit,
        onQueryChange: (String) -> Unit,
        provideAnchor: (View) -> Unit,
    ) {
        var query by remember { mutableStateOf("") }
        val container = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = container,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = container.toArgb()
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = container.luminance() > 0.5f
            }
        }
        CenterAlignedTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = colors,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            title = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onQueryChange(it)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("搜索") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            AndroidView(
                                modifier = Modifier.size(1.dp),
                                factory = { ctx -> View(ctx).also { provideAnchor(it) } }
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
            },
            actions = {
                IconButton(onClick = onFilter) { Icon(Icons.Outlined.FilterList, contentDescription = "筛选") }
                IconButton(onClick = { onSearch(query) }) { Icon(Icons.Filled.Search, contentDescription = "搜索") }
            }
        )
    }
    }

    private lateinit var sourceViewModel : SourceViewModel
    private var searchAdapter = FastSearchAdapter()
    private var searchAdapterFilter = FastSearchAdapter()
    private var searchTitle: String? = ""
    private var spNames = HashMap<String, String>()
    private var isFilterMode = false
    private var searchFilterKey: String? = "" // 过滤的key
    private var resultVods = HashMap<String, MutableList<Movie.Video>>()
    private var pauseRunnable: MutableList<Runnable>? = null
    private var mSearchSuggestionsDialog: SearchSuggestionsDialog? = null
    // Views hosted via AndroidView inside Compose
    private lateinit var tabLayout: DslTabLayout
    private lateinit var mGridView: TvRecyclerView
    private lateinit var mGridViewFilter: TvRecyclerView
    private lateinit var llLayout: LinearLayout
    private val showSuggest = mutableStateOf(true)
    private var suggestAnchorView: View? = null
    private lateinit var historyFlow: com.zhy.view.flowlayout.TagFlowLayout
    private lateinit var hotFlow: com.zhy.view.flowlayout.TagFlowLayout
    private var pendingInitTabs: Boolean = false

    override fun init() {
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        initData()
    }

    override fun onResume() {
        super.onResume()
        if (pauseRunnable != null && pauseRunnable!!.size > 0) {
            searchExecutorService = Executors.newFixedThreadPool(10)
            allRunCount.set(pauseRunnable!!.size)
            for (runnable: Runnable? in pauseRunnable!!) {
                searchExecutorService!!.execute(runnable)
            }
            pauseRunnable!!.clear()
            pauseRunnable = null
        }
    }

    override fun getLayoutResID(): Int = -1

    override fun initVb() {
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FastSearchTopBar(
                        onBack = { finish() },
                        onFilter = { filterSearchSource() },
                        onSearch = { query -> search(query) },
                        onQueryChange = { text ->
                            if (text.isEmpty()) {
                                mSearchSuggestionsDialog?.dismiss()
                                hideHotAndHistorySearch(false)
                            } else {
                                getSuggest(text)
                            }
                        },
                        provideAnchor = { v -> suggestAnchorView = v }
                    )
                    if (showSuggest.value) {
                        // Suggestion area: history + hot
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            factory = { ctx ->
                                LinearLayout(ctx).apply {
                                    orientation = LinearLayout.VERTICAL
                                    // History header
                                    val header = LinearLayout(ctx).apply {
                                        orientation = LinearLayout.HORIZONTAL
                                        val tv = TextView(ctx)
                                        tv.text = "历史搜索"
                                        tv.textSize = 15f
                                        addView(tv)
                                        val clear = ImageView(ctx)
                                        clear.setImageResource(R.drawable.ic_clear)
                                        val lp = LinearLayout.LayoutParams(40.dp.value.toInt(), 40.dp.value.toInt())
                                        lp.marginStart = 16
                                        clear.layoutParams = lp
                                        clear.setOnClickListener { view ->
                                            Hawk.put(HawkConfig.HISTORY_SEARCH, ArrayList<Any>())
                                            view.postDelayed({ renderHistory(this) }, 300)
                                        }
                                        addView(clear)
                                    }
                                    addView(header)
                                    // History flow
                                    historyFlow = TagFlowLayout(ctx)
                                    addView(historyFlow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                                    renderHistory(this)
                                    // Hot header
                                    val hotTitle = TextView(ctx)
                                    hotTitle.text = "热门搜索"
                                    hotTitle.textSize = 15f
                                    val hotLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                    hotLp.topMargin = (10.dp.value).toInt()
                                    addView(hotTitle, hotLp)
                                    // Hot flow
                                    hotFlow = TagFlowLayout(ctx)
                                    addView(hotFlow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                                    loadHotWords()
                                }
                            }
                        )
                    } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left: DslTabLayout
                        AndroidView(
                            modifier = Modifier.width(120.dp).fillMaxHeight(),
                            factory = { ctx ->
                                DslTabLayout(ctx).also { tl ->
                                    tabLayout = tl
                                    tl.configTabLayoutConfig {
                                        onSelectViewChange = { _, selectViewList, _, _ ->
                                            val tvItem: TextView = selectViewList.first() as TextView
                                            filterResult(tvItem.text.toString())
                                        }
                                    }
                                    if (pendingInitTabs) {
                                        pendingInitTabs = false
                                        // now safe to build tabs
                                        searchResult()
                                    }
                                }
                            }
                        )
                        // Right: container with two RecyclerViews
                        AndroidView(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 10.dp),
                            factory = { ctx ->
                                LinearLayout(ctx).apply {
                                    orientation = LinearLayout.VERTICAL
                                    llLayout = this
                                    // main list
                                    mGridView = TvRecyclerView(ctx).also { rv ->
                                        rv.setHasFixedSize(true)
                                        rv.layoutManager = LinearLayoutManager(ctx)
                                        rv.adapter = searchAdapter
                                        rv.visibility = View.INVISIBLE
                                    }
                                    addView(mGridView, LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT
                                    ))
                                    // filter list
                                    mGridViewFilter = TvRecyclerView(ctx).also { rv ->
                                        rv.layoutManager = LinearLayoutManager(ctx)
                                        rv.adapter = searchAdapterFilter
                                        rv.visibility = View.GONE
                                    }
                                    addView(mGridViewFilter, LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT
                                    ))
                                    // Avoid wrapping this view with LoadSir inside factory to prevent parent conflicts
                                }
                            },
                            update = {
                                if (this@FastSearchActivity::mGridView.isInitialized && this@FastSearchActivity::mGridViewFilter.isInitialized) {
                                    if (isFilterMode) {
                                        mGridView.visibility = View.GONE
                                        mGridViewFilter.visibility = View.VISIBLE
                                    } else {
                                        mGridView.visibility = View.VISIBLE
                                        mGridViewFilter.visibility = View.GONE
                                    }
                                }
                            }
                        )
                    }
                    }
                }
            }
        }
        // item click listeners
        searchAdapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val video = searchAdapter.data[position]
            try {
                if (searchExecutorService != null) {
                    pauseRunnable = searchExecutorService!!.shutdownNow()
                    searchExecutorService = null
                    JsLoader.stopAll()
                }
            } catch (th: Throwable) {
                th.printStackTrace()
            }
            val bundle = Bundle()
            bundle.putString("id", video.id)
            bundle.putString("sourceKey", video.sourceKey)
            jumpActivity(DetailActivity::class.java, bundle)
        }
        searchAdapterFilter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val video = searchAdapterFilter.data[position]
            if (video != null) {
                try {
                    if (searchExecutorService != null) {
                        pauseRunnable = searchExecutorService!!.shutdownNow()
                        searchExecutorService = null
                        JsLoader.stopAll()
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
                val bundle = Bundle()
                bundle.putString("id", video.id)
                bundle.putString("sourceKey", video.sourceKey)
                jumpActivity(DetailActivity::class.java, bundle)
            }
        }
    }

    /**
     * 指定搜索源(过滤)
     */
    private fun filterSearchSource() {
        val allSourceBean = ApiConfig.get().sourceBeanList
        if (allSourceBean.isNotEmpty()) {
            val searchAbleSource: MutableList<SourceBean> = ArrayList()
            for (sourceBean: SourceBean in allSourceBean) {
                if (sourceBean.isSearchable) {
                    searchAbleSource.add(sourceBean)
                }
            }
            val mSearchCheckboxDialog = SearchCheckboxDialog(this@FastSearchActivity, searchAbleSource, mCheckSources)
            mSearchCheckboxDialog.show()
        }

    }

    private fun filterResult(spName: String) {
        if (spName === "全部显示") {
            isFilterMode = false
            return
        }
        val key = spNames[spName]
        if (key.isNullOrEmpty()) return
        if (searchFilterKey === key) return
        isFilterMode = true
        searchFilterKey = key
        val list: List<Movie.Video> = (resultVods[key]) ?: emptyList()
        searchAdapterFilter.setNewData(list)
    }

    private fun initData() {
        mCheckSources = SearchHelper.getSourcesForSearch()
        if (intent != null && intent.hasExtra("title")) {
            val title = intent.getStringExtra("title")
            if (!TextUtils.isEmpty(title)) {
                showLoading()
                search(title)
            }
        } else {
            // 首次进入且无预置标题，默认展示 历史/热门
            hideHotAndHistorySearch(false)
        }
    }

    private fun hideHotAndHistorySearch(isHide: Boolean) {
        showSuggest.value = !isHide
    }

    private fun initHistorySearch() {
        // 步骤B暂不展示历史/热门区域
    }

    /**
     * 热门搜索
     */
    private val hotWords: Unit
        get() { /* unused now; using loadHotWords() in Compose */ }

    /**
     * 联想搜索
     */
    private fun getSuggest(text: String) {
        // 加载热词
        OkGo.get<String>("https://suggest.video.iqiyi.com/?if=mobile&key=$text")
            .execute(object : AbsCallback<String?>() {
                override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
                    val titles: MutableList<String> = ArrayList()
                    try {
                        val json = JsonParser.parseString(response.body()).asJsonObject
                        val datas = json["data"].asJsonArray
                        for (data: JsonElement in datas) {
                            val item = data as JsonObject
                            titles.add(item["name"].asString.trim { it <= ' ' })
                        }
                    } catch (th: Throwable) {
                        LogUtils.d(th.toString())
                    }
                    if (titles.isNotEmpty()) {
                        showSuggestDialog(titles)
                    }
                }

                @Throws(Throwable::class)
                override fun convertResponse(response: Response): String {
                    return response.body()!!.string()
                }
            })
    }

    private fun showSuggestDialog(list: List<String>) {
        if (mSearchSuggestionsDialog == null) {
            mSearchSuggestionsDialog =
                SearchSuggestionsDialog(this@FastSearchActivity, list
                ) { _, text ->
                    LogUtils.d("搜索:$text")
                    mSearchSuggestionsDialog!!.dismissWith { search(text) }
                }
            val builder = XPopup.Builder(this@FastSearchActivity)
                .isViewMode(true)
                .isRequestFocus(false) //不强制焦点
                .setPopupCallback(object : SimpleCallback() {
                    override fun onDismiss(popupView: BasePopupView) { // 弹窗关闭了就置空对象,下次重新new
                        super.onDismiss(popupView)
                        mSearchSuggestionsDialog = null
                    }
                })
            suggestAnchorView?.let { anchor ->
                builder.atView(anchor).notDismissWhenTouchInView(anchor)
                val extra = (4 * this@FastSearchActivity.resources.displayMetrics.density).toInt()
                val dy = anchor.height / 2 + extra
                builder.offsetY(dy)
            }
            builder.asCustom(mSearchSuggestionsDialog).show()
        } else { // 不为空说明弹窗为打开状态(关闭就置空了).直接刷新数据
            mSearchSuggestionsDialog!!.updateSuggestions(list)
        }
    }

    private fun renderHistory(parent: LinearLayout) {
        val mSearchHistory: List<String> = Hawk.get(HawkConfig.HISTORY_SEARCH, ArrayList())
        historyFlow.adapter = object : TagAdapter<String?>(mSearchHistory) {
            override fun getView(parent: FlowLayout, position: Int, s: String?): View {
                val tv = TextView(this@FastSearchActivity)
                tv.text = s
                tv.textSize = 14f
                tv.setPadding(20, 10, 20, 10)
                return tv
            }
        }
        historyFlow.setOnTagClickListener { _: View?, position: Int, _: FlowLayout? ->
            search(mSearchHistory[position])
            true
        }
    }

    private fun loadHotWords() {
        OkGo.get<String>("https://node.video.qq.com/x/api/hot_search")
            .params("channdlId", "0")
            .params("_", System.currentTimeMillis())
            .execute(object : AbsCallback<String?>() {
                override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
                    try {
                        val hots = ArrayList<String>()
                        val itemList =
                            JsonParser.parseString(response.body()).asJsonObject["data"].asJsonObject["mapResult"].asJsonObject["0"].asJsonObject["listInfo"].asJsonArray
                        for (ele: JsonElement in itemList) {
                            val obj = ele as JsonObject
                            hots.add(obj["title"].asString.trim { it <= ' ' }
                                .replace("<|>|《|》|-".toRegex(), "").split(" ".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()[0])
                        }
                        hotFlow.adapter = object : TagAdapter<String?>(hots as List<String?>?) {
                            override fun getView(
                                parent: FlowLayout,
                                position: Int,
                                s: String?
                            ): View {
                                val tv = TextView(this@FastSearchActivity)
                                tv.text = s
                                tv.textSize = 14f
                                tv.setPadding(20, 10, 20, 10)
                                return tv
                            }
                        }
                        hotFlow.setOnTagClickListener { _: View?, position: Int, _: FlowLayout? ->
                            search(hots[position])
                            true
                        }
                    } catch (th: Throwable) {
                        th.printStackTrace()
                    }
                }

                override fun convertResponse(response: Response): String {
                    return response.body()!!.string()
                }
            })
    }

    private fun saveSearchHistory(searchWord: String?) {
        if (!searchWord.isNullOrEmpty()) {
            val history = Hawk.get(HawkConfig.HISTORY_SEARCH, ArrayList<String?>())
            if (!history.contains(searchWord)) {
                history.add(0, searchWord)
            } else {
                history.remove(searchWord)
                history.add(0, searchWord)
            }
            if (history.size > 30) {
                history.removeAt(30)
            }
            Hawk.put(HawkConfig.HISTORY_SEARCH, history)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun server(event: ServerEvent) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            val title = event.obj as String
            showLoading()
            search(title)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(if (event.obj == null) null else event.obj as AbsXml)
            } catch (e: Exception) {
                searchData(null)
            }
        }
    }

    private fun search(title: String?) {
        if (title.isNullOrEmpty()) {
            ToastUtils.showShort("请输入搜索内容")
            return
        }

        if (mSearchSuggestionsDialog != null && mSearchSuggestionsDialog!!.isShow) {
            mSearchSuggestionsDialog!!.dismiss()
        }
        if (!Hawk.get(HawkConfig.PRIVATE_BROWSING, false)) { //无痕浏览不存搜索历史
            saveSearchHistory(title)
        }
        // 切换为结果区域，由 Compose update 控制显示隐藏
        hideHotAndHistorySearch(true)
        KeyboardUtils.hideSoftInput(this)
        cancel()
        showLoading()
        searchTitle = title
        //fenci();
        searchAdapter.setNewData(ArrayList())
        searchAdapterFilter.setNewData(ArrayList())
        resultVods.clear()
        searchFilterKey = ""
        isFilterMode = false
        spNames.clear()
        if (this::tabLayout.isInitialized) {
            tabLayout.removeAllViews()
        }
        searchResult()
    }

    private var searchExecutorService: ExecutorService? = null
    private val allRunCount = AtomicInteger(0)
    private fun getSiteTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.gravity = Gravity.CENTER
        val params = DslTabLayout.LayoutParams(-2, -2)
        params.topMargin = 20
        params.bottomMargin = 20
        textView.setPadding(20, 10, 20, 10)
        textView.layoutParams = params
        return textView
    }

    private fun searchResult() {
        if (!this::tabLayout.isInitialized) {
            pendingInitTabs = true
            return
        }
        try {
            if (searchExecutorService != null) {
                searchExecutorService!!.shutdownNow()
                searchExecutorService = null
                JsLoader.stopAll()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        } finally {
            searchAdapter.setNewData(ArrayList())
            searchAdapterFilter.setNewData(ArrayList())
            allRunCount.set(0)
        }
        searchExecutorService = Executors.newFixedThreadPool(10)
        val searchRequestList: MutableList<SourceBean> = ArrayList()
        searchRequestList.addAll(ApiConfig.get().sourceBeanList)
        val home = ApiConfig.get().homeSourceBean
        searchRequestList.remove(home)
        searchRequestList.add(0, home)
        val siteKey = ArrayList<String>()
        tabLayout.addView(getSiteTextView("全部显示"))
        tabLayout.setCurrentItem(0, true, false)
        for (bean: SourceBean in searchRequestList) {
            if (!bean.isSearchable) {
                continue
            }
            if (mCheckSources != null && !mCheckSources!!.containsKey(bean.key)) {
                continue
            }
            siteKey.add(bean.key)
            spNames[bean.name] = bean.key
            allRunCount.incrementAndGet()
        }
        for (key: String in siteKey) {
            searchExecutorService!!.execute {
                try {
                    sourceViewModel.getSearch(key, searchTitle)
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * 添加到最后面并返回最后一个key
     * @param key
     * @return
     */
    private fun addWordAdapterIfNeed(key: String): String {
        try {
            var name = ""
            for (n: String in spNames.keys) {
                if ((spNames[n] == key)) {
                    name = n
                }
            }
            if ((name == "")) return key
            for (i in 0 until tabLayout.childCount) {
                val item = tabLayout.getChildAt(i) as TextView
                if ((name == item.text.toString())) {
                    return key
                }
            }
            tabLayout.addView(getSiteTextView(name))
            return key
        } catch (e: Exception) {
            return key
        }
    }

    private fun matchSearchResult(name: String, searchTitle: String?): Boolean {
        var searchTitle = searchTitle
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false
        searchTitle = searchTitle!!.trim { it <= ' ' }
        val arr = searchTitle.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var matchNum = 0
        for (one: String in arr) {
            if (name.contains(one)) matchNum++
        }
        return if (matchNum == arr.size) true else false
    }

    private fun searchData(absXml: AbsXml?) {
        var lastSourceKey = ""
        if ((absXml != null) && (absXml.movie != null) && (absXml.movie.videoList != null) && (absXml.movie.videoList.size > 0)) {
            val data: MutableList<Movie.Video> = ArrayList()
            for (video: Movie.Video in absXml.movie.videoList) {
                if (!matchSearchResult(video.name, searchTitle)) continue
                data.add(video)
                if (!resultVods.containsKey(video.sourceKey)) {
                    resultVods[video.sourceKey] = ArrayList()
                }
                resultVods[video.sourceKey]!!.add(video)
                if (video.sourceKey !== lastSourceKey) { // 添加到最后面并记录最后一个key用于下次判断
                    lastSourceKey = addWordAdapterIfNeed(video.sourceKey)
                }
            }
            if (searchAdapter.data.size > 0) {
                searchAdapter.addData(data)
            } else {
                showSuccess()
                if (!isFilterMode) mGridView.visibility = View.VISIBLE
                searchAdapter.setNewData(data)
            }
        }
        val count = allRunCount.decrementAndGet()
        if (count <= 0) {
            if (searchAdapter.data.size <= 0) {
                showEmpty()
            }
            cancel()
        }
    }

    private fun cancel() {
        OkGo.getInstance().cancelTag("search")
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        try {
            if (searchExecutorService != null) {
                searchExecutorService!!.shutdownNow()
                searchExecutorService = null
                JsLoader.load()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
    override fun afterTextChanged(editable: Editable) {
        val text = editable.toString()
        if (TextUtils.isEmpty(text)) {
            mSearchSuggestionsDialog?.dismiss()
            hideHotAndHistorySearch(false)
        } else {
            getSuggest(text)
        }
    }
}