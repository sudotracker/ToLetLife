package com.sudotracker.toletlife


import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sudotracker.toletlife.Error.ErrorResponse
import com.sudotracker.toletlife.Error.ValidationErrorResponse
import com.sudotracker.toletlife.Requests.LoginRequest
import com.sudotracker.toletlife.Requests.OtpRequest
import com.sudotracker.toletlife.Responses.LoginResponse
import com.sudotracker.toletlife.Responses.OtpResponse
import com.sudotracker.toletlife.Services.IdentityService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val et_login_email: EditText = findViewById(R.id.et_login_email)
        et_login_email.hint = "Email"
        val et_login_password: EditText = findViewById(R.id.et_login_password)
        et_login_password.hint = "Password"
        val btn_login: Button = findViewById(R.id.btn_login)
        val btn_send_otp: Button = findViewById(R.id.btn_send_otp)
        val et_send_otp_email: EditText = findViewById(R.id.et_send_otp_email)
        val intent_email = intent.getStringExtra("email")
        if(intent_email != null){
            et_login_email.setText(intent_email)
            saveToken(null)
        }

        val jwtToken = loadData()
        if(jwtToken != null){
            val intent = Intent(this, RentalOptions::class.java)
            startActivity(intent)
        }
        et_send_otp_email.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && (event.action == KeyEvent.ACTION_UP || event.action == KeyEvent.ACTION_DOWN)) {
                btn_send_otp.performClick()
                return@OnKeyListener true
            }
            false
        })


        btn_login.setOnClickListener {
            val email = et_login_email.text
            et_login_email.setText("")
            val password = et_login_password.text
            et_login_password.setText("")
            if(email.toString() != "" && password.toString() != ""){
                sendLogin(email.toString(),password.toString())
            }
        }

        btn_send_otp.setOnClickListener {
            val email = et_send_otp_email.text
            et_send_otp_email.setText("")
            if(email.toString() != ""){
                sendOtp(email.toString())
            }
        }
    }

    private fun sendOtp(email: String) {
        val otpRequest = OtpRequest(email)
        val call = IdentityService.identityInstance.sendOtp(otpRequest)
        val intent = Intent(this, RegisterActivity::class.java)
        val gson = Gson()
        call.enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.code() == 422) {
                    val type = object : TypeToken<ValidationErrorResponse>() {}.type
                    val errorResponse: ValidationErrorResponse? =
                        gson.fromJson(response.errorBody()?.charStream(), type)
                    Toast.makeText(this@MainActivity,errorResponse?.detail?.first()?.msg.toString(),Toast.LENGTH_LONG).show()
                    return
                } else if (response.code() > 399) {
                    val type = object : TypeToken<ErrorResponse>() {}.type
                    val errorResponse: ErrorResponse? =
                        gson.fromJson(response.errorBody()?.charStream(), type)
                    if(errorResponse?.detail.toString().contains("Otp already sent to")){
                        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        Toast.makeText(this@MainActivity,errorResponse?.detail.toString(),Toast.LENGTH_LONG).show()
                        startActivity(intent)
                        return
                    }
                    Toast.makeText(this@MainActivity,errorResponse?.detail.toString(),Toast.LENGTH_LONG).show()
                    return
                }else if (response.code() == 200) {
                    val jsonResponse = gson.toJson(response.body())
                    val resp: OtpResponse = gson.fromJson(jsonResponse,OtpResponse::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    Toast.makeText(this@MainActivity,resp.otpResponse,Toast.LENGTH_LONG).show()
                    intent.putExtra("email",email)
                    startActivity(intent)
                    return
                }
            }
            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d("failure", "Error in failure", t)
            }
        })
    }

    private fun sendLogin(email: String,password: String){
        val loginRequest = LoginRequest(email,password)
        val call = IdentityService.identityInstance.sendLogin(loginRequest)
        val intent = Intent(this, RentalOptions::class.java)
        val gson = Gson()
        call.enqueue(object : Callback<Any>{
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.code() == 422) {
                    val type = object : TypeToken<ValidationErrorResponse>() {}.type
                    val errorResponse: ValidationErrorResponse? =
                        gson.fromJson(response.errorBody()?.charStream(), type)
                    Toast.makeText(this@MainActivity,errorResponse?.detail?.first()?.msg.toString(),Toast.LENGTH_LONG).show()
                    return
                } else if (response.code() > 399) {
                    val type = object : TypeToken<ErrorResponse>() {}.type
                    val errorResponse: ErrorResponse? =
                        gson.fromJson(response.errorBody()?.charStream(), type)
                    Toast.makeText(this@MainActivity,errorResponse?.detail.toString(),Toast.LENGTH_LONG).show()
                    return
                }else if (response.code() == 200) {
                    val jsonResponse = gson.toJson(response.body())
                    val resp: LoginResponse = gson.fromJson(jsonResponse,LoginResponse::class.java)
                    saveToken(resp.token)
                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    Toast.makeText(this@MainActivity,"Login Successful",Toast.LENGTH_LONG).show()
                    startActivity(intent)
                    return
                }
            }
            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d("failure", "Error in failure", t)
            }

        })

    }
    private fun saveToken(token: String?){
        val sharedPreferences = getSharedPreferences("jwtToken", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply{
            putString("JWT_TOKEN",token)
        }.apply()
    }
    private fun loadData(): String?{
        val sharedPreferences = getSharedPreferences("jwtToken",Context.MODE_PRIVATE)
        return sharedPreferences.getString("JWT_TOKEN",null)
    }
}