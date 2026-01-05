package com.kuma.warpexchange.model

import com.kuma.warpexchange.enums.Direction
import com.kuma.warpexchange.network.ApiError
import com.kuma.warpexchange.network.ApiException
import java.math.BigDecimal
import java.math.RoundingMode

class OrderRequestBean(
    var direction: Direction?,
    var price: BigDecimal?,
    var quantity: BigDecimal?
) : ValidatableBean {


    override fun validate() {
        if (this.direction == null) {
            throw ApiException(ApiError.PARAMETER_INVALID, "direction", "direction is required.")
        }
        // price:
        if (this.price == null) {
            throw ApiException(ApiError.PARAMETER_INVALID, "price", "price is required.")
        }
        this.price = price!!.setScale(2, RoundingMode.DOWN)
        if (price!!.signum() <= 0) {
            throw ApiException(ApiError.PARAMETER_INVALID, "price", "price must be positive.")
        }
        // quantity:
        if (this.quantity == null) {
            throw ApiException(ApiError.PARAMETER_INVALID, "quantity", "quantity is required.")
        }
        this.quantity = quantity!!.setScale(2, RoundingMode.DOWN)
        if (quantity!!.signum() <= 0) {
            throw ApiException(ApiError.PARAMETER_INVALID, "quantity", "quantity must be positive.")
        }
    }
}

