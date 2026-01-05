package com.kuma.warpexchange.enums

enum class Direction(
    /**
     * Direction的int值
     */
    val value: Int
) {
    BUY(1),

    SELL(0);

    /**
     * Get negate direction.
     */
    fun negate(): Direction {
        return if (this == BUY) SELL else BUY
    }

    companion object {
        fun of(intValue: Int): Direction {
            if (intValue == 1) {
                return BUY
            }
            if (intValue == 0) {
                return SELL
            }
            throw IllegalArgumentException("Invalid Direction value.")
        }
    }
}
