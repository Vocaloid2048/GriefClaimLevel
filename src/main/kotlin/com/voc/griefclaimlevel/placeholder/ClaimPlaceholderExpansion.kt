package com.voc.griefclaimlevel.placeholder

import com.voc.griefclaimlevel.GriefClaimLevel
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

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