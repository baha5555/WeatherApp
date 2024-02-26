package com.example.weatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.fragments.MainFragment

const val KEY = "b7927acef7844326b3f124158242202"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().replace(R.id.placeHolder, MainFragment.newInstance()).commit()

       /* binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.button.setOnClickListener {
            getResult("London")
        }*/

    }

   /* private fun getResult(name: String) {
        val url = "http://api.weatherapi.com/v1/current.json" +
                "?key=$KEY&q=$name&aqi=no"

        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET,
                url,
                {
                    response ->
                    Log.i("MyLog", "response:$response ")
                },
                {
                    Log.d("MyLog", "VOLLEY error: $it") })
        queue.add(stringRequest)
    }*/
}