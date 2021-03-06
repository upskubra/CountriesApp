package com.example.countryapp.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.countryapp.model.Country
import com.example.countryapp.service.CountryAPIService
import com.example.countryapp.service.CountryDatabase
import com.example.countryapp.util.CustomSharedPreferences
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch

class ListViewModel(application: Application) : BaseViewModel(application) {

    private val countryApiService = CountryAPIService()

    // use and at object, required for memory management
    private val disposable = CompositeDisposable()
    private var customPreferences = CustomSharedPreferences(getApplication())
    private var refreshTime = 10 * 60 * 1000* 1000* 1000L


    val countries = MutableLiveData<List<Country>>()
    val countryErrorMessage = MutableLiveData<Boolean>()
    val countryProgressBar = MutableLiveData<Boolean>()

    fun refreshData() {

        val updateTime = customPreferences.getTime()
        if (updateTime != null && updateTime != 0L && System.nanoTime() -  updateTime < refreshTime ){
            getDataFromSQL()
        } else {
            getDataFromAPI()
        }

    }

    private fun getDataFromSQL(){

        launch {
            val countries = CountryDatabase(getApplication()).countryDao().getAllCountries()
            showCounties(countries)
            Toast.makeText(getApplication(), "SQL", Toast.LENGTH_LONG).show()
        }
    }
fun refreshDataFromAPI(){
    getDataFromAPI()
}
    private fun getDataFromAPI() {

        countryProgressBar.value = true

        disposable.add(
            countryApiService.getData()
                // different thread because asynchronous, working background
                .subscribeOn(Schedulers.newThread())

                .observeOn(AndroidSchedulers.mainThread())

                .subscribeWith(object : DisposableSingleObserver<List<Country>>() {
                    override fun onSuccess(t: List<Country>) {

                        storeInSQLite(t)


                    }

                    override fun onError(e: Throwable) {
                        countryErrorMessage.value = true
                        countryProgressBar.value = false
                        e.printStackTrace()

                    }

                })
        )
        Toast.makeText(getApplication(), "JSON", Toast.LENGTH_LONG).show()


    }

    private fun showCounties(countryList: List<Country>) {
        countries.value = countryList
        countryErrorMessage.value = false
        countryProgressBar.value = false
    }

    private fun storeInSQLite(list: List<Country>) {
        launch {
            val dao = CountryDatabase(getApplication()).countryDao()
            dao.deleteAllCountries()
            val listLong = dao.insertAll(*list.toTypedArray()) // -->individual
            var i = 0
            while (i < list.size) {
                list[i].uuid = listLong[i].toInt()
                i++
            }
            showCounties(list)
        }

        customPreferences.saveTime(System.nanoTime())

    }

    override fun onCleared() {
        //for memory
        super.onCleared()
        disposable.clear()
    }
}