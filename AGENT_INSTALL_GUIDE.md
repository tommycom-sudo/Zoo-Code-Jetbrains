# Zoo Code JetBrains 自定义版交付与安装指南

最后验证时间：2026-11-07
目标：让其他 Agent 在新电脑上安装或重建本次修改后的 Zoo Code JetBrains 插件。

## 1. 当前交付物

可直接安装的插件：

```text
C:\code\Zoo-Code-JetBrains\dist\ZooCode-3.94.0.zip
```

构建信息：

```text
Plugin ID: ZooCode
Plugin version: 3.94.0
Zoo Code extension version: 3.94.0
VS Code platform version: 1.100.0
File size: 115,504,771 bytes
SHA-256: C9E756C87AC7B59F7689B1B98E8AB97B375CCB0C60A45A18C933F202C8D660D4
```

新电脑如果只需要使用插件，不必重新编译，直接复制并安装该 ZIP 即可。

安装前可校验：

```powershell
Get-FileHash `
  -Algorithm SHA256 `
  "C:\path\to\ZooCode-3.94.0.zip"
```

哈希应为：

```text
C9E756C87AC7B59F7689B1B98E8AB97B375CCB0C60A45A18C933F202C8D660D4
```

## 2. 本次修改的功能

### 2.1 选区操作浮层

在编辑器中选中非空代码后，约 180 ms 后显示：

```text
Zoo Code
添加到对话
```

点击后，代码会进入当前 Zoo Code 对话输入框。

发送的上下文格式：

```text
文件绝对路径:起始行-结束行
```

随后附带选中代码。

该功能已经合并进完整 JetBrains 插件，不再需要单独的 `Zoo Code Selection` 伴生插件。

### 2.2 设置开关

设置位置：

```text
Settings > Tools > Zoo Code
```

开关：

```text
Show Zoo Code actions when text is selected
```

行为：

- 默认开启。
- 开启时，选区旁自动显示“添加到对话”。
- 关闭时，不自动显示选区浮层。
- 即使关闭自动浮层，仍可使用：
  ```text
  Tools > Add Selection to Zoo Code
  ```
- 设置持久化在 IDE 配置中，无需重启。

### 2.3 NodeSocket 日志降噪

修复正常关闭 Zoo Code 时出现的：

```text
Socket[extension-host] IO exception occurred while reading data
java.net.SocketException: Connection reset
```

正常关闭流程会先标记预期断开，再终止 Node extension host。预期断开的 `Connection reset` 只写 DEBUG 日志；运行期间真正异常断开的 socket 仍写 ERROR。

### 2.4 完整功能保留

本次没有替换 Zoo Code 的配置、模型、历史、MCP 和 Webview。

安装后仍应具备：

- 模型和服务商配置
- 模式和自定义模式
- 任务历史
- MCP
- 工具审批
- 完整聊天 Webview

### 2.5 聊天链接定位到代码行（2026-11-07 补充）

支持点击聊天中的 `[方法名](相对路径/File.java:118)` 后打开文件、将光标定位到第 118 行并居中滚动。文件首次打开与已经打开时均应用目标位置；旧链接的行号或列号超出文件范围时，会收敛到文件或行尾。

此次修复在 JetBrains 编辑器适配层处理 `showTextDocument` 的 `selection`，并实现后续设置选区的 RPC；无需修改 Agent 生成链接的格式。同时修复普通编辑器的范围滚动及协议坐标转换（协议行列从 1 开始，IDEA 索引从 0 开始）。

本次新增安装包为 `dist/ZooCode-3.94.0-line-navigation.zip`，与文档开头记录的原始 ZIP 分开保存；开头的原始文件大小和哈希不适用于本包。安装后需要重启 IDEA 才会加载新的插件代码。

验证命令（在 `jetbrains_plugin` 目录执行，先设置有效的 JDK 17）：

```powershell
.\gradlew.bat test --tests '*EditorNavigationTest' --tests '*EditorFocusIntentTest' --offline
```

手工验收：点击同一文件中的两个不同行号链接，分别在文件关闭和已经打开的情况下确认光标及视口定位；不带行号的文件链接应继续正常打开。

## 3. 新电脑直接安装

### 3.1 前置条件

- IntelliJ IDEA 2023.3 或更新版本
- 支持 JCEF 的 IDE runtime
- 不需要 JDK、Node.js、Visual Studio，前提是直接安装预编译 ZIP

### 3.2 安装步骤

1. 将 `ZooCode-3.94.0.zip` 复制到新电脑。
2. 打开：
   ```text
   Settings > Plugins
   ```
3. 点击齿轮按钮。
4. 选择：
   ```text
   Install Plugin from Disk...
   ```
5. 选择 `ZooCode-3.94.0.zip`。
6. 按提示重启 IDE。
7. 确认插件列表只有完整的 `Zoo Code`。

如果新电脑还安装了旧版 `Zoo Code Selection`，请卸载它，避免出现两个选区浮层。

### 3.3 验收

确认完整聊天窗口可用：

- 设置入口存在
- 模型选择存在
- 历史记录存在
- MCP 入口存在
- 聊天输入框可发送消息

确认选区功能：

1. 打开任意真实文件。
2. 选中一段非空代码。
3. 确认出现“添加到对话”。
4. 点击后确认代码进入 Zoo Code 输入框。

确认设置开关：

1. 打开 `Settings > Tools > Zoo Code`。
2. 关闭选区浮层。
3. 重新选中代码，确认不再自动显示。
4. 重新开启，确认功能恢复。

## 4. 从源码重建

### 4.1 相关仓库

本修改位于完整 JetBrains 插件仓库：

```text
Zoo-Code-JetBrains
```

Zoo Code Agent 本体位于：

```text
Zoo-Code
```

两者关系：

```text
Zoo-Code
  └─ Zoo Code VS Code 扩展和 Agent

Zoo-Code-JetBrains
  ├─ JetBrains IDE 插件
  ├─ extension_host
  ├─ JCEF Webview
  └─ 打包后的 Zoo Code Agent
```

本次 JetBrains 修改尚未提交到上游。新电脑不能只克隆官方 `main` 就得到这些改动，必须使用：

- 已提交的自定义分支，或
- 包含当前工作区修改的源码副本，或
- 已生成的 `ZooCode-3.94.0.zip`

### 4.2 必需工具

- Git
- Git Bash
- JDK 17
- Node.js 20.19.2
- npm
- curl
- unzip

推荐直接使用插件管理的 Node 20：

```text
C:\Users\<user>\AppData\Local\JetBrains\IntelliJIdea<version>\zoocode-node\node-v20.19.2-win-x64
```

### 4.3 两种构建方式

#### 方式 A：复用已安装插件的预编译环境

适用于没有 Visual Studio C++ Build Tools 的 Windows 电脑。

前提：

- 新电脑已经安装并启用过官方 ZooCode 3.94.0 插件。
- 安装目录中存在：
  ```text
  plugins/ZooCode/node_modules
  plugins/ZooCode/runtime
  ```

命令：

```powershell
$repo = "C:\code\Zoo-Code-JetBrains"
$node20 = "C:\Users\<user>\AppData\Local\JetBrains\IntelliJIdea<version>\zoocode-node\node-v20.19.2-win-x64"
$jdk17 = "C:\path\to\jdk-17"

$env:PATH = "$node20;$env:PATH"
$env:JAVA_HOME = $jdk17
$env:ZOO_EXTENSION_VERSION = "3.94.0"

cd $repo

& "C:\Program Files\Git\bin\bash.exe" -lc `
  "./scripts/build.sh --reuse-installed-plugin --mode release"
```

该模式：

- 复用预编译 `node_modules`
- 复用 `runtime/extension.js`
- 跳过 `node-gyp`
- 跳过 `npm install`
- 跳过 Extension Host TypeScript 编译
- 不要求 `deps/vscode` 子模块

注意：该模式使用已安装插件的 Extension Host runtime，因此：

- 不要用它构建 `extension_host/` 中刚修改的 JavaScript。
- 必须让 `ZOO_EXTENSION_VERSION` 与已安装插件版本一致。

#### 方式 B：正常完整编译

适用于修改了 `extension_host/`，或需要从零构建的环境。

需要：

- Visual Studio 2022 C++ Build Tools
- MSVC v143 x64/x86 build tools
- Windows 11 SDK
- Python 3.12
- `deps/vscode` 子模块

配置：

```powershell
npm config set python "C:\Python312\python.exe"
npm config set msvs_version 2022
```

初始化子模块：

```powershell
cd C:\code\Zoo-Code-JetBrains
git submodule update --init --recursive
```

构建：

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:ZOO_EXTENSION_VERSION = "3.94.0"

& "C:\Program Files\Git\bin\bash.exe" -lc `
  "./scripts/build.sh --mode release"
```

### 4.4 构建产物

```text
C:\code\Zoo-Code-JetBrains\dist\ZooCode-3.94.0.zip
```

中间产物：

```text
jetbrains_plugin/build/distributions/
```

下载缓存：

```text
.zoo-build/
jetbrains_plugin/build/genPlatform/
```

## 5. 平台包下载地址

`genPlatform` 需要四个平台的 VS Code 运行时包。

目标目录：

```text
C:\code\Zoo-Code-JetBrains\jetbrains_plugin\build\genPlatform
```

| 保存文件名         | 地址                                                                    |
| ------------------ | ----------------------------------------------------------------------- |
| `windows-x64.zip`  | `https://update.code.visualstudio.com/1.100.0/win32-x64-archive/stable` |
| `darwin-x64.zip`   | `https://update.code.visualstudio.com/1.100.0/darwin/stable`            |
| `darwin-arm64.zip` | `https://update.code.visualstudio.com/1.100.0/darwin-arm64/stable`      |
| `linux-x64.zip`    | `https://update.code.visualstudio.com/1.100.0/linux-x64/stable`         |

注意：Linux 下载内容是 tar.gz，但构建要求文件名保持为 `linux-x64.zip`。

下载脚本：

```powershell
$dir = "C:\code\Zoo-Code-JetBrains\jetbrains_plugin\build\genPlatform"
New-Item -ItemType Directory -Force -Path $dir | Out-Null

$downloads = @(
    @{
        Name = "windows-x64.zip"
        Url  = "https://update.code.visualstudio.com/1.100.0/win32-x64-archive/stable"
    },
    @{
        Name = "darwin-x64.zip"
        Url  = "https://update.code.visualstudio.com/1.100.0/darwin/stable"
    },
    @{
        Name = "darwin-arm64.zip"
        Url  = "https://update.code.visualstudio.com/1.100.0/darwin-arm64/stable"
    },
    @{
        Name = "linux-x64.zip"
        Url  = "https://update.code.visualstudio.com/1.100.0/linux-x64/stable"
    }
)

foreach ($item in $downloads) {
    $target = Join-Path $dir $item.Name
    $partial = "$target.part"

    Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue

    & curl.exe `
        -fL `
        --retry 5 `
        --retry-delay 3 `
        --connect-timeout 30 `
        --max-time 3600 `
        -o $partial `
        $item.Url

    if ($LASTEXITCODE -ne 0) {
        throw "下载失败: $($item.Url)"
    }

    Move-Item -LiteralPath $partial -Destination $target -Force
}
```

构建脚本会自动验证并删除损坏缓存。

## 6. build.sh 的预编译复用模式

### 自动复用已安装插件

```bash
./scripts/build.sh --reuse-installed-plugin
```

自动查找：

```text
plugins/ZooCode/node_modules
plugins/ZooCode/runtime
```

支持 Windows、macOS 和 Linux 常见 JetBrains 插件目录。

### 指定预编译依赖目录

```bash
./scripts/build.sh \
  --reuse-node-modules "/path/to/ZooCode/node_modules"
```

也可使用环境变量：

```bash
ZOO_REUSE_NODE_MODULES="/path/to/ZooCode/node_modules" \
  ./scripts/build.sh
```

### 两种模式的区别

| 模式                       | 需要 VS C++ | 需要 deps/vscode | 重新编译 Host TS |
| -------------------------- | ----------- | ---------------- | ---------------- |
| `--reuse-installed-plugin` | 否          | 否               | 否               |
| `--reuse-node-modules`     | 否          | 是               | 是               |
| 普通完整构建               | 是          | 是               | 是               |

## 7. MCP 配置位置

### 全局 MCP

```text
C:\Users\<user>\.zoo-code\globalStorage\wecode-ai.zoo-code\settings\mcp_settings.json
```

当前默认内容：

```json
{
  "mcpServers": {}
}
```

### 项目级 MCP

```text
<project-root>\.roo\mcp.json
```

同名 Server 同时存在时，项目级配置优先。

示例：

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "C:/code"],
      "env": {},
      "disabled": false,
      "alwaysAllow": [],
      "timeout": 60
    }
  }
}
```

Zoo Code 会监听配置文件并自动重新加载。

## 8. 本次代码修改清单

### 选区功能

新增：

- `jetbrains_plugin/src/main/kotlin/org/zoocode/jetbrains/selection/ZooCodeSelectionPopupService.kt`
- `jetbrains_plugin/src/main/kotlin/org/zoocode/jetbrains/selection/SelectionPopupSettings.kt`
- `jetbrains_plugin/src/main/kotlin/org/zoocode/jetbrains/selection/SelectionPopupConfigurable.kt`
- `jetbrains_plugin/src/main/kotlin/org/zoocode/jetbrains/selection/AddSelectionToContextAction.kt`

修改：

- `ZooCodeContextMenuProvider.kt`
- `WecoderPlugin.kt`
- `plugin.xml`

### NodeSocket 关闭处理

修改：

- `NodeSocket.kt`
- `ExtensionHostManager.kt`
- `ExtensionSocketServer.kt`
- `ExtensionUnixDomainSocketServer.kt`
- `WecoderPlugin.kt`

### 构建系统

修改：

- `scripts/build.sh`
- `jetbrains_plugin/genPlatform.gradle`
- `README.md`
- `BUILD.md`
- `jetbrains_plugin/gradle.properties`

### 测试

新增：

- `NodeSocketTest.kt`
- `SelectionPopupSettingsTest.kt`

验证结果：

```text
NodeSocketTest: 3/3 passed
SelectionPopupSettingsTest: 2/2 passed
Kotlin compilation: passed
Modified Kotlin source ktlint: passed
```

## 9. 常见问题

### `JAVA_HOME` 是无效目录

不要使用文档中的占位符：

```text
C:\path\to\jdk-17
```

必须替换为实际 JDK 17 路径，并验证：

```powershell
& "$env:JAVA_HOME\bin\java.exe" -version
```

### `node-gyp` 找不到 Visual Studio

使用：

```bash
./scripts/build.sh --reuse-installed-plugin
```

或安装 VS Build Tools 和 Python 3.12。

### `npm ls` 报 extraneous/missing/invalid

构建脚本已改为：

```bash
npm ls --package-lock-only --omit=dev --depth=10 --parseable
```

不要手动改回普通 `npm ls`。

### `gradle clean` 无法删除 `windows-x64.zip`

默认构建不再强制 clean。

只有传入 `--clean` 才会清理。清理失败时脚本会停止 Gradle daemon、等待并重试。

### 平台 ZIP 是 0 字节或损坏

构建脚本会自动删除损坏文件。新的 `genPlatform.gradle` 使用：

- curl
- 超时
- 重试
- `.part` 临时文件
- 完整下载后原子移动

### 出现 `Cannot resolve the latest Gradle IntelliJ Plugin version`

这是 Gradle IntelliJ Plugin 的在线自更新检查警告，通常不会阻止本地构建。只需继续观察后续任务是否成功。

### 同时出现两个“添加到对话”

卸载旧的：

```text
Zoo Code Selection
```

只保留：

```text
Zoo Code
```

### 模型目录出现 Vercel AI Gateway 报错

这是另一个上游兼容性问题：非语言模型没有 `context_window` 和 `max_tokens`。当前交付 ZIP 是否包含该修复，取决于打包时 Zoo Code 3.94.0 Agent 是否已经更新。该错误通常不影响现有模型使用。

## 10. 其他 Agent 执行清单

1. 优先复制并安装 `ZooCode-3.94.0.zip`。
2. 校验 SHA-256。
3. 卸载旧 `Zoo Code Selection`。
4. 重启 IDE。
5. 检查完整 Zoo Code 配置、模型和历史入口。
6. 验证选区浮层和“添加到对话”。
7. 验证 `Settings > Tools > Zoo Code` 开关。
8. 如果需要源码构建，按本文件第 4 节选择构建模式。
9. 不要在没有 `deps/vscode` 时修改并编译 `extension_host/`。
10. 不要混用不同 Zoo Code Agent 版本和预编译 Extension Host runtime。
