package com.example.weatherapp.fragments

import android.app.Dialog
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.DialogManager
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.adapters.VpAdapters
import com.example.weatherapp.adapters.WeatherModel

import com.example.weatherapp.databinding.FragmentMainBinding
import com.example.weatherapp.responseItem.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

const val API_KEY = "b7927acef7844326b3f124158242202"

class MainFragment : Fragment() {
    private lateinit var location: FusedLocationProviderClient
    private val flist = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )
    private val tlist = listOf(
        "Hours", "Days"
    )
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        updateCurrentCard()
    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }

    private fun init() = with(binding) {
        location = LocationServices.getFusedLocationProviderClient(requireActivity())
        val adapter = VpAdapters(activity as FragmentActivity, flist)
        vp.adapter = adapter
        TabLayoutMediator(tabLayout, vp) { tab, pos ->
            tab.text = tlist[pos]
        }.attach()
        ibSync.setOnClickListener {
            tabLayout.selectTab(tabLayout.getTabAt(0))
            checkLocation()
        }
        ibSearch.setOnClickListener {
            DialogManager.searchByNameDialog(requireContext(),object :DialogManager.Listener{
                override fun onClick(name: String?) {
                    name?.let { it1 -> requestWeatherData(it1) }
                }

            })
        }

    }

    private fun checkLocation() {
        if (isLocationEnanled()) {
            getLocation()
        } else {
            DialogManager.locationSettingsDialog(requireContext(), object : DialogManager.Listener {
                override fun onClick(name:String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
        }
    }

    private fun isLocationEnanled(): Boolean {
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getLocation() {
        val ct = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        location.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, ct.token)
            .addOnCompleteListener {
                requestWeatherData("${it.result.latitude},${it.result.longitude}")
            }
    }

    private fun updateCurrentCard() = with(binding) {
        model.liveDataCurrent.observe(viewLifecycleOwner) {
            val maxMinTemp = "${it.maxTemp}°C/${it.minTemp}°C"
            tvData.text = it.time
            tvCity.text = it.city
            tvCurrentTemp.text = it.currentTemp.ifEmpty { "${it.maxTemp}°C/${it.minTemp}°C" }
            tvContidion.text = it.condition
            tvMaxMin.text = if (it.currentTemp.isEmpty()) "" else maxMinTemp
            Picasso.get().load("https:" + it.imageUrl).into(imageView3)
        }
    }

    private fun permissionListener() {
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission() {
        if (!isPermissionGranted(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionListener()
            pLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city: String) {
        binding.progressBar.visibility = View.VISIBLE

        val url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                "$API_KEY" +
                "&q=" +
                "$city" +
                "&days=" +
                "5" +
                "&aqi=no&alerts=no\n"
        val queue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(
            Request.Method.GET,
            url,
            { response ->
                parseWeatherData(response)
                Toast.makeText(requireContext(),"Данные обновлено",Toast.LENGTH_SHORT).show()
                /*Log.i("MyLog", "response:$response ")*/
            },
            {
                binding.progressBar.visibility = View.INVISIBLE
                Log.d("MyLog", "VOLLEY error: $it")
            })
        queue.add(stringRequest)
    }

    private fun parseWeatherData(result: String) {
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel> {
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast").getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")

        for (i in 0 until daysArray.length()) {
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }

    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel) {
        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current").getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObject.getJSONObject("current").getJSONObject("condition").getString("icon"),
            weatherItem.hours
        )
        model.liveDataCurrent.value = item
        Log.d("MyLog", "City ${item.city}")
        Log.d("MyLog", "Time ${item.time}")
        Log.d("MyLog", "Condition ${item.condition}")
        Log.d("MyLog", "Temp ${item.currentTemp}")
        Log.d("MyLog", "Url ${item.imageUrl}")
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            MainFragment()
    }
}