package com.github.yhirano.rxnetworkandroid.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.yhirano.rxnetworkandroid.ConnectionState
import com.github.yhirano.rxnetworkandroid.RxNetworkAndroid
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private val textView by lazy {
        findViewById<TextView>(R.id.text)
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RxNetworkAndroid.observeNetworkConnectivity(this)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                textView.text = when (it) {
                    ConnectionState.CONNECTED -> "Connected to the Internet"
                    ConnectionState.DISCONNECTED -> "Disconnected to the Internet"
                }
            }
            .addTo(compositeDisposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }
}