package com.voiddevelopers.wastatussaver.domain.model

enum class BackupInterval(val label: String) {
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), ONLY_WHEN_I_TAP("Only When I tap \"Backup\" "),
}