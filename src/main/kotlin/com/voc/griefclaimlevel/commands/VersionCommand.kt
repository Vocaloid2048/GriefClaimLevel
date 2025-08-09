/*
 * Created by Voc-夜芷冰 (Vocaloid2048)
 * Copyright © 2025 . All rights reserved.
 */

package com.voc.griefclaimlevel.commands

import com.voc.griefclaimlevel.GriefClaimLevel
import com.voc.griefclaimlevel.placeholders
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.io.File

class VersionCommand(
    private val plugin: GriefClaimLevel
) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        sender.sendMessage("${ChatColor.GREEN}${plugin.gclTranslation["version-plugin-author"]}")
        sender.sendMessage("${ChatColor.AQUA}${plugin.gclTranslation["version-plugin-version"]}".placeholders(
            plugin.description.version
        ))
        return true
    }
}