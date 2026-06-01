package kr.co.kimberly.wma.Manager.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import koamtac.kdc.sdk.KDCBarcodeDataReceivedListener
import koamtac.kdc.sdk.KDCConnectionListenerEx
import koamtac.kdc.sdk.KDCConstants
import koamtac.kdc.sdk.KDCData
import koamtac.kdc.sdk.KDCDevice
import koamtac.kdc.sdk.KDCErrorListener
import koamtac.kdc.sdk.KDCReader

@SuppressLint("StaticFieldLeak")
object ScannerManager: KDCConnectionListenerEx, KDCErrorListener, KDCBarcodeDataReceivedListener {
    private var context: Context? = null
    private var kdcReader: KDCReader? = null
    private var callback: ScannerCallback? = null

    /**
     * 스캐너 초기화
     */
    fun initialize(context: Context, callback: ScannerCallback) {
        this.context = context
        this.callback = callback

        if (kdcReader == null) {
            kdcReader = KDCReader()

            kdcReader?.SetContext(context)
            kdcReader?.SetKDCConnectionListenerEx(this)
            kdcReader?.SetKDCErrorListener(this)
            kdcReader?.SetBarcodeDataReceivedListener(this)
        }
    }

    /**
     * 콜백 해제
     */
    fun clearCallback() {
        callback = null
    }

    /**
     * 연결 여부
     */
    fun isConnected(): Boolean {
        return kdcReader?.IsConnected() == true
    }

    /**
     * 연결
     */
    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = bluetoothAdapter.bondedDevices
        val targetDevice = pairedDevices.firstOrNull {
            it.address == address
        } ?: return

        val kdcDevice = KDCDevice(targetDevice)
        kdcReader?.ConnectEx(kdcDevice)
    }

    /**
     * 연결 해제
     */
    fun disconnect() {
        kdcReader?.Disconnect()
    }

    override fun ConnectionChangedEx(device: KDCDevice<*>, state: Int) {
        when (state) {
            KDCConstants.CONNECTION_STATE_CONNECTED -> {
                callback?.onConnected(device.GetDeviceName())
            }

            KDCConstants.CONNECTION_STATE_LOST -> {
                callback?.onDisconnected(device.GetDeviceName())
            }

            KDCConstants.CONNECTION_STATE_FAILED -> {
                callback?.onConnectionFailed(device.GetDeviceName())
            }

        }
    }

    override fun ErrorReceived(p0: KDCDevice<*>?, p1: Int) {

    }

    override fun BarcodeDataReceived(data: KDCData) {
        val barcode = data.GetData()
        callback?.onBarcodeScanned(barcode)
    }
}