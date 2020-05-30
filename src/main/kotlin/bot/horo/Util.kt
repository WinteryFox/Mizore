package bot.horo

import kotlin.math.min


// http://commons.apache.org/sandbox/commons-text/jacoco/org.apache.commons.text.similarity/FuzzyScore.java.html
fun String.fuzzyScore(query: CharSequence?): Float {
    require(query != null) { "Strings must not be null" }

    val termLowerCase = this.toLowerCase()
    val queryLowerCase = query.toString().toLowerCase()

    var score = 0

    // the position in the term which will be scanned next for potential
    // query character matches
    var termIndex = 0

    // index of the previously matched character in the term
    var previousMatchingCharacterIndex = Int.MIN_VALUE
    for (element in queryLowerCase) {
        var termCharacterMatchFound = false
        while (termIndex < termLowerCase.length
            && !termCharacterMatchFound
        ) {
            val termChar = termLowerCase[termIndex]
            if (element == termChar) {
                // simple character matches result in one point
                score++

                // subsequent character matches further improve
                // the score.
                if (previousMatchingCharacterIndex + 1 == termIndex) {
                    score += 2
                }
                previousMatchingCharacterIndex = termIndex

                // we can leave the nested loop. Every character in the
                // query can match at most one character in the term.
                termCharacterMatchFound = true
            }
            termIndex++
        }
    }
    return score / this.length.toFloat()
}


fun String.levenshteinDistance(y: String?): Int {
    require(y != null) { "Strings must not be null" }

    fun costOfSubstitution(a: Char, b: Char): Int {
        return if (a == b) 0 else 1
    }

    val dp = Array(this.length + 1) { IntArray(y.length + 1) }
    for (i in 0..this.length) {
        for (j in 0..y.length) {
            when {
                i == 0 -> dp[i][j] = j
                j == 0 -> dp[i][j] = i
                else -> {
                    dp[i][j] = min(
                        min(
                            dp[i - 1][j - 1] + costOfSubstitution(this[i - 1], y[j - 1]),
                            dp[i - 1][j] + 1
                        ),
                        dp[i][j - 1] + 1
                    )
                }
            }
        }
    }
    return dp[this.length][y.length]
}