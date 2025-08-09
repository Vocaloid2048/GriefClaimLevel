/*
 * Created by Voc-夜芷冰 (Vocaloid2048)
 * Copyright © 2025 . All rights reserved.
 */

package com.voc.griefclaimlevel.commands

import com.voc.griefclaimlevel.GriefClaimLevel
import com.voc.griefclaimlevel.sendMessageError
import com.voc.griefclaimlevel.sendMessageSuccess
import com.voc.griefclaimlevel.sendMessageWarning
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ReloadCommand(private val plugin: GriefClaimLevel) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessageError("${plugin.gclTranslation["reload-no-permission"]}", true)
            return true
        }else{
            sender.sendMessageWarning("${plugin.gclTranslation["reload-start"]}", true)

            if(plugin.configInit()){
                sender.sendMessageSuccess("${plugin.gclTranslation["reload-success"]}", true)
            } else {
                sender.sendMessageError("${plugin.gclTranslation["reload-failed"]}", true)
            }
        }



        return true
    }

}