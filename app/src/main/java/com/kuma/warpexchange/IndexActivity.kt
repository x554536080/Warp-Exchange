package com.kuma.warpexchange

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kuma.warpexchange.MainActivity.Companion.USER_AUTHORIZATION
import com.kuma.warpexchange.network.TradingService
import com.kuma.warpexchange.util.SPUtil
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class IndexActivity : AppCompatActivity() {

    companion object {
        const val BASE_URL_API = "http://172.20.10.3:8001/"
    }

    private val retrofit =
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
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_index)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.index_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val call = retrofit.create(TradingService::class.java)
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
}