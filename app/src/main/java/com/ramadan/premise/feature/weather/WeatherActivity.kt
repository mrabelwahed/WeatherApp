package com.ramadan.premise.feature.weather

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.ramadan.premise.R
import com.ramadan.premise.domain.entity.WeatherInfo
import com.ramadan.premise.feature.weather.adapter.ForecastAdapter
import com.ramadan.premise.core.common.AppConst.HUMIDITY
import com.ramadan.premise.core.common.AppConst.PRESSURE
import com.ramadan.premise.core.common.AppConst.TEMPREATURE
import com.ramadan.premise.core.common.AppConst.WEATHER_STATUS
import com.ramadan.premise.core.error.Failure
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_weather.*
import javax.inject.Inject

@AndroidEntryPoint
class WeatherActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    @Inject lateinit var forecastAdapter: ForecastAdapter
    private val weatherInfoViewModel: WeatherInfoViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)
        weatherPullTpRefresh.setOnRefreshListener(this)
        handleGoButtonClick()
        handleEnterButtonClick()
        handleNext14DaysForecast()
        observeCurrentWeatherState()
        observeForecastWeatherState()
        observeFailureState()
    }

    private fun observeFailureState() {
        weatherInfoViewModel.failure.observe(this, Observer {
            handleLoading(false)
            hidePullToRefresh()
            when(it){
                is Failure.NetworkConnection ->
                    displayError(getString(R.string.no_internet_connection))
                else ->
                    displayError("something went wrong")
            }
        })
    }

    private fun handleEnterButtonClick() {
        cityNameFiled.setOnKeyListener { view, keyCode, event ->
            if (event?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                view?.let { getWeatherInfo(it) }
                true
            } else false
        }
    }

    private fun handleNext14DaysForecast() {
        next14DaysButton.setOnClickListener {
            handleLoading(true)
            weatherInfoViewModel.getForecastWeatherData()
        }
    }

    private fun handleGoButtonClick() {
        goButton.setOnClickListener {
           getWeatherInfo(it)
        }
    }

    private fun getWeatherInfo(view : View){
        val cityName = cityNameFiled.text.toString()
        if (cityName.isEmpty())
            displayError(getString(R.string.city_name_hint))
        else {
            hideKeyboardFrom(this, view)
            weatherInfoViewModel.resetWeatherState()
            handleLoading(true)
            weatherInfoViewModel.getCurrentWeatherInfo(cityNameFiled.text.toString())
        }
    }

    private fun observeCurrentWeatherState() {
        weatherInfoViewModel.weatherDataState.observe(
            this,
            Observer {
                when (it) {
                    is WeatherInfo -> {
                        handleLoading(false)
                        setCurrentWeatherInfo(it)
                        next14DaysButton.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    private fun observeForecastWeatherState() {
        weatherInfoViewModel.weatherForecastDataState.observe(
            this,
            Observer {
                when (it) {
                    is List<WeatherInfo> -> {
                        handleLoading(false)
                        hidePullToRefresh()
                        forecastRecyclerView.layoutManager = LinearLayoutManager(this)
                        forecastRecyclerView.adapter = forecastAdapter
                        forecastAdapter.submitList(it)
                    }
                }
            }
        )
    }

    private fun setCurrentWeatherInfo(data: WeatherInfo) {
        temperatureTextView.text = TEMPREATURE.plus(data.temperature.toString())
        humidityTextView.text = HUMIDITY.plus(data.humidity.toString())
        pressureTextView.text = PRESSURE.plus(data.pressure.toString())
        weatherStatusTextView.text = WEATHER_STATUS.plus(data.weatherStatus)
        Glide.with(applicationContext).load(data.weatherIcon).into(weatherIcon)
    }

    private fun handleLoading(isDisplayed: Boolean) {
        weatherLoader.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    private fun hidePullToRefresh() {
        weatherPullTpRefresh.isRefreshing = false
    }

    private fun displayError(message: String?) {
        message?.let { Snackbar.make(weatherMainView, it, Snackbar.LENGTH_SHORT).show() }
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRefresh() {
        weatherInfoViewModel.resetForecastState()
        weatherInfoViewModel.getForecastWeatherData()
    }
}
