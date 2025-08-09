/*
 * Created by Voc-夜芷冰 (Vocaloid2048)
 * Copyright © 2025 . All rights reserved.
 */

package com.voc.griefclaimlevel

import com.voc.griefclaimlevel.commands.*
import com.voc.griefclaimlevel.placeholder.ClaimPlaceholderExpansion
import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
        serverSender.sendMessageInfo("${gclTranslation["gcl-trying-init"]}", true)
        griefPrevention = server.pluginManager.getPlugin("GriefPrevention")?.let {
            it as GriefPrevention
        } ?: run {
            serverSender.sendMessageError("${gclTranslation["gcl-not-found-griefPrevention"]}", true)
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
            serverSender.sendMessageError("WHERE IS MY CONFIG FILE...??? : ${e.message}",true)
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
            serverSender.sendMessageSuccess("${gclTranslation["gcl-placeholder-registered"]}", true)
        } else {
            serverSender.sendMessageWarning("${gclTranslation["gcl-placeholderapi-not-found"]}", true)
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