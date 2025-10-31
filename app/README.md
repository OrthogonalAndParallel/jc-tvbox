
**目录**
```
src
└── main
    ├── assets - 依赖资源，包含第三方库的jar包
    ├── java - 项目源代码
    └── res
        ├── anim - 动画资源，包含页面切换动画（如fade_in.xml、slide_in_bottom.xml等）
        ├── drawable - 图形资源，包含大量XML定义的UI元素
        ├── drawable-xhdpi
        ├── drawable-xxhdpi
        ├── drawable-xxxhdpi
        ├── layout - UI布局文件
        ├── menu
        ├── raw
        ├── values - 资源值定义，包含主题、颜色、字符串等基础配置
        ├── values-night - 夜间模式专用资源，适配暗色主题
        ├── values-v29
        └── xml

```

# Java源码目录功能说明

## 1. 核心模块

### `com.github.catvod`
- **crawler**: 爬虫框架核心
    - [Spider.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/crawler/Spider.java): 爬虫基础接口，定义[homeContent](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/crawler/Spider.java#L20-L22)、[detailContent](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/crawler/Spider.java#L32-L34)、[searchContent](file:///Users/jinchen/Documents/github/jc-tvbox/tvbox/network/xianyuyimu/一木源/JSON/娱乐包/爱央视.py#L161-L165)等关键方法
    - [JarLoader.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/crawler/JarLoader.java)/`JsLoader.java`: 负责加载JAR或JS扩展的爬虫引擎
    - [SpiderNull.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/crawler/SpiderNull.java): 空实现爬虫，用于异常处理

- **net**: 网络层封装
    - [OkHttp.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/net/OkHttp.java): OkHttp客户端封装
    - [OkhttpInterceptor.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/net/OkhttpInterceptor.java): 自定义网络拦截器
    - [SSLCompat.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/net/SSLCompat.java): SSL/TLS兼容性处理

- **utils**: 工具类
    - [Path.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/utils/Path.java): 文件路径管理工具，提供[root()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/utils/Path.java#L35-L37)、[cache()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/utils/Path.java#L39-L41)等方法获取标准目录
    - [Util.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/utils/Util.java): 通用工具方法集合

- **Init.java**: 应用初始化入口，提供[context()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/Init.java#L22-L24)获取全局上下文

### `com.github.tvbox.osc`
TVBox主应用逻辑模块，按功能组织：

#### 基础架构
- **base**: 基础UI组件
    - [App.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/base/App.java): 应用入口和全局配置
    - [BaseLazyFragment.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/base/BaseLazyFragment.java): 实现懒加载机制的Fragment基类，管理[isViewCreated](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/base/BaseLazyFragment.java#L42-L42)和[currentVisibleState](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/base/BaseLazyFragment.java#L47-L47)状态
    - [BaseVbActivity.kt](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/base/BaseVbActivity.kt)/`BaseVbFragment.kt`: ViewBinding封装基类

- **api**: 数据源配置
    - [ApiConfig.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java): 核心配置管理，处理[sourceBeanList](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java#L55-L55)和[parseBeanList](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java#L59-L59)，管理视频源和解析器配置

- **bean**: 数据模型
    - [VodInfo.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/bean/VodInfo.java): 视频信息容器，包含[seriesFlags](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/bean/VodInfo.java#L44-L44)和[seriesMap](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/bean/VodInfo.java#L48-L48)等关键属性
    - [SourceBean.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/bean/SourceBean.java): 视频源配置模型
    - [Subtitle.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/bean/Subtitle.java): 字幕数据模型

#### 数据管理
- **cache**: 本地缓存
    - [VodRecord.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/cache/VodRecord.java)/`VodCollect.java`: 视频记录和收藏数据模型
    - [CacheManager.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/cache/CacheManager.java): 缓存管理服务

- **data**: 数据库层
    - [AppDataBase.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/data/AppDataBase.java): Room数据库定义
    - [AppDataManager.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/data/AppDataManager.java): 数据库操作管理

- **event**: 事件总线
    - [HistoryStateEvent.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/event/HistoryStateEvent.java): 历史记录状态事件
    - [ServerEvent.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/event/ServerEvent.java): 服务器状态事件

#### 核心功能
- **player**: 播放器系统
    - [IjkMediaPlayer.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/player/IjkMediaPlayer.java): IJK播放器封装，支持缓存功能(`cache_file_path`)
    - [EXOmPlayer.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/player/EXOmPlayer.java): ExoPlayer封装
    - `controller`: 播放控制器（[VodController.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/player/controller/VodController.java)等）
    - `render`: 渲染相关（[SurfaceRenderView.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/player/render/SurfaceRenderView.java)）

- **subtitle**: 字幕处理引擎
    - [format](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/utils/Util.java#L120-L126): 字幕格式处理（[FormatASS.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/subtitle/format/FormatASS.java)、[FormatSRT.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/subtitle/format/FormatSRT.java)、[FormatSTL.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/subtitle/format/FormatSTL.java)）
    - `model`: 字幕数据模型（[TimedTextObject.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/subtitle/model/TimedTextObject.java)、[Subtitle.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/bean/Subtitle.java)）
    - [SubtitleLoader.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/subtitle/SubtitleLoader.java): 字幕加载和解析核心
    - [SubtitleEngine.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/subtitle/SubtitleEngine.java): 字幕渲染引擎

- **server**: 内置HTTP服务器
    - [RemoteServer.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/server/RemoteServer.java): 提供文件管理API（`/newFolder`、`/delFolder`、`/delFile`）
    - [ControlManager.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/server/ControlManager.java): 服务器控制管理

- **util**: 工具库
    - [FileUtils.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/FileUtils.java): 文件操作工具，提供[getFolderSize()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/FileUtils.java#L338-L354)、[getFormatSize()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/FileUtils.java#L362-L392)等方法
    - [Thunder.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/thunder/Thunder.java): 迅雷/磁力链解析（[isSupportUrl()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/thunder/Thunder.java#L374-L377)、[errorInfo()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/thunder/Thunder.java#L343-L371)）
    - [SubtitleHelper.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/SubtitleHelper.java): 字幕辅助工具
    - [HawkConfig.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/HawkConfig.java): 全局配置管理

#### UI层
- **ui**: 用户界面
    - `activity`: 各类Activity（`MainActivity.java`、`HistoryActivity.java`）
    - `fragment`: UI片段（[HomeFragment.kt](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/ui/fragment/HomeFragment.kt)、[GridFragment.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/ui/fragment/GridFragment.java)）
    - `dialog`: 对话框组件（[ApiHistoryDialog.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/ui/dialog/ApiHistoryDialog.java)、[LastViewedDialog.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/ui/dialog/LastViewedDialog.java)）
    - [adapter](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/ui/dialog/CastListDialog.java#L29-L29): 列表适配器

- **viewmodel**: MVVM架构
    - [SourceViewModel.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/viewmodel/SourceViewModel.java): 视频源数据管理
    - [SubtitleViewModel.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/viewmodel/SubtitleViewModel.java): 字幕搜索和加载逻辑

## 2. 辅助模块

### `com.p2p`
- [P2PClass.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/p2p/P2PClass.java): P2P网络传输功能实现

### `okhttp3.dnsoverhttps`
- DNS over HTTPS实现
    - [DnsOverHttps.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/okhttp3/dnsoverhttps/DnsOverHttps.java): 安全DNS查询核心实现
    - [BootstrapDns.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/okhttp3/dnsoverhttps/BootstrapDns.java): 引导DNS配置

## 3. 功能亮点

1. **多源视频支持**：通过[ApiConfig.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java)管理多个视频源，每个源有独立的解析规则

2. **字幕系统**：完整的字幕处理流程，支持ASS/SRT/STL等多种格式，通过[TimedTextObject](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/subtitle/model/TimedTextObject.java#L39-L167)统一模型处理

3. **内置文件服务**：[RemoteServer.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/server/RemoteServer.java)提供完整的文件管理API，支持创建/删除文件夹和文件操作

4. **懒加载架构**：[BaseLazyFragment.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/base/BaseLazyFragment.java)实现智能的Fragment可见性管理，优化资源使用

5. **特殊链接解析**：[Thunder.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/thunder/Thunder.java)处理迅雷、磁力链等特殊链接，[isSupportUrl()](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/thunder/Thunder.java#L374-L377)方法识别支持的链接类型

6. **灵活的爬虫框架**：[Spider.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/catvod/crawler/Spider.java)定义统一接口，支持Jar/JS扩展的爬虫引擎

7. **缓存优化**：[IjkMediaPlayer.java](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/player/IjkMediaPlayer.java)实现智能缓存，支持`cache_file_path`和`cache_map_path`配置

8. **历史记录管理**：通过[VodRecord](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/cache/VodRecord.java#L13-L32)和[HawkConfig](file:///Users/jinchen/Documents/github/jc-tvbox/app/src/main/java/com/github/tvbox/osc/util/HawkConfig.java#L7-L66)实现完整的观看历史记录功能