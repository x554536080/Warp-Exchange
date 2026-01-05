package com.kuma.warpexchange.network

class ApiException : RuntimeException {
    val error: ApiErrorResponse

    constructor(error: ApiError) : super(error.toString()) {
        this.error = ApiErrorResponse(error, null, "")
    }

    constructor(error: ApiError, data: String?) : super(error.toString()) {
        this.error = ApiErrorResponse(error, data!!, "")
    }

    constructor(error: ApiError?, data: String?, message: String?) : super(message) {
        this.error = ApiErrorResponse(error!!, data!!, message!!)
    }
}
