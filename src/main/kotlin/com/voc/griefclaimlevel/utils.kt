package com.voc.griefclaimlevel

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Logger
import kotlin.to

val GCFLoggerPrefix = "&b[GriefClaimLevel]&r"
val GCFLoggerPrefixS = "${ChatColor.AQUA}[GriefClaimLevel]${ChatColor.RESET}"

fun Logger.infoP(info: String) = info("$GCFLoggerPrefix &f$info")

fun Logger.warningP(warning: String) = warning("$GCFLoggerPrefix &e$warning")

fun Logger.severeP(severe: String) = severe("$GCFLoggerPrefix &c$severe")

fun String.placeholders(vararg placeholders: String): String {
    var result = this
    placeholders.forEachIndexed { index, value ->
        result = result.replace("$"+"{${index + 1}}", value)
    }
    return result
}

fun CommandSender.sendMessageWarning(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefixS " else ""}${ChatColor.YELLOW}$message")
}

fun CommandSender.sendMessageError(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefixS " else ""}${ChatColor.RED}$message")
}

fun CommandSender.sendMessageSuccess(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefixS " else ""}${ChatColor.GREEN}$message")
}

fun CommandSender.sendMessageInfo(message: String, prefixNeed : Boolean = false) {
    this.sendMessage("${if (prefixNeed) "$GCFLoggerPrefixS " else ""}${ChatColor.WHITE}$message")
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
        logger.severeP("CANNOT READ TRANSLATION FILE! Please check the file permissions.")
        server.pluginManager.disablePlugin(this)
        return emptyMap()
    }

    try {
        val translationConfig = YamlConfiguration.loadConfiguration(file)
        return translationConfig.getKeys(false).associateWith { (translationConfig.getString(it, "???") ?: "???") }
    } catch (e: Exception) {
        logger.severeP("Unable to load Translation file: ${e.message}")
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
            logger.warningP("${gclTranslation["gcl-blockconfig-empty"]}")
        }
        return blockValues
    } catch (e: Exception) {
        logger.severeP("${gclTranslation["gcl-unable-load-yml"]}".placeholders(fileName, e.message ?: "Unknown error"))
        return emptyMap()
    }
}


// Not useful ...
fun getLocalizedBlockName(material: Material): String {
    val component = material.translationKey().let { Component.translatable(it) }
    return LegacyComponentSerializer.legacySection().serialize(component)
}