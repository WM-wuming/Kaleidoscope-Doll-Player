# 森罗物语：玩偶·玩家 (Kaleidoscope Doll Player)

这是 [Li Suchen 的 Kaleidoscope Doll Workshop](https://github.com/lisuchen118/Kaleidoscope-Doll-Workshop/tree/1.20.1-fabric) 的 **Minecraft Fabric 非官方移植与扩展**，基于原项目的 [`086c300`](https://github.com/lisuchen118/Kaleidoscope-Doll-Workshop/commit/086c300b8f17e46516c9da6bad3ba9274154a2b6) 提交。本项目需要 [森罗物语：玩偶 Fabric 移植版（Kaleidoscope Doll Refabricated）](https://github.com/WM-wuming/KaleidoscopeDoll-Refabricated-Fabric) 作为前置，不是前置模组本体。

模组 ID：`kaleidoscope_doll_player`。旧版 `kaleidoscope_doll_workshop` 的物品和方块不会自动迁移，升级前请备份存档。

## 移植版新增功能

- **复用前置电脑**：不再使用旧版独立的工坊电脑。把羊毛放入前置模组的电脑（`kaleidoscope_doll:computer`），输入玩家名后点击「✔」生成一只；留空用自己的皮肤，Shift + 点击可用完当前槽里的羊毛。
- **皮肤 PNG 直链**：也可输入直接返回 `64×64` 或 `64×32` PNG 的 HTTP(S) 地址，不支持跳转。使用经典手臂，图片随玩偶保存，之后不再依赖原链接。
- **LittleSkin 回退**：优先查询 Minecraft 正版账户；只有正版接口确认玩家名不存在时才查 LittleSkin。LittleSkin 玩偶使用经典手臂。
- **皮肤更新与定格**：物品栏和已摆放的未定格玩家名玩偶会定期检查皮肤更新。一张纸和一枚紫水晶碎片可无序合成「玩偶定格片」。手持定格片右键已摆放的玩偶，或在物品栏用光标拿着定格片右键玩偶，即可保存当前皮肤；生存模式下只有保存成功才消耗道具。
- **保留皮肤的衍生玩偶**：玩家玩偶可与不死图腾、雪球、鸡蛋、末影珍珠或风弹合成，也可用黏液球做成可骑乘玩偶，或用铁砧装到头盔上。这些形态会保留玩家皮肤，包括已定格的皮肤。

按正版玩家名制作玩偶、把玩偶戴在头上，以及捏玩偶时的音效，均继承自原项目。

## 安装和构建

已验证构建和世界回归测试的 Minecraft Fabric 版本：

| Minecraft | Java |
| --- | --- |
| 26.3、26.2、26.1.2 | 25 |
| 1.21.11、1.21.10 | 21 |

请安装与游戏版本匹配的本扩展、Kaleidoscope Doll、Fabric Loader、Fabric API、GeckoLib 和 Forge Config API Port。多人游戏请在客户端和服务端都安装本扩展及前置。

构建时请使用 JDK 25 或以上，并准备好对应 Minecraft 版本的 Kaleidoscope Doll Fabric JAR（当前用 `1.4.2` 构建验证）。前置 JAR 不包含在此仓库中。`-PminecraftTarget` 默认为 `26.3`，用 `-PlocalDollDependency` 指定前置 JAR 路径：

```powershell
.\gradlew.bat build runGameTest "-PminecraftTarget=26.3" "-PlocalDollDependency=C:/path/to/kaleidoscopedoll-26.3-fabric-1.4.2-fabric+mc26.3.jar"
```

构建 `1.21.x` 时还需要 JDK 21 工具链；如需指定其路径，可设置 `DOLL_JDK21` 后传入 `-Porg.gradle.java.installations.fromEnv=DOLL_JDK21`。macOS / Linux 使用 `./gradlew`。默认版本的构建产物位于 `build/libs/`，其他版本的产物位于 `build/minecraft-<版本>/libs/`。

### GitHub Actions

推送到 `26.3-fabric` 或向该分支发起 PR 会自动运行 [Build Fabric mod](https://github.com/WM-wuming/Kaleidoscope-Doll-Player/actions/workflows/build.yml)，也可在 Actions 页面手动触发。

`26.3-fabric` 分支上的推送或手动构建通过整个版本矩阵后，会在 [Releases](https://github.com/WM-wuming/Kaleidoscope-Doll-Player/releases) 自动发布 `build-<提交短 SHA>` 预发布版，同一提交不重复发布。PR 构建仅提供 Actions **Artifacts**，不会创建 Release。每个版本提供一个扩展模组 JAR 和一个源码 JAR，不含前置 JAR。

CI 从公开的前置仓库拉取固定提交，逐版本构建前置，再构建扩展并运行世界测试，不需要额外的仓库密钥。

## 署名与许可

原项目作者为 **Li Suchen**。代码采用 **GPL-3.0-only**，详见 [LICENSE](LICENSE)。

原项目经 **药水棒冰（ysbb）** 许可使用了部分《森罗物语：玩偶》的贴图、模型和音效。这些素材的版权归原权利人所有，不适用 GPL；未经许可不得提取、再分发或用于其他项目。

## English

An unofficial Minecraft Fabric port of **Kaleidoscope Doll Workshop** by **Li Suchen**, based on the original `1.20.1-fabric` branch. It requires the separate **Kaleidoscope Doll Refabricated** mod for the same Minecraft version. Builds and world tests have passed for **26.3, 26.2, 26.1.2, 1.21.11, and 1.21.10**.

The port uses the base mod's computer instead of a separate Workshop Computer. Insert wool and enter a player name or a direct HTTP(S) skin PNG URL, then click **✔**; leave the field blank for your own skin, or Shift-click to use all the wool. Minecraft accounts take priority over LittleSkin. Direct URLs are stored with the doll and must return a `64×64` or `64×32` PNG without redirects.

Unlocked name-based dolls refresh periodically in inventories or as placed blocks. Craft a **Doll Snapshot** from paper and an amethyst shard to freeze a doll's skin. Functional, rideable and helmet-mounted variants also preserve the skin.

Old `kaleidoscope_doll_workshop` items and blocks do not migrate automatically. Back up your world before upgrading. The code is GPL-3.0-only; the borrowed assets remain all rights reserved by **ysbb**.
