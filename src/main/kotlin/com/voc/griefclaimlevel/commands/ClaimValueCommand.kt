/*
 * Created by Voc-夜芷冰 (Vocaloid2048)
 * Copyright © 2025 . All rights reserved.
 */

package com.voc.griefclaimlevel.commands

import com.voc.griefclaimlevel.GriefClaimLevel
import com.voc.griefclaimlevel.GriefClaimLevel.ClaimBlockInfo
import com.voc.griefclaimlevel.getLocalizedBlockName
import com.voc.griefclaimlevel.placeholders
import com.voc.griefclaimlevel.sendMessageError
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

class ClaimValueCommand(private val plugin: GriefClaimLevel) : CommandExecutor {
    private val countYRange = Pair(
        (plugin.config["claim_height_land_below"]).toString().toIntOrNull() ?: -32,
        (plugin.config["claim_height_land_above"]).toString().toIntOrNull() ?: 64
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val needDetail = args.getOrNull(0)?.toBoolean() ?: false
        if (sender !is Player) {
            sender.sendMessageError("${plugin.gclTranslation["claimvalue-player-only"]}")
            return true
        }

        val claim = plugin.griefPrevention.dataStore.getClaimAt(sender.location, false, null)
        if (claim == null || claim.ownerID != sender.uniqueId) {
            sender.sendMessageError("${plugin.gclTranslation["claimvalue-must-in-your-claim"]}")
            return true
        }

        val (score, listOfBlocks) = calculateClaimScore(claim, needDetail)
        plugin.updateClaimScore(sender.uniqueId, score)
        sender.sendMessage("${ChatColor.GOLD}${plugin.gclTranslation["claimvalue-your-score"]}".placeholders(
            "(${claim.lesserBoundaryCorner.x},${claim.greaterBoundaryCorner.y + 6 - countYRange.first},${claim.lesserBoundaryCorner.z}) ~ (${claim.greaterBoundaryCorner.x},${claim.greaterBoundaryCorner.y + 6 + countYRange.second},${claim.lesserBoundaryCorner.z})",
            "${ChatColor.AQUA}$score"
        ))

        if (needDetail) {
            sender.sendMessage("${ChatColor.GOLD}${plugin.gclTranslation["claimvalue-detail"]}")
            // 定義欄位寬度
            val nameWidth = (listOfBlocks.keys.maxByOrNull { it.length } ?: "") .length // 名稱欄寬度
            val countWidth = 8  // 數量欄寬度
            val scoreWidth = 8 // 分數欄寬度
            val formatter = DecimalFormat(plugin.config["decimal-format"]?.toString() ?: "#,###.##")
            // 標題欄
            sender.sendMessage(
                "${ChatColor.GOLD}" +
                        "${plugin.config["claimvalue-detail-header-blockname"]}".padEnd(nameWidth) + " | " +
                        "${plugin.config["claimvalue-detail-header-blockcount"]}".padEnd(countWidth) + " | " +
                        "${plugin.config["claimvalue-detail-header-blockscore"]}".padEnd(scoreWidth)
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

    fun calculateClaimScore(claim: Claim, needDetail: Boolean = false): Pair<Double, MutableMap<String, ClaimBlockInfo>> {
        val world = claim.lesserBoundaryCorner.world ?: return Pair(0.0, mutableMapOf())
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
                    score += plugin.BLOCK_VALUES[blockId] ?: 0.0
                    if(needDetail){
                        val localizedName = getLocalizedBlockName(block.type)
                        listOfBlocks.compute(localizedName) { _, blockInfo -> (blockInfo ?: ClaimBlockInfo(0,0.0)) + ClaimBlockInfo(1,plugin.BLOCK_VALUES[blockId] ?: 0.0) }
                    }
                }
            }
        }

        return Pair(score,listOfBlocks)
    }

}

class ClaimValueTabCompleter(private val plugin: GriefClaimLevel) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("true", "false")
        }
        return emptyList()
    }
}