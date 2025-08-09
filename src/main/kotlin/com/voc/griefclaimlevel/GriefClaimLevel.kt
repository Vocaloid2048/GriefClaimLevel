package com.voc.griefclaimlevel

import com.voc.griefclaimlevel.commands.ClaimLeaderboardCommand
import com.voc.griefclaimlevel.commands.ClaimValueCommand
import com.voc.griefclaimlevel.commands.ClaimValueTabCompleter
import com.voc.griefclaimlevel.commands.ReloadCommand
import com.voc.griefclaimlevel.commands.VersionCommand
import com.voc.griefclaimlevel.placeholder.ClaimPlaceholderExpansion
import me.ryanhamshire.GriefPrevention.Claim
import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

class GriefClaimLevel : JavaPlugin() {
    lateinit var griefPrevention: GriefPrevention
    internal lateinit var config: Map<String, Any?>
    internal lateinit var gclTranslation: Map<String, String>
    internal val BLOCK_VALUES: Map<String, Double> by lazy { loadBlockConfig() }
    private val leaderboard = ConcurrentHashMap<UUID, Double>()
    val claimScores = ConcurrentHashMap<UUID, Double>()

    override fun onEnable() {
        configInit()

        // Enabling GriefClaimLevel - Check whether GriefPrevention Exist
        logger.infoP("${gclTranslation["gcl-trying-init"]}")
        griefPrevention = server.pluginManager.getPlugin("GriefPrevention")?.let {
            it as GriefPrevention
        } ?: run {
            logger.severeP("${gclTranslation["gcl-not-found-griefPrevention"]}")
            server.pluginManager.disablePlugin(this)
            return
        }

        commandRegisting()

        registerPlaceholder()

        updateLeaderboard()
    }

    internal fun configInit() : Boolean{
        saveDefaultConfig()

        val file = File(dataFolder, "config.yml")
        try {
            val configFile = YamlConfiguration.loadConfiguration(file)
            config = configFile.getKeys(false).associateWith { (configFile.get(it, null)) }

        } catch (e: Exception) {
            logger.severeP("${ChatColor.RED}WHERE IS MY CONFIG FILE...??? : ${e.message}")
            server.pluginManager.disablePlugin(this)
            return false
        }

        gclTranslation = loadTranslationConfig(config.get("language")?.toString() ?: "zh_TW")
        return true
    }

    internal fun registerPlaceholder(){
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            // Register Placeholder
            ClaimPlaceholderExpansion(this).register()
            logger.infoP("${gclTranslation["gcl-placeholder-registered"]}")
        } else {
            logger.warningP("${gclTranslation["gcl-placeholderapi-not-found"]}")
        }
    }

    internal fun commandRegisting(){
        getCommand("claimvalue")?.setExecutor(ClaimValueCommand(this))
        getCommand("claimvalue")?.setTabCompleter(ClaimValueTabCompleter(this))
        getCommand("claimlb")?.setExecutor(ClaimLeaderboardCommand(this))
        getCommand("gcfversion")?.setExecutor(VersionCommand(this))
        getCommand("gcfreload")?.setExecutor(ReloadCommand(this))
    }


    data class ClaimBlockInfo(
        var count: Int,
        var score: Double,
    ) {
        operator fun plus(other: ClaimBlockInfo): ClaimBlockInfo {
            return ClaimBlockInfo(count + other.count, score + other.score)
        }
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