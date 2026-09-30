# CS2 掉落记录（Android / Kotlin）

CS2 每周掉落与收益支出记录的原生 Android 应用。UI 与交互延续 Web 版（`../web/`），数据格式与 Web 版备份完全互通。

## 技术栈

- Kotlin + Jetpack Compose (Material3)
- Coil（图片加载，Steam CDN 直链）
- OkHttp + kotlinx.serialization（拉取/解析物品目录）
- 本地 JSON 文件存储（`filesDir/data.json`），无数据库依赖

## 功能

- **首页**：距下期倒计时 / 总收益支出 + 仿手机日历（本期胶囊高亮、今天圆形）+ 各期打卡卡片（最新期在上，横排物品格子）
- **统计页**：累计总收益支出、各武器箱掉落次数、按类型/品质分布、各期收益（按年筛选）
- **搜索页**（新增）：
  - 物品：搜索武器箱/皮肤/涂鸦 → 查看该物品出现的所有期 → 点击跳转
  - 日期：选日期跳转到所在期
  - 期数：输入期号跳转；也可从「有记录的期」列表直接跳
- **添加/编辑**：期 + 期内日期两级选择（第 1 期起~今天）、物品搜索选择（带预览图）、快捷武器箱、数量、金额（**负数 = 支出**，如开箱花费）、备注
- **设置**：周期设置（第 1 期起始日 / 每期天数）、数据管理（JSON 备份导入导出、CSV 导出、清空）、物品目录管理

## 物品目录（ByMykel/CSGO-API）

- 数据源：[ByMykel/CSGO-API](https://github.com/ByMykel/CSGO-API)（zh-CN，约 4800 个物品：武器箱 / 皮肤 / 涂鸦）
- 图片为 Steam CDN 直链（`community.akamai.steamstatic.com`）
- 首次使用需下载约 16MB，之后缓存到本地（设置页可手动更新）

## 构建

用 **Android Studio**（推荐）打开本目录，等 Gradle 同步完成后直接运行：

- JDK 17
- Android SDK（compileSdk 35 / minSdk 26 / targetSdk 35）
- Android Studio 自带 gradle wrapper 首次会自动下载

命令行构建（已内置 Gradle Wrapper 9.5.0）：

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> 若提示找不到 SDK，在根目录创建 `local.properties`：
> `sdk.dir=C:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk`

## 数据兼容

- 导出/导入的 JSON 备份与 Web 版 `cs2-drops-backup.json` 格式一致（`settings` + `drops`）
- `drops` 字段：`id / name / date / qty / price / note / category / rarity`（`img`、`rcolor` 为 Android 新增可选字段）
- 导入按 `id` 去重合并

## 目录结构

```
app/src/main/java/com/cs2/tracker/
├── MainActivity.kt        # 底部导航 + FAB + 弹窗 + 跳转协调
├── data/
│   ├── Models.kt          # Drop / AppSettings / 备份 / 目录条目
│   ├── Period.kt          # 期计算（与 Web 版一致）
│   ├── Store.kt           # 数据存储 + 导入导出
│   └── CsgoApi.kt         # CSGO-API 客户端（多镜像源 + 缓存）
└── ui/
    ├── Theme.kt           # 深色 CS2 橙主题
    ├── Common.kt          # 物品图 / 期徽章 / 掉落格子
    ├── HomeScreen.kt      # 首页（统计条+日历+期卡片）
    ├── StatsScreen.kt     # 统计页
    ├── SearchScreen.kt    # 搜索页（物品/日期/期数）
    ├── AddDropSheet.kt    # 添加/编辑底部弹窗
    └── SettingsScreen.kt  # 设置页
```
