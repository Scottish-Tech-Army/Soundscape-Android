package org.scottishtecharmy.soundscape.utils

fun String.containsNumber(): Boolean {
    val words = split(" ")
    for (word in words) {
        if (word.isEmpty()) continue
        if (word.first().isDigit()) {
            // If any word starts with a number return true
            return true
        }
    }
    return false
}

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
    if (needleCanBeShorter && (len2 > len1)) {
        // Only compare up to the size of the needle. This allows comparison of the start of strings
        // so that "Tesco" matches with "Tesco Express" and "Christine" matches with "Christine's on
        // the Green".
        len2 = len1
        // Give a better score to strings that are naturally the same length, i.e. searching for
        // "Westerton" should prioritize "Westerton" over "Westerton Vets". This cost is only
        // incurred when the haystack was originally longer than the needle.
        sameSizeCost = 0.01
    }

    val maxLen = maxOf(len1, len2)
    if (maxLen == 0)
        return 0.0

    // The DP table of distances, row by row. Only the last three rows are ever looked at - the
    // transposition looks two back - so those are all that's kept, rather than allocating the
    // whole table for every one of the many comparisons a search makes.
    var twoRowsBack = IntArray(len2 + 1)
    var previousRow = IntArray(len2 + 1) { j -> j } // Cost of deleting all chars from s2
    var currentRow = IntArray(len2 + 1)

    for (i in 1..len1) {
        currentRow[0] = i // Cost of inserting all chars from s1
        for (j in 1..len2) {
            // If characters are the same, cost is the same as the previous state
            val cost = if (this[i - 1] == haystackString[j - 1]) 0 else 1

            // Find the minimum cost from three possible operations:
            val deletionCost = previousRow[j] + 1               // Deletion
            val insertionCost = currentRow[j - 1] + 1           // Insertion
            val substitutionCost = previousRow[j - 1] + cost    // Substitution
            var distance = minOf(deletionCost, insertionCost, substitutionCost)

            // --- Damerau-Levenshtein Addition ---
            // Check for transposition of adjacent characters
            if (i > 1 && j > 1 &&
                this[i - 1] == haystackString[j - 2] &&
                this[i - 2] == haystackString[j - 1]
            ) {
                // If a transposition is found, compare its cost with the current minimum
                distance = minOf(distance, twoRowsBack[j - 2] + 1)
            }
            currentRow[j] = distance
        }
        // Move down a row, reusing the oldest row's array for the next one
        val reused = twoRowsBack
        twoRowsBack = previousRow
        previousRow = currentRow
        currentRow = reused
    }
    // The final value in the DP table is the Damerau-Levenshtein distance
    // Normalize the distance to a ratio. A lower ratio means a better match.
    return (previousRow[len2] / maxLen.toDouble()) + sameSizeCost
}
