package com.voc.griefclaimlevel.commands

import com.voc.griefclaimlevel.GriefClaimLevel
import com.voc.griefclaimlevel.GriefClaimLevel.ClaimBlockInfo
import com.voc.griefclaimlevel.getLocalizedBlockName
import com.voc.griefclaimlevel.loadTranslationConfig
import com.voc.griefclaimlevel.placeholders
import com.voc.griefclaimlevel.sendMessageError
import com.voc.griefclaimlevel.sendMessageSuccess
import com.voc.griefclaimlevel.sendMessageWarning
import me.ryanhamshire.GriefPrevention.Claim
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.DecimalFormat
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.max
import kotlin.math.min

class ReloadCommand(private val plugin: GriefClaimLevel) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessageError("${plugin.gclTranslation["reload-no-permission"]}", true)
            return true
        }else{
            sender.sendMessageWarning("${plugin.gclTranslation["reload-start"]}", true)
        }

        if(plugin.configInit()){
            sender.sendMessageSuccess("${plugin.gclTranslation["reload-success"]}", true)
        } else {
            sender.sendMessageError("${plugin.gclTranslation["reload-failed"]}", true)
        }

        return true
    }

}