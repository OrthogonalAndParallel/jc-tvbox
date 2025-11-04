## 项目说明

* 使用Google Compose Meterial Design 设计风格的开源TVBOX
* 去除java+xml，拥抱kotlin+compose

**目录**
```
jc-tv-app
├── app - 主应用核心
├── gradle
├── crash - 
├── player - 多媒体播放引擎
└── quickjs - JavaScript引擎
```

**Gradle任务**

* assemble: 这个任务通常用于构建所有项目输出。对于Android项目，这通常意味着构建所有变体（如debug和release）的APK（或AAB）文件。
* assembleAndroidTest: 构建用于运行单元测试和/或Android测试的APK。
* build: 这是一个聚合任务。执行它通常会执行项目的所有标准构建流程，包括编译、打包、运行测试、生成文档等。它是最常用的任务之一，因为它能确保整个项目处于最新和可交付的状态。
* buildDependents: 构建当前项目及其依赖的所有子项目。
* buildKotlinToolingMetadata: 用于构建Kotlin工具所需的元数据。
* buildNeeded: 构建当前项目及其所需的其他子项目（依赖当前项目的项目不需要构建）。
* bundle: 构建Android App Bundle (AAB)。这是Google Play推荐的上传格式。
* compileDebugAndroidTestSources / compileDebugSources / compileDebugUnitSources: 编译不同配置（Debug/Unit Test/Android Test）的源代码。
* compileReleaseSimonSources / compileReleaseSimonUnitTestSources / compileReleaseSources / compileReleaseUnitTestSource: 编译不同配置（Release/Release-Simon变体/Unit Test）的源代码。
* extractDebugAnnotations / extractReleaseAnnotations / extractReleaseSimonAnnotations: 提取不同构建类型（如Debug或Release）的注解信息，这可能用于生成API文档或提高构建效率。


## 使用说明

* jc.json 为汇总多仓库线路

## 开源声明
- 所有源均收集于互联网，仅供测试研究使用，不得商用；
- 本项目不存储任何的流媒体内容，所有的法律责任与后果应由使用者自行承担；
- 您可以Fork本项目，但引用本项目内容到其他仓库的情况，务必要遵守开源协议.