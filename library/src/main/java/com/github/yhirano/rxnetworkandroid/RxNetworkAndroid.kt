package com.github.yhirano.rxnetworkandroid

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.annotation.RequiresPermission
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class RxNetworkAndroid private constructor() {
    companion object {
        private const val TAG = "RxNetworkAndroid"

        private val subject = PublishSubject.create<ConnectionState>()

        @JvmStatic
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        fun observeNetworkConnectivity(context: Context): Observable<ConnectionState> {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (isConnected(connectivityManager)) {
                        subject.onNext(ConnectionState.CONNECTED)
                    }
                }

                override fun onLost(network: Network) {
                    if (!isConnected(connectivityManager, listOf(network))) {
                        subject.onNext(ConnectionState.DISCONNECTED)
                    }
                }
            }

            connectivityManager.registerNetworkCallback(request, callback)

            return subject
                .toFlowable(BackpressureStrategy.LATEST)
                .doOnCancel {
                    try {
                        connectivityManager.unregisterNetworkCallback(callback)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to unregister network callback.", e)
                    }
                }
                .startWithItem(
                    if (getActiveNetworks(connectivityManager).isNotEmpty()) {
                        ConnectionState.CONNECTED
                    } else {
                        ConnectionState.DISCONNECTED
                    }
                )
                .distinctUntilChanged()
                .toObservable()
        }

        /**
         * @param excludes The networks specified here does not check the connection.
         */
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        private fun isConnected(
            connectivityManager: ConnectivityManager,
            excludes: Collection<Network> = emptyList()
        ): Boolean {
            return getActiveNetworks(connectivityManager, excludes).isNotEmpty()
        }

        /**
         * @param excludes The networks specified here does not check the connection.
         */
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        private fun getActiveNetworks(
            connectivityManager: ConnectivityManager,
            excludes: Collection<Network> = emptyList()
        ): List<NetworkCapabilities> {
            return connectivityManager.allNetworks
                .filter { !excludes.contains(it) }
                .mapNotNull { connectivityManager.getNetworkCapabilities(it) }
                .filter {
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
        }
    }
}