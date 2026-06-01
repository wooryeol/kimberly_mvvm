package kr.co.kimberly.wma.common

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext

@SuppressLint("MissingPermission")
class ConnectThread(private val myUUID: UUID, private val device: BluetoothDevice, private val mContext: Context): Thread() {
    companion object {
        private const val TAG = "CONNECT_SOCKET"
    }

    private val connectSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(myUUID)
    }

    /*override fun run() {
        try {
            // 연결 수행
            connectSocket?.connect()
            connectSocket?.let {
                val connectedThread = ConnectedThread(bluetoothSocket = it)
                connectedThread.start()
            }
        } catch (e: IOException) { // 기기와의 연결이 실패할 경우 호출
            connectSocket?.close()
            // Utils.log("$TAG run error ====> $e")
        }
    }*/

    fun customRun():Boolean {
        try {
            // 연결 수행
            connectSocket?.connect()
            connectSocket?.let {
                val connectedThread = ConnectedThread(bluetoothSocket = it)
                connectedThread.start()
                return true
            }
        } catch (e: IOException) { // 기기와의 연결이 실패할 경우 호출
            connectSocket?.close()
            // Utils.log("$TAG run error ====> $e")
            return false
        }
        return false
    }

    fun cancel() {
        try {
            connectSocket?.close()
        } catch (e: IOException) {
            // Utils.log("$TAG cancel error ====> $e")
        }
    }

    private inner class ConnectedThread(private val bluetoothSocket: BluetoothSocket): Thread() {
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream

        init {
            try {
                // BluetoothSocket의 InputStream, OutputStream 초기화
                inputStream = bluetoothSocket.inputStream
                outputStream = bluetoothSocket.outputStream
            } catch (e: IOException) {
                // Utils.log("$TAG ConnectedThread init error ====> $e")
            }
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    // 데이터 받기(읽기)
                    bytes = inputStream.read(buffer)
                    // 받은 데이터를 문자열로 변환 (ASCII로 시도)
                    val barcodeData = String(buffer, 0, bytes, Charsets.US_ASCII)
                    // 정규 표현식으로 바코드 패턴 추출 (임의의 두 알파벳 + 8~14자리 숫자 또는 연속된 12~14자리 숫자)
                    val regex = Regex("[A-Z]{2}\\d{8,14}|\\d{12,14}")
                    val matches = regex.findAll(barcodeData)
                    // 추출된 바코드 데이터
                    val barcodeList = matches.map { it.value }.toList()
                    // 각 바코드를 쉼표로 구분하여 하나의 문자열로 결합
                    barcodeList.joinToString(separator = ",")
                    val intent = Intent("kr.co.kimberly.wma.ACTION_BARCODE_SCANNED")
                    intent.putExtra("data",  barcodeList.joinToString(separator = ","))
                    mContext.sendBroadcast(intent)

                } catch (e: Exception) { // 기기와의 연결이 끊기면 호출
                    // Utils.log("기기와의 연결이 끊겼습니다.")
                    // Utils.log("error ====> $e")
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                // 데이터 전송
                outputStream.write(bytes)
            } catch (e: IOException) {
                // Utils.log("$TAG ConnectedThread write error ====> $e")
            }
        }

        fun cancel() {
            try {
                bluetoothSocket.close()
            } catch (e: IOException) {
                // Utils.log("$TAG ConnectedThread cancel error ====> $e")
            }
        }
    }
}


