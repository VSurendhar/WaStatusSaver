package com.voidDeveloper.wastatussaver.domain.model

enum class AutoSaveIntervalDomain(val hours: Int, val label: String) {
    ONE_HOUR(1, "1 Hour"),
    SIX_HOURS(6, "6 Hours"),
    TWELVE_HOURS(12, "12 Hours"),
    TWENTY_FOUR_HOURS(24, "24 Hours")
}
