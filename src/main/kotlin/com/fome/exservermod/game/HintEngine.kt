package com.fome.exservermod.game

/**
 * Seeker hint rotation for CLASSIC mode.
 *
 * Guarantees:
 * - the very first hint of a seeking phase is the distance hint;
 * - distance shows up every other hint (twice as often as before);
 * - varied hints come from a shuffled bag, so none of them repeats until
 *   the whole bag is exhausted.
 */
class HintEngine {
    private var nextIsDistance: Boolean = true
    private var lastVaried: VariedHint? = null
    private val bag: MutableList<VariedHint> = mutableListOf()

    fun reset() {
        nextIsDistance = true
        lastVaried = null
        bag.clear()
    }

    fun nextHint(): SeekerHint =
        if (nextIsDistance) {
            nextIsDistance = false
            SeekerHint(SeekerHintType.DISTANCE, 0)
        } else {
            nextIsDistance = true
            if (bag.isEmpty()) {
                bag.addAll(VariedHint.entries)
                bag.shuffle()
                // Guarantee no immediate repeat across bag boundaries
                if (bag.size > 1 && bag.last() == lastVaried) {
                    val swapIndex = kotlin.random.Random.nextInt(bag.size - 1)
                    val tmp = bag[swapIndex]
                    bag[swapIndex] = bag[bag.size - 1]
                    bag[bag.size - 1] = tmp
                }
            }
            val varied = bag.removeAt(bag.size - 1)
            lastVaried = varied
            SeekerHint(SeekerHintType.VARIED, varied.ordinal)
        }

    enum class SeekerHintType {
        DISTANCE,
        VARIED,
    }

    enum class VariedHint {
        LANDMARK,
        BLOCK_UNDER,
        SKY_ACCESS,
        HEIGHT_DIFF,
    }

    data class SeekerHint(
        val type: SeekerHintType,
        val variedOrdinal: Int,
    )
}
