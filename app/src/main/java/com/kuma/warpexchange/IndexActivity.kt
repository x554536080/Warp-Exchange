package com.kuma.warpexchange

import android.content.Intent
import android.content.res.Resources
import android.graphics.Paint
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kuma.warpexchange.MainActivity.Companion.BASE_URL_IP
import com.kuma.warpexchange.MainActivity.Companion.LOGIN_STATUS_COOKIE
import com.kuma.warpexchange.MainActivity.Companion.USER_AUTHORIZATION
import com.kuma.warpexchange.enums.Direction
import com.kuma.warpexchange.model.OrderRequestBean
import com.kuma.warpexchange.network.service.TradingService
import com.kuma.warpexchange.util.SPUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.math.BigDecimal

class IndexActivity : AppCompatActivity() {

    companion object {
        const val BASE_URL_API = "http://${BASE_URL_IP}:8001/"
        const val BASE_URL_PUSH = "http://${BASE_URL_IP}:8006/"
    }

    private lateinit var myOrdersItemContainer: LinearLayout
    private lateinit var orderFormPriceEditText: EditText
    private lateinit var orderFormQuantityEditText: EditText

    private val tradingService =
        Retrofit.Builder().baseUrl(BASE_URL_API)
            .client(OkHttpClient.Builder().addInterceptor { c ->
                val newRequest = c.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        " Basic ${SPUtil.getString(this, USER_AUTHORIZATION)}"
                    ).method(c.request().method(), c.request().body()).build()
                c.proceed(newRequest)
            }.build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(TradingService::class.java)
    private lateinit var webSocket: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_index)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.index_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
    }

    private fun initViews() {
        myOrdersItemContainer = findViewById(R.id.my_orders_item_container)
        orderFormPriceEditText = findViewById(R.id.order_form_price_editText)
        orderFormQuantityEditText = findViewById(R.id.order_form_quantity_editText)
        findViewById<TextView>(R.id.index_signOut).setOnClickListener {
            SPUtil.removeString(this@IndexActivity, LOGIN_STATUS_COOKIE)
            SPUtil.removeString(this@IndexActivity, USER_AUTHORIZATION)
            startActivity(Intent(this@IndexActivity, MainActivity::class.java))
            finish()
        }
        queryPersonalAssets()
        initOrderForm()
        queryOpenOrders()
        queryOrderBook()
        initWebSocket()
    }

    private fun initOrderForm() {
        findViewById<TextView>(R.id.order_form_buy_button).setOnClickListener { v ->
            createOrder(
                OrderRequestBean(
                    Direction.BUY,
                    BigDecimal(orderFormPriceEditText.text.toString()),
                    BigDecimal(orderFormQuantityEditText.text.toString()),
                )
            )
        }
        findViewById<TextView>(R.id.order_form_sell_button).setOnClickListener { v ->
            createOrder(
                OrderRequestBean(
                    Direction.SELL,
                    BigDecimal(orderFormPriceEditText.text.toString()),
                    BigDecimal(orderFormQuantityEditText.text.toString()),
                )
            )
        }
    }

    private fun queryPersonalAssets() {
        val call = tradingService
            .getAssets()
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                val jsonObject = JSONObject(response.body() ?: "")
                findViewById<TextView>(R.id.assets_USD_available).text =
                    jsonObject.getJSONObject("USD").getDouble("available").toString()
                findViewById<TextView>(R.id.assets_USD_frozen).text =
                    jsonObject.getJSONObject("USD").getDouble("frozen").toString()
                findViewById<TextView>(R.id.assets_BTC_available).text =
                    jsonObject.getJSONObject("BTC").getDouble("available").toString()
                findViewById<TextView>(R.id.assets_BTC_frozen).text =
                    jsonObject.getJSONObject("BTC").getDouble("frozen").toString()
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                println("fail")
            }

        })
    }

    private fun createOrder(orderRequestBean: OrderRequestBean) {
        tradingService.createOrder(orderRequestBean).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                Toast.makeText(this@IndexActivity, "Creation Successful", Toast.LENGTH_SHORT).show()
                orderFormPriceEditText.setText("")
                orderFormQuantityEditText.setText("")
                //待优化刷新逻辑
                queryPersonalAssets()
                queryOpenOrders()
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@IndexActivity, "Creation Failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun queryOpenOrders() {
        tradingService.getOpenOrders().enqueue(
            object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    println(response.body())

                    myOrdersItemContainer.removeAllViews()
                    val itemsArray = JSONArray(response.body())
                    for (i in 0 until itemsArray.length()) {
                        val itemObject = itemsArray[i] as JSONObject
                        val itemView = layoutInflater.inflate(R.layout.layout_my_orders_item, null)
                        itemView.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                40f,
                                Resources.getSystem().displayMetrics
                            ).toInt()
                        )
                        var textColor = 0
                        if (itemObject.getString("direction") == "BUY") {
                            textColor = 0xFF7FDF7F.toInt()
                        } else if (itemObject.getString("direction") == "SELL") {
                            textColor = 0xFFDF7F7F.toInt()
                        }
                        itemView.findViewById<TextView>(R.id.my_orders_item_direction).text =
                            itemObject.getString("direction")
                        itemView.findViewById<TextView>(R.id.my_orders_item_direction)
                            .setTextColor(textColor)
                        itemView.findViewById<TextView>(R.id.my_orders_item_price).text =
                            itemObject.getDouble("price").toString()
                        itemView.findViewById<TextView>(R.id.my_orders_item_price)
                            .setTextColor(textColor)
                        itemView.findViewById<TextView>(R.id.my_orders_item_quantity).text =
                            itemObject.getInt("quantity").toString()
                        itemView.findViewById<TextView>(R.id.my_orders_item_quantity)
                            .setTextColor(textColor)
                        itemView.findViewById<TextView>(R.id.my_orders_item_unfilled).text =
                            itemObject.getInt("unfilledQuantity").toString()
                        itemView.findViewById<TextView>(R.id.my_orders_item_unfilled)
                            .setTextColor(textColor)
                        itemView.findViewById<TextView>(R.id.my_orders_item_cancel).paint.flags =
                            Paint.UNDERLINE_TEXT_FLAG
                        itemView.findViewById<TextView>(R.id.my_orders_item_cancel).paint.isAntiAlias =
                            true
                        itemView.findViewById<TextView>(R.id.my_orders_item_cancel)
                            .setOnClickListener { v ->
                                tradingService.cancelOrder(itemObject.getInt("id")).enqueue(
                                    object : Callback<String> {
                                        override fun onResponse(
                                            call: Call<String>,
                                            response: Response<String>
                                        ) {
                                            queryPersonalAssets()
                                            queryOpenOrders()
                                        }

                                        override fun onFailure(call: Call<String>, t: Throwable) {
                                            print("fail")
                                        }
                                    })
                            }
                        myOrdersItemContainer.addView(itemView)
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Toast.makeText(
                        this@IndexActivity,
                        "Open Orders Query Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun queryOrderBook() {
        tradingService.getOrderBook().enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.body() != null) {
                    parseOrderBookJsonObject(JSONObject(response.body()!!))
                    println(response.body())
                }

            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@IndexActivity, "Order Book Query Failed", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun initWebSocket() {
        webSocket = OkHttpClient().newWebSocket(Request.Builder().url("$BASE_URL_PUSH/notification")
            .build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    super.onOpen(webSocket, response)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    val jsonObject = JSONObject(text)
                    if (jsonObject.getString("type") == "orderbook") {
                        parseOrderBookJsonObject(jsonObject.getJSONObject("data"))
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosing(webSocket, code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: okhttp3.Response?
                ) {
                    super.onFailure(webSocket, t, response)
                }
            })
    }

    private fun parseOrderBookJsonObject(orderBookObject: JSONObject) {
        val buyArray = orderBookObject.getJSONArray("buy")
        val sellArray = orderBookObject.getJSONArray("sell")
        for (i in 0 until 5) {
            val priceResourceId =
                resources.getIdentifier(
                    "order_book_buy_item_${i + 1}_price",
                    "id",
                    packageName
                )
            val quantityResourceId =
                resources.getIdentifier(
                    "order_book_buy_item_${i + 1}_quantity",
                    "id",
                    packageName
                )
            if (buyArray.length() > i) {
                findViewById<TextView>(priceResourceId).text =
                    (buyArray[i] as JSONObject).getString("price")
                findViewById<TextView>(quantityResourceId).text =
                    (buyArray[i] as JSONObject).getString("quantity")

            } else {
                findViewById<TextView>(priceResourceId).text = "-"
                findViewById<TextView>(quantityResourceId).text = "-"
            }
        }
        for (i in 0 until 5) {
            val priceResourceId =
                resources.getIdentifier(
                    "order_book_sell_item_${i + 1}_price",
                    "id",
                    packageName
                )
            val quantityResourceId =
                resources.getIdentifier(
                    "order_book_sell_item_${i + 1}_quantity",
                    "id",
                    packageName
                )
            if (sellArray.length() > i) {
                findViewById<TextView>(priceResourceId).text =
                    (sellArray[i] as JSONObject).getString("price")
                findViewById<TextView>(quantityResourceId).text =
                    (sellArray[i] as JSONObject).getString("quantity")
            } else {
                findViewById<TextView>(priceResourceId).text = "-"
                findViewById<TextView>(quantityResourceId).text = "-"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, null)
    }
}
