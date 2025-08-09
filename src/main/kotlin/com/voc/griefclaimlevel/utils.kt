/*
 * Created by Voc-夜芷冰 (Vocaloid2048)
 * Copyright © 2025 . All rights reserved.
 */

package com.voc.griefclaimlevel

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Logger
import kotlin.to

val GCFLoggerPrefix =
    "${ChatColor.DARK_GRAY}[Grief" +
        "${ChatColor.GOLD}Claim" +
        "${ChatColor.AQUA}Level]" +
        "${ChatColor.RESET}"
val serverSender = Bukkit.getServer().consoleSender

fun String.placeholders(vararg placeholders: String): String {
    var result = this
    placeholders.forEachIndexed { index, value ->
        result = result.replace("$"+"{${index + 1}}", value)
    }
    return result
}

fun CommandSender.sendMessageWarning(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefix " else ""}${ChatColor.YELLOW}$message")
}

fun CommandSender.sendMessageError(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefix " else ""}${ChatColor.RED}$message")
}

fun CommandSender.sendMessageSuccess(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefix " else ""}${ChatColor.GREEN}$message")
}

fun CommandSender.sendMessageInfo(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefix " else ""}${ChatColor.WHITE}$message")
}

fun GriefClaimLevel.loadTranslationConfig(langCode: String) : Map<String, String> {
    val file = File(dataFolder, "translation_${langCode}.yml")
    val fileEN = File(dataFolder, "translation_en_US.yml")

    if (!file.exists()) {
        saveResource("translation_${langCode}.yml", false)
    }
    if (!fileEN.exists()) {
        saveResource("translation_en_US.yml", false)
    }
    if (!file.canRead()) {
        serverSender.sendMessageError("CANNOT READ TRANSLATION FILE! Please check the file permissions.", true)
        server.pluginManager.disablePlugin(this)
        return emptyMap()
    }

    try {
        val translationConfig = YamlConfiguration.loadConfiguration(file)
        return translationConfig.getKeys(false).associateWith { (translationConfig.getString(it, "???") ?: "???") }
    } catch (e: Exception) {
        serverSender.sendMessageError("Unable to load Translation file: ${e.message}", true)
        server.pluginManager.disablePlugin(this)
        return emptyMap()
    }
}

internal fun GriefClaimLevel.loadBlockConfig() : Map<String, Double> {
    val fileName = "blockconfig"
    val file = File(dataFolder, "$fileName.yml")
    if (!file.exists()) {
        saveResource("$fileName.yml", false)
    }
    try {
        val blockConfig = YamlConfiguration.loadConfiguration(file)
        val blockValues = blockConfig.getConfigurationSection("blocks")?.getKeys(false)?.associate {
            it to blockConfig.getDouble("blocks.$it", 0.0)
        } ?: emptyMap()
        if (blockValues.isEmpty()) {
            serverSender.sendMessageError("${gclTranslation["gcl-blockconfig-empty"]}", true)
        }
        return blockValues
    } catch (e: Exception) {
        serverSender.sendMessageError("${gclTranslation["gcl-unable-load-yml"]}".placeholders(fileName, e.message ?: "Unknown error"), true)
        return emptyMap()
    }
}


// Not useful ...
fun getLocalizedBlockName(material: Material): String {
    val component = material.translationKey().let { Component.translatable(it) }
    return LegacyComponentSerializer.legacySection().serialize(component)
}