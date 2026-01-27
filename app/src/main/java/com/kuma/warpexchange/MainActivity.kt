package com.kuma.warpexchange

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kuma.warpexchange.network.service.UserService
import com.kuma.warpexchange.util.CookieUtil
import com.kuma.warpexchange.util.SPUtil
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlin.io.encoding.ExperimentalEncodingApi

class MainActivity : AppCompatActivity() {

    companion object {
        const val BASE_URL_IP = "192.168.124.3"
        const val BASE_URL_USER = "http://${BASE_URL_IP}:8000/"
        const val APP_SP_KEY = "warp_exchange_sp"
        const val LOGIN_STATUS_COOKIE = "login_status_cookie_sp"
        const val USER_AUTHORIZATION = "user_authorization_sp"
    }

    private var isSignUp = false

    private lateinit var signUpToggleButton: ToggleButton
    private lateinit var signInToggleButton: ToggleButton
    private lateinit var emailText: TextView
    private lateinit var emailEditText: EditText
    private lateinit var passwordText: TextView
    private lateinit var passwordEditText: EditText
    private lateinit var signUpNameText: TextView
    private lateinit var signUpNameEditText: EditText
    private lateinit var loginButton: Button


    private val userService =
        Retrofit.Builder().baseUrl(BASE_URL_USER)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(UserService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initViews()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (SPUtil.getString(this, LOGIN_STATUS_COOKIE).isNotEmpty()) {
            if (CookieUtil.getCookieExpires(
                    SPUtil.getString(
                        this,
                        LOGIN_STATUS_COOKIE
                    )
                )?.time!! > System.currentTimeMillis()
            ) {
                val operationResultTextView: TextView = findViewById(R.id.operation_result)
                operationResultTextView.text = "Signed in and expired at ${
                    CookieUtil.getCookieExpires(
                        SPUtil.getString(this, LOGIN_STATUS_COOKIE)
                    )?.toString()!!
                }"
                startActivity(Intent(this, IndexActivity::class.java))
                finish()
            } else {
                val operationResultTextView: TextView = findViewById(R.id.operation_result)
                operationResultTextView.text = "Login session expired"
            }

        }
    }

    private fun initViews() {
        signInToggleButton = findViewById(R.id.login_signIn_toggleButton)
        signInToggleButton.setOnCheckedChangeListener { _, c ->
            if (c) {
                switchToSignUp(false)
            }
        }
        signUpToggleButton = findViewById(R.id.login_signUp_toggleButton)
        signUpToggleButton.setOnCheckedChangeListener { _, c ->
            if (c) {
                switchToSignUp(true)
            }
        }
        signInToggleButton.isChecked = true
        signInToggleButton.alpha = 0.5f

        emailText = findViewById(R.id.login_email_text)
        emailEditText = findViewById(R.id.login_email_editText)
        signUpNameText = findViewById(R.id.login_signUp_name_text)
        signUpNameEditText = findViewById(R.id.login_signup_name_editText)
        passwordText = findViewById(R.id.login_password_text)
        passwordEditText = findViewById(R.id.login_password_editText)
        loginButton = findViewById(R.id.login_button)
        loginButton.setOnClickListener { v ->
            if (isSignUp) {
                doSignUp()
            } else {
                doSignIn()
            }

        }
    }

    private fun switchToSignUp(switch: Boolean) {
        if (switch) {
            isSignUp = true
            signInToggleButton.isChecked = false
            signUpToggleButton.isClickable = false
            signInToggleButton.isClickable = true
            signUpToggleButton.alpha = 0.5f
            signInToggleButton.alpha = 1f
            signUpNameText.visibility = View.VISIBLE
            signUpNameEditText.visibility = View.VISIBLE
            loginButton.text = "Sign Up"
        } else {
            isSignUp = false
            signUpToggleButton.isChecked = false
            signInToggleButton.isClickable = false
            signUpToggleButton.isClickable = true
            signUpToggleButton.alpha = 1f
            signInToggleButton.alpha = 0.5f
            signUpNameText.visibility = View.GONE
            signUpNameEditText.visibility = View.GONE
            loginButton.text = "Sign In"
        }
    }

    private fun doSignUp() {
        val operationResultTextView: TextView = findViewById(R.id.operation_result)
        val call = userService.signUp(
            emailEditText.text.toString(),
            signUpNameEditText.text.toString(),
            passwordEditText.text.toString()
        )
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                operationResultTextView.text = response.body()
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                operationResultTextView.text = "fail"
            }

        })
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun doSignIn() {
        val operationResultTextView: TextView = findViewById(R.id.operation_result)
        val call = userService
            .signIn(
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            )
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.headers().get("Set-Cookie")?.isEmpty() == false) {
                    SPUtil.putString(
                        this@MainActivity,
                        LOGIN_STATUS_COOKIE,
                        response.headers().get("Set-Cookie")!!
                    )
                    SPUtil.putString(
                        this@MainActivity,
                        USER_AUTHORIZATION,
                        kotlin.io.encoding.Base64.encode(
                            "${emailEditText.text}:${passwordEditText.text}".toByteArray(
                                Charsets.UTF_8
                            )
                        )
                    )
                }

                operationResultTextView.text = response.body()
                startActivity(Intent(this@MainActivity, IndexActivity::class.java))
                finish()
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                operationResultTextView.text = "fail"
            }

        })
    }

}