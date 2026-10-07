package com.warmup.manager.util

import com.warmup.manager.data.model.AccountEntity
import com.warmup.manager.data.model.Platform

object CsvHelper {
    fun exportAccountsToCsv(accounts: List<AccountEntity>): String {
        val sb = StringBuilder()
        sb.append("id,username,platform,nicheTag,notes,targetDailyMinutes,targetDays,isWarmedUp,isConnected\n")
        accounts.forEach { acc ->
            val escapedUsername = escapeCsv(acc.username)
            val escapedNiche = escapeCsv(acc.nicheTag)
            val escapedNotes = escapeCsv(acc.notes)
            sb.append("${acc.id},$escapedUsername,${acc.platform.name},$escapedNiche,$escapedNotes,${acc.targetDailyMinutes},${acc.targetDays},${acc.isExplicitlyWarmedUp},${acc.isConnected}\n")
        }
        return sb.toString()
    }

    fun parseAccountsFromCsv(csvContent: String): List<AccountEntity> {
        val accounts = mutableListOf<AccountEntity>()
        val lines = csvContent.lines()
        if (lines.isEmpty()) return accounts

        // Check if first line is header
        val dataLines = if (lines[0].contains("username", ignoreCase = true)) lines.drop(1) else lines

        for (line in dataLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val tokens = parseCsvLine(trimmed)
            if (tokens.size >= 3) {
                try {
                    val username = tokens.getOrNull(1)?.ifEmpty { tokens[0] } ?: continue
                    val platformStr = tokens.getOrNull(2) ?: "TIKTOK"
                    val nicheTag = tokens.getOrNull(3) ?: ""
                    val notes = tokens.getOrNull(4) ?: ""
                    val targetMinutes = tokens.getOrNull(5)?.toIntOrNull() ?: 30
                    val targetDays = tokens.getOrNull(6)?.toIntOrNull() ?: 5
                    val isWarmed = tokens.getOrNull(7)?.toBooleanStrictOrNull() ?: false
                    val isConnected = tokens.getOrNull(8)?.toBooleanStrictOrNull() ?: false

                    accounts.add(
                        AccountEntity(
                            id = 0, // auto-generate
                            username = username.removePrefix("@").let { "@$it" },
                            platform = Platform.fromString(platformStr),
                            nicheTag = nicheTag,
                            notes = notes,
                            targetDailyMinutes = targetMinutes,
                            targetDays = targetDays,
                            isExplicitlyWarmedUp = isWarmed,
                            isConnected = isConnected
                        )
                    )
                } catch (_: Exception) {
                    // skip malformed line
                }
            }
        }
        return accounts
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (insideQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    insideQuotes = !insideQuotes
                }
            } else if (c == ',' && !insideQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
