package org.scottishtecharmy.soundscape.utils

fun String.blankOrEmpty() = this.isBlank() || this.isEmpty()
fun String.nullIfEmpty(): String? = ifEmpty { null }

/**
 * fuzzyCompare is based on Damerau-Levenshtein distance. It return a score which is the ratio of
 * the distance to the length of the strings. However, it also allows for the search string to be
 * shorter than the haystack string and will give a slightly better score to strings that are
 * naturally the same length.
 */
fun String.fuzzyCompare(haystackString: String, needleCanBeShorter: Boolean): Double {
    val len1 = this.length
    var len2 = haystackString.length
    var sameSizeCost = 0.0
    if(needleCanBeShorter && (len2 > len1)) {
        // Only compare up to the size of the needle. This allows comparison of the start of strings
        // so that "Tesco" matches with "Tesco Express" and "Christine" matches with "Christine's on
        // the Green".
        len2 = len1
        // Give a better score to strings that are naturally the same length, i.e. searching for
        // "Westerton" should prioritize "Westerton" over "Westerton Vets". This cost is only
        // incurred when the haystack was originally longer than the needle.
        sameSizeCost = 0.01
    }

    // Create a DP table to store distances
    val dp = Array(len1 + 1) { IntArray(len2 + 1) }

    for (i in 0..len1) {
        for (j in 0..len2) {
            when {
                i == 0 -> dp[i][j] = j // Cost of deleting all chars from s2
                j == 0 -> dp[i][j] = i // Cost of inserting all chars from s1
                else -> {
                    // If characters are the same, cost is the same as the previous state
                    val cost = if (this[i - 1] == haystackString[j - 1]) 0 else 1

                    // Find the minimum cost from three possible operations:
                    val deletionCost = dp[i - 1][j] + 1       // Deletion
                    val insertionCost = dp[i][j - 1] + 1       // Insertion
                    val substitutionCost = dp[i - 1][j - 1] + cost // Substitution

                    dp[i][j] = minOf(deletionCost, insertionCost, substitutionCost)

                    // --- Damerau-Levenshtein Addition ---
                    // Check for transposition of adjacent characters
                    if (i > 1 && j > 1 &&
                        this[i - 1] == haystackString[j - 2] &&
                        this[i - 2] == haystackString[j - 1]
                    ) {
                        // If a transposition is found, compare its cost with the current minimum
                        val transpositionCost = dp[i - 2][j - 2] + 1
                        dp[i][j] = minOf(dp[i][j], transpositionCost)
                    }
                }
            }
        }
    }
    // The final value in the DP table is the Damerau-Levenshtein distance
    // Normalize the distance to a ratio. A lower ratio means a better match.
    val maxLen = maxOf(len1, len2)
    if (maxLen == 0)
        return 0.0

    return (dp[len1][len2] / maxLen.toDouble()) + sameSizeCost
}
