package com.healthmd.data.health

/**
 * Fork-local (not in upstream): the Health Connect data origin (app package) whose activity data
 * should be used exclusively, so two writers recording the same movement are never mixed.
 *
 * `null` keeps the stock behaviour (Health Connect's own source priority). When set, activity
 * totals (steps, distance, calories, floors, elevation) and workout distance/calories/elevation
 * come only from this origin; sleep, heart and body data are unaffected.
 */
fun interface ActivityOriginPreference {
    suspend fun preferredOrigin(): String?

    companion object {
        val NONE = ActivityOriginPreference { null }
    }
}

/**
 * Same as [singleOrigin] but tries each origin in [preferredOrigins] in order (e.g. the user's
 * chosen origin, then the workout's own) before falling back to the origin with the largest total.
 */
internal fun <T> List<T>.singleOrigin(
    preferredOrigins: List<String>,
    originOf: (T) -> String,
    amountOf: (T) -> Double,
): List<T> {
    val byOrigin = groupBy(originOf)
    if (byOrigin.size <= 1) return this
    preferredOrigins.firstNotNullOfOrNull { byOrigin[it] }?.let { return it }
    return byOrigin.maxByOrNull { (_, records) -> records.sumOf(amountOf) }!!.value
}
