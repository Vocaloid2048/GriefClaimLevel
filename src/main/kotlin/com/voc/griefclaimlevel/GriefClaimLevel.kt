package com.voc.griefclaimlevel

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.ryanhamshire.GriefPrevention.Claim
import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.DecimalFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class GriefClaimLevel : JavaPlugin() {
    lateinit var griefPrevention: GriefPrevention
    private lateinit var blockValues: Map<String, Double>
    val claimScores = ConcurrentHashMap<UUID, Double>()
    private val leaderboard = ConcurrentHashMap<UUID, Double>()

    override fun onEnable() {
        logger.info("正在嘗試啟用 GriefClaimLevel")
        saveDefaultConfig()
        loadBlockConfig()
        griefPrevention = server.pluginManager.getPlugin("GriefPrevention")?.let {
            it as GriefPrevention
        } ?: run {
            logger.severe("GriefPrevention 未找到，插件將禁用！")
            server.pluginManager.disablePlugin(this)
            return
        }
        getCommand("claimvalue")?.setExecutor(ClaimValueCommand(this))
        getCommand("version")?.setExecutor(VersionCommand(this, description.version))
        getCommand("claimlb")?.setExecutor(ClaimLeaderboardCommand(this))
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            ClaimPlaceholderExpansion(this).register()
            logger.info("PlaceholderAPI 已註冊")
        } else {
            logger.warning("PlaceholderAPI 未找到，Placeholder 功能將不可用")
        }
        updateLeaderboard()
    }

    private fun loadBlockConfig() {
        val file = File(dataFolder, "blockconfig.yml")
        if (!file.exists()) {
            saveResource("blockconfig.yml", false)
        }
        try {
            val config = YamlConfiguration.loadConfiguration(file)
            blockValues = config.getConfigurationSection("blocks")?.getKeys(false)?.associate {
                it to config.getDouble("blocks.$it", 0.0)
            } ?: emptyMap()
            if (blockValues.isEmpty()) {
                logger.warning("blockconfig.yml 未包含有效的方塊配置，將使用空配置")
            }
        } catch (e: Exception) {
            logger.severe("無法載入 blockconfig.yml: ${e.message}")
            blockValues = emptyMap()
        }
    }

    data class ClaimBlockInfo(
        var count: Int,
        var score: Double,
    ) {
        operator fun plus(other: ClaimBlockInfo): ClaimBlockInfo {
            return ClaimBlockInfo(count + other.count, score + other.score)
        }
    }

    fun calculateClaimScore(claim: Claim, needDetail: Boolean = false): Pair<Double, MutableMap<String, ClaimBlockInfo>> {
        val world = claim.lesserBoundaryCorner.world ?: return Pair(0.0, mutableMapOf())
        //logger.severe { "${claim.lesserBoundaryCorner} | ${claim.greaterBoundaryCorner}" }
        // greaterBoundaryCorner.y.toInt() + 6 suppose is the flat that u declared
        val minY = max(world.minHeight, claim.greaterBoundaryCorner.y.toInt() + 6 - 24)
        val maxY = min(world.maxHeight, claim.greaterBoundaryCorner.y.toInt() + 6 + 64)
        var score = 0.0
        val listOfBlocks = mutableMapOf<String, ClaimBlockInfo>()

        for (x in claim.lesserBoundaryCorner.x.toInt()..claim.greaterBoundaryCorner.x.toInt()) {
            for (z in claim.lesserBoundaryCorner.z.toInt()..claim.greaterBoundaryCorner.z.toInt()) {
                for (y in minY..maxY) {
                    val block = world.getBlockAt(x, y, z)
                    val blockId = block.type.name.lowercase()
                    score += blockValues[blockId] ?: 0.0
                    if(needDetail){
                        val localizedName = getLocalizedBlockName(block.type, logger)
                        listOfBlocks.compute(localizedName) { _, blockInfo -> (blockInfo ?: ClaimBlockInfo(0,0.0)) + ClaimBlockInfo(1,blockValues[blockId] ?: 0.0) }
                    }
                }
            }
        }

        return Pair(score,listOfBlocks)
    }

    // Modify Language yourself
    fun getLocalizedBlockName(material: Material, logger: Logger): String {
        val component = material.translationKey().let { Component.translatable(it) }
        return LegacyComponentSerializer.legacySection().serialize(component)
    }


    fun updateClaimScore(playerId: UUID, score: Double) {
        claimScores[playerId] = score
        updateLeaderboard()
    }

    private fun updateLeaderboard() {
        leaderboard.clear()
        leaderboard.putAll(claimScores)
    }

    fun getPlayerRank(playerId: UUID): Int {
        val sorted = leaderboard.entries.sortedByDescending { it.value }
        return sorted.indexOfFirst { it.key == playerId } + 1
    }

    fun getLeaderboard(page: Int, perPage: Int = 10): List<Map.Entry<UUID, Double>> {
        val sorted = leaderboard.entries.sortedByDescending { it.value }
        val start = (page - 1) * perPage
        return sorted.subList(start.coerceAtLeast(0), minOf(start + perPage, sorted.size))
    }
}

class VersionCommand(
    private val plugin: GriefClaimLevel,
    private val pluginVersion: String
) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        sender.sendMessage("${ChatColor.GREEN}GriefClaimLevel 由 Voc-夜芷冰 製作，當前版本 ${ChatColor.YELLOW}$pluginVersion")
        return true
    }
}

class ClaimValueCommand(private val plugin: GriefClaimLevel) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val needDetail = args.getOrNull(0)?.toBoolean() ?: false
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}此指令僅限玩家使用！")
            return true
        }

        val claim = plugin.griefPrevention.dataStore.getClaimAt(sender.location, false, null)
        if (claim == null || claim.ownerID != sender.uniqueId) {
            sender.sendMessage("${ChatColor.RED}你必須在自己的領地內使用此指令！")
            return true
        }

        val (score, listOfBlocks) = plugin.calculateClaimScore(claim, needDetail)
        plugin.updateClaimScore(sender.uniqueId, score)
        sender.sendMessage("${ChatColor.GREEN}你的領地 [(${claim.lesserBoundaryCorner.x},${claim.greaterBoundaryCorner.y + 6 - 24},${claim.lesserBoundaryCorner.z}) ~ (${claim.greaterBoundaryCorner.x},${claim.greaterBoundaryCorner.y + 6 + 64},${claim.lesserBoundaryCorner.z})] 加權分數為：${ChatColor.YELLOW}$score")
        if (needDetail) {
            sender.sendMessage("${ChatColor.GOLD}========= 分數明細 ==========")
            // 定義欄位寬度
            val nameWidth = (listOfBlocks.keys.maxByOrNull { it.length } ?: "") .length // 名稱欄寬度
            val countWidth = 8  // 數量欄寬度
            val scoreWidth = 8 // 分數欄寬度
            val formatter = DecimalFormat("#,###.##")
            // 標題欄
            sender.sendMessage(
                "${ChatColor.GOLD}" +
                        "名稱".padEnd(nameWidth) + " | " +
                        "數量".padEnd(countWidth) + " | " +
                        "分數".padEnd(scoreWidth)
            )
            sender.sendMessage("${ChatColor.GRAY}${ "-".repeat(nameWidth + countWidth + scoreWidth + 4)}")
            // 按分數排序並顯示
            listOfBlocks.entries
                .sortedByDescending { it.value.score }
                .filter { (_, value) -> value.score != 0.0 }
                .forEach { (name, info) ->
                    val paddedName = name.padEnd(nameWidth)
                    val paddedCount = formatter.format( info.count).padStart(countWidth)
                    val paddedScore = formatter.format(info.score).padStart(scoreWidth)
                    sender.sendMessage(
                        "${ChatColor.YELLOW}$paddedName${ChatColor.RESET} | " +
                                "${ChatColor.GREEN}$paddedCount${ChatColor.RESET} | " +
                                "${ChatColor.AQUA}$paddedScore"
                    )
                }
        }
        return true
    }
}

class ClaimLeaderboardCommand(private val plugin: GriefClaimLevel) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val page = args.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val entries = plugin.getLeaderboard(page)
        if (entries.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}排行榜目前為空！")
            return true
        }

        sender.sendMessage("${ChatColor.GOLD}=== 領地分數排行榜 (第 $page 頁) ===")
        entries.forEachIndexed { index, entry ->
            val playerName = plugin.server.getOfflinePlayer(entry.key).name ?: "未知玩家"
            sender.sendMessage("${ChatColor.YELLOW}${index + 1 + (page - 1) * 10}. ${ChatColor.WHITE}$playerName: ${ChatColor.GREEN}${entry.value}")
        }
        return true
    }
}

class ClaimPlaceholderExpansion(private val plugin: GriefClaimLevel) : PlaceholderExpansion() {
    override fun getIdentifier(): String = "claimvalue"
    override fun getAuthor(): String = "Voc-夜芷冰"
    override fun getVersion(): String = plugin.description.version

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        return when (params) {
            "claim_score" -> plugin.claimScores[player.uniqueId]?.toString() ?: "0.0"
            "claim_leaderboard_rank" -> plugin.getPlayerRank(player.uniqueId).toString()
            else -> null
        }
    }
}