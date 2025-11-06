package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.WindowCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider

import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.Source
import com.github.tvbox.osc.bean.Subscription
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.Utils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.obsez.android.lib.filechooser.ChooserDialog
import com.orhanobut.hawk.Hawk
import java.util.function.Consumer

class SubscriptionActivity : BaseActivity() {

    private var mBeforeUrl = Hawk.get(HawkConfig.API_URL, "")
    private var mSelectedUrl = mBeforeUrl
    private var mBeforeUrls: MutableList<String> = Hawk.get(HawkConfig.API_URLS, ArrayList())
    private var mergedChanged: Boolean = false
    private var mSubscriptions: MutableList<Subscription> = Hawk.get(HawkConfig.SUBSCRIPTIONS, ArrayList())
    private var mSubscriptionAdapter = SubscriptionAdapter()
    private val mSources: MutableList<Source> = ArrayList()
    private val showChoose = mutableStateOf(false)
    private var chooseChecked: Boolean = false
    private val showDelete = mutableStateOf(false)
    private var deletePos: Int = -1
    private val showActions = mutableStateOf(false)
    private var actionPos: Int = -1
    private val showRename = mutableStateOf(false)
    private val renameText = mutableStateOf("")
    private val showPerm = mutableStateOf(false)
    private var permChecked: Boolean = false

    override fun init() {
        // 当前订阅取自 API_URL；复选框用于合并选择
        val savedMerged: MutableList<String> = Hawk.get(HawkConfig.API_URLS, ArrayList())
        if (savedMerged.isNotEmpty()) {
            for (i in mSubscriptions.indices) {
                val sub = mSubscriptions[i]
                sub.isChecked = savedMerged.contains(sub.url)
            }
        }
        mSubscriptionAdapter.setNewData(mSubscriptions)

        mSubscriptionAdapter.setOnItemChildClickListener { _: BaseQuickAdapter<*, *>?, view: View, position: Int ->
            if (view.id == R.id.iv_del) {
                if (mSubscriptions[position].url == mSelectedUrl) {
                    ToastUtils.showShort("不能删除当前使用的订阅")
                    return@setOnItemChildClickListener
                }
                deletePos = position
                showDelete.value = true
            } else if (view.id == R.id.cb) {
                val item = mSubscriptions[position]
                item.isChecked = !item.isChecked
                mSubscriptions[position] = item
                mSubscriptionAdapter.notifyItemChanged(position)
                // 实时保存合并仓
                val mergedUrls = ArrayList<String>()
                for (s in mSubscriptions) {
                    if (s.isChecked) mergedUrls.add(s.url)
                }
                val oldUrls: MutableList<String> = Hawk.get(HawkConfig.API_URLS, ArrayList())
                Hawk.put(HawkConfig.API_URLS, mergedUrls)
                if (oldUrls != mergedUrls) mergedChanged = true
            }
        }

        mSubscriptionAdapter.setOnItemClickListener { _: BaseQuickAdapter<*, *>?, _: View?, position: Int ->
            // 单击仅设置当前使用仓(API_URL)，不改变勾选（勾选用于多选合并）
            val subscription = mSubscriptions[position]
            mSelectedUrl = subscription.url
            ToastUtils.showShort("已设为当前订阅")
        }

        mSubscriptionAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { _: BaseQuickAdapter<*, *>?, _: View, position: Int ->
                actionPos = position
                showActions.value = true
                true
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
                    // Add Subscription dialog state & UI
                    val showAdd = remember { mutableStateOf(false) }
                    val addName = remember { mutableStateOf("") }
                    val addUrl = remember { mutableStateOf("") }
                    val addChecked = remember { mutableStateOf(false) }
                    val showTip = remember { mutableStateOf(false) }

                    if (showAdd.value) {
                        AlertDialog(
                            onDismissRequest = { showAdd.value = false },
                            title = { Text(text = "订阅: ${mSubscriptions.size + 1}") },
                            text = {
                                val scroll = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .sizeIn(maxHeight = 360.dp)
                                        .verticalScroll(scroll)
                                ) {
                                    TextField(
                                        value = addName.value,
                                        onValueChange = { addName.value = it },
                                        label = { Text("名称") },
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextField(
                                        value = addUrl.value,
                                        onValueChange = { addUrl.value = it },
                                        label = { Text("地址") },
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = addChecked.value, onCheckedChange = { addChecked.value = it })
                                        Text(text = "设为当前使用")
                                        Spacer(modifier = Modifier.weight(1f))
                                        TextButton(onClick = {
                                            showAdd.value = false
                                            permChecked = addChecked.value
                                            showPerm.value = true
                                        }) { Text("本地导入") }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val name = addName.value.trim()
                                    val url = addUrl.value.trim()
                                    if (name.isEmpty() || url.isEmpty()) return@TextButton
                                    for (item in mSubscriptions) {
                                        if (item.url == url) {
                                            ToastUtils.showLong("订阅地址与" + item.name + "相同")
                                            return@TextButton
                                        }
                                    }
                                    addSubscription(name, url, addChecked.value)
                                    mSubscriptionAdapter.setNewData(mSubscriptions)
                                    showAdd.value = false
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAdd.value = false }) { Text("取消") }
                            }
                        )
                    }

                    if (showPerm.value) {
                        AlertDialog(
                            onDismissRequest = { showPerm.value = false },
                            title = { Text("提示") },
                            text = { Text("这将访问您设备文件的读取权限") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showPerm.value = false
                                    XXPermissions.with(this@SubscriptionActivity)
                                        .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                                        .request(object : OnPermissionCallback {
                                            override fun onGranted(permissions: List<String>, all: Boolean) {
                                                if (all) {
                                                    pickFile(permChecked)
                                                } else {
                                                    ToastUtils.showLong("部分权限未正常授予,请授权")
                                                }
                                            }
                                            override fun onDenied(permissions: List<String>, never: Boolean) {
                                                if (never) {
                                                    ToastUtils.showLong("读写文件权限被永久拒绝，请手动授权")
                                                    XXPermissions.startPermissionActivity(this@SubscriptionActivity, permissions)
                                                } else {
                                                    ToastUtils.showShort("获取权限失败")
                                                    showPerm.value = true
                                                }
                                            }
                                        })
                                }) { Text("允许") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPerm.value = false }) { Text("取消") }
                            }
                        )
                    }

                    if (showDelete.value) {
                        AlertDialog(
                            onDismissRequest = { showDelete.value = false },
                            title = { Text(text = "删除订阅") },
                            text = { Text(text = "确定删除订阅吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val pos = deletePos
                                    if (pos in 0 until mSubscriptions.size) {
                                        mSubscriptions.removeAt(pos)
                                        mSubscriptionAdapter.notifyDataSetChanged()
                                    }
                                    showDelete.value = false
                                    deletePos = -1
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDelete.value = false }) { Text("取消") }
                            }
                        )
                    }

                    if (showTip.value) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showTip.value = false },
                            sheetState = sheetState
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Text(text = "订阅使用说明", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "在此页面可以管理你的订阅，支持新增网络地址或本地导入，长按条目可进行置顶、重命名与复制等操作。")
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showTip.value = false }) { Text("关闭") }
                                }
                            }
                        }
                    }

                    if (showActions.value && actionPos in 0 until mSubscriptions.size) {
                        val item = mSubscriptions[actionPos]
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showActions.value = false },
                            sheetState = sheetState
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                val actions = listOf(
                                    if (item.isTop) "取消置顶" else "置顶",
                                    "重命名",
                                    "复制地址"
                                )
                                LazyColumn {
                                    itemsIndexed(actions) { index, label ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 14.dp)
                                                .clickable {
                                                    when (index) {
                                                        0 -> {
                                                            item.isTop = !item.isTop
                                                            mSubscriptions[actionPos] = item
                                                            mSubscriptionAdapter.setNewData(mSubscriptions)
                                                            showActions.value = false
                                                        }
                                                        1 -> {
                                                            renameText.value = item.name
                                                            showActions.value = false
                                                            showRename.value = true
                                                        }
                                                        2 -> {
                                                            ClipboardUtils.copyText(item.url)
                                                            ToastUtils.showLong("已复制")
                                                            showActions.value = false
                                                        }
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        if (index < actions.lastIndex) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            HorizontalDivider(color = DividerDefaults.color)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showRename.value && actionPos in 0 until mSubscriptions.size) {
                        AlertDialog(
                            onDismissRequest = { showRename.value = false },
                            title = { Text("更改为") },
                            text = {
                                TextField(
                                    value = renameText.value,
                                    onValueChange = { renameText.value = it },
                                    placeholder = { Text("新的订阅名") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val text = renameText.value
                                    if (!TextUtils.isEmpty(text)) {
                                        val trimmed = text.trim()
                                        if (trimmed.length > 8) {
                                            ToastUtils.showShort("不要过长,不方便记忆")
                                        } else {
                                            mSubscriptions[actionPos].name = trimmed
                                            mSubscriptionAdapter.notifyItemChanged(actionPos)
                                            showRename.value = false
                                        }
                                    }
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRename.value = false }) { Text("取消") }
                            }
                        )
                    }

                    if (showChoose.value) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { showChoose.value = false },
                            sheetState = sheetState
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Text(text = "选择数据源", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyColumn {
                                    itemsIndexed(mSources) { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
                                                .clickable {
                                                    addSubscription(item.sourceName, item.sourceUrl, chooseChecked)
                                                    mSubscriptionAdapter.setNewData(mSubscriptions)
                                                    showChoose.value = false
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = item.sourceName, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        if (index < mSources.lastIndex) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            HorizontalDivider(color = DividerDefaults.color)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                modifier = Modifier.statusBarsPadding(),
                                colors = colors,
                                title = { Text(text = "订阅管理") },
                                actions = {
                                    IconButton(onClick = { showTip.value = true }) {
                                        Icon(Icons.Outlined.Info, contentDescription = "使用说明")
                                    }
                                    TextButton(onClick = {
                                        val mergedUrls = ArrayList<String>()
                                        for (s in mSubscriptions) {
                                            if (s.isChecked) mergedUrls.add(s.url)
                                        }
                                        val oldUrls: MutableList<String> = Hawk.get(HawkConfig.API_URLS, ArrayList())
                                        Hawk.put(HawkConfig.API_URLS, mergedUrls)
                                        if (oldUrls != mergedUrls) {
                                            mergedChanged = true
                                            // 立即应用修改，重启首页
                                            val intent = Intent(this@SubscriptionActivity, MainActivity::class.java)
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            startActivity(intent)
                                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                                        }
                                        ToastUtils.showShort("已保存合并仓选择")
                                    }) { Text("保存") }
                                    IconButton(onClick = {
                                        addName.value = "订阅: ${mSubscriptions.size + 1}"
                                        addUrl.value = ""
                                        addChecked.value = false
                                        showAdd.value = true
                                    }) {
                                        Icon(Icons.Filled.Add, contentDescription = "添加订阅")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    RecyclerView(ctx).apply {
                                        layoutManager = LinearLayoutManager(ctx)
                                        adapter = mSubscriptionAdapter
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // showUseTip removed; replaced by Compose ModalBottomSheet

    // showAddSubscription removed; replaced by Compose AlertDialog

    private fun showPermissionTipPopup(checked: Boolean) {
        XPopup.Builder(this@SubscriptionActivity)
            .isDarkTheme(Utils.isDarkTheme())
            .asConfirm("提示", "这将访问您设备文件的读取权限") {
                XXPermissions.with(this)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                pickFile(checked)
                            } else {
                                ToastUtils.showLong("部分权限未正常授予,请授权")
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                ToastUtils.showLong("读写文件权限被永久拒绝，请手动授权")
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(
                                    this@SubscriptionActivity,
                                    permissions
                                )
                            } else {
                                ToastUtils.showShort("获取权限失败")
                                showPermissionTipPopup(checked)
                            }
                        }
                    })
            }.show()
    }

    /**
     *
     * @param checked 与showPermissionTipPopup一样,只记录并传递选中状态
     */
    private fun pickFile(checked: Boolean) {
        ChooserDialog(this@SubscriptionActivity, R.style.FileChooser)
            .withFilter(false, false, "txt", "json")
            .withStartFile(
                if (TextUtils.isEmpty(Hawk.get("before_selected_path"))) "/storage/emulated/0/Download" else Hawk.get(
                    "before_selected_path"
                )
            )
            .withChosenListener(ChooserDialog.Result { _, pathFile ->
                Hawk.put("before_selected_path", pathFile.parent)
                val clanPath =
                    pathFile.absolutePath.replace("/storage/emulated/0", "clan://localhost")
                for (item in mSubscriptions) {
                    if (item.url == clanPath) {
                        ToastUtils.showLong("订阅地址与" + item.name + "相同")
                        return@Result
                    }
                }
                addSubscription(pathFile.name, clanPath, checked)
            })
            .build()
            .show()
    }

    private fun addSubscription(name: String, url: String, checked: Boolean) {
        if (url.startsWith("clan://")) {
            addSub2List(name, url, checked)
            mSubscriptionAdapter.setNewData(mSubscriptions)
        } else if (url.startsWith("http")) {
            showLoadingDialog()
            OkGo.get<String>(url)
                .tag("get_subscription")
                .execute(object : AbsCallback<String?>() {
                    override fun onSuccess(response: Response<String?>) {
                        dismissLoadingDialog()
                        try {
                            val json = JsonParser.parseString(response.body()).asJsonObject
                            // 多线路?
                            val urls = json["urls"]
                            // 多仓?
                            val storeHouse = json["storeHouse"]
                            if (urls != null && urls.isJsonArray) { // 多线路
                                if (checked) {
                                    ToastUtils.showLong("多条线路请主动选择")
                                }
                                val urlList = urls.asJsonArray
                                if (urlList != null && urlList.size() > 0 && urlList[0].isJsonObject
                                    && urlList[0].asJsonObject.has("url")
                                    && urlList[0].asJsonObject.has("name")
                                ) { //多线路格式
                                    for (i in 0 until urlList.size()) {
                                        val obj = urlList[i] as JsonObject
                                        val name = obj["name"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val url = obj["url"].asString.trim { it <= ' ' }
                                        mSubscriptions.add(Subscription(name, url))
                                    }
                                }
                            } else if (storeHouse != null && storeHouse.isJsonArray) { // 多仓
                                val storeHouseList = storeHouse.asJsonArray
                                if (storeHouseList != null && storeHouseList.size() > 0 && storeHouseList[0].isJsonObject
                                    && storeHouseList[0].asJsonObject.has("sourceName")
                                    && storeHouseList[0].asJsonObject.has("sourceUrl")
                                ) { //多仓格式
                                    mSources.clear()
                                    for (i in 0 until storeHouseList.size()) {
                                        val obj = storeHouseList[i] as JsonObject
                                        val name = obj["sourceName"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val url = obj["sourceUrl"].asString.trim { it <= ' ' }
                                        mSources.add(Source(name, url))
                                    }
                                    chooseChecked = checked
                                    showChoose.value = true
                                }
                            } else { // 单线路/其余
                                addSub2List(name, url, checked)
                            }
                        } catch (th: Throwable) {
                            addSub2List(name, url, checked)
                        }
                        mSubscriptionAdapter.setNewData(mSubscriptions)
                    }

                    @Throws(Throwable::class)
                    override fun convertResponse(response: okhttp3.Response): String {
                        return response.body()!!.string()
                    }

                    override fun onError(response: Response<String?>) {
                        super.onError(response)
                        dismissLoadingDialog()
                        ToastUtils.showLong("订阅失败,请检查地址或网络状态")
                    }
                })
        } else {
            ToastUtils.showShort("订阅格式不正确")
        }
    }

    /**
     * 仅当选中本地文件和添加的为单线路时,使用此订阅生效。多线路会直接解析全部并添加,多仓会展开并选择,最后也按多线路处理,直接添加
     * @param name
     * @param url
     * @param checkNewest
     */
    private fun addSub2List(name: String, url: String, checkNewest: Boolean) {
        if (checkNewest) { //选中最新的,清除以前的选中订阅
            for (subscription in mSubscriptions) {
                if (subscription.isChecked) {
                    subscription.setChecked(false)
                }
            }
            mSelectedUrl = url
            mSubscriptions.add(Subscription(name, url).setChecked(true))
        } else {
            mSubscriptions.add(Subscription(name, url).setChecked(false))
        }
    }

    override fun onPause() {
        super.onPause()
        // 更新缓存
        Hawk.put(HawkConfig.API_URL, mSelectedUrl)
        Hawk.put<List<Subscription>?>(HawkConfig.SUBSCRIPTIONS, mSubscriptions)
        // 保存多仓合并所选（使用复选框为合并选择）
        val mergedUrls = ArrayList<String>()
        for (s in mSubscriptions) {
            if (s.isChecked) mergedUrls.add(s.url)
        }
        Hawk.put(HawkConfig.API_URLS, mergedUrls)
    }

    override fun finish() {
        //切换了订阅地址
        if (!TextUtils.isEmpty(mSelectedUrl) && mBeforeUrl != mSelectedUrl) {
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        OkGo.getInstance().cancelTag("get_subscription")
    }
}