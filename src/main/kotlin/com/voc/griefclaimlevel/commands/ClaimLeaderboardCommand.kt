package com.voc.griefclaimlevel.commands

import com.voc.griefclaimlevel.GriefClaimLevel
import com.voc.griefclaimlevel.placeholders
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ClaimLeaderboardCommand(private val plugin: GriefClaimLevel) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val page = args.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val entries = plugin.getLeaderboard(page)
        if (entries.isEmpty()) {
            sender.sendMessage("${ChatColor.YELLOW}${plugin.gclTranslation["leaderboard-empty"]}")
            return true
        }

        sender.sendMessage("${ChatColor.GOLD}${plugin.gclTranslation["leaderboard-header"]}".placeholders("$page"))
        entries.forEachIndexed { index, entry ->
            val playerName = plugin.server.getOfflinePlayer(entry.key).name ?: "Unknown"
            sender.sendMessage("${ChatColor.YELLOW}${index + 1 + (page - 1) * 10}. ${ChatColor.WHITE}$playerName: ${ChatColor.GREEN}${entry.value}")
        }
        return true
    }
}
