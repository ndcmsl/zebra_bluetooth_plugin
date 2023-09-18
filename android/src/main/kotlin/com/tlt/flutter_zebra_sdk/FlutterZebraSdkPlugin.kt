package com.tlt.flutter_zebra_sdk

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.zebra.sdk.btleComm.BluetoothLeConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.comm.TcpConnection
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveredPrinterNetwork
import com.zebra.sdk.printer.discovery.DiscoveryHandler
import com.zebra.sdk.printer.discovery.DiscoveryUtil
import com.zebra.sdk.printer.discovery.NetworkDiscoverer
import com.zebra.sdk.comm.BluetoothConnectionInsecure
import com.zebra.sdk.printer.discovery.UsbDiscoverer
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.util.UUID


// import kotlinx.serialization.*
// import kotlinx.serialization.json.*
//import kotlinx.serialization.internal.*


interface JSONConvertable {
  fun toJSON(): String = Gson().toJson(this)
}

inline fun <reified T : JSONConvertable> String.toObject(): T = Gson().fromJson(this, T::class.java)

data class ZebreResult(
        var type: String? = null,
        var success: Boolean? = null,
        var message: String? = null,
        var content: Any? = null
) : JSONConvertable

class ZebraPrinterInfo(
        var address: String? = null,
        var productName: String? = null,
        var serialNumber: String? = null,
        var availableInterfaces: Any? = null,
        var darkness: String? = null,
        var availableLanguages: Any? = null,
        val linkOSMajorVer: Long? = null,
        val firmwareVer: String? = null,
        var jsonPortNumber: String? = null,
        val primaryLanguage: String? = null
): JSONConvertable


/** FlutterZebraSdkPlugin */
class FlutterZebraSdkPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  // / The MethodChannel that will the communication between Flutter and native Android
  // /
  // / This local reference serves to register the plugin with the Flutter Engine and unregister it
  // / when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel
  private var logTag: String = "ZebraSDK"
  private lateinit var context: Context
  private lateinit var activity: Activity
  var conn: BluetoothLeConnection? = null
  var printers: MutableList<ZebraPrinterInfo> = ArrayList()

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity;
  }

  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_zebra_sdk")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull rawResult: Result) {
    val result: MethodResultWrapper = MethodResultWrapper(rawResult)
    Thread(MethodRunner(call, result)).start()
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  inner class MethodRunner(call: MethodCall, result: Result) : Runnable {
    private val call: MethodCall = call
    private val result: Result = result

    override fun run() {
      when (call.method) {
        "destroyBluetoothConnection" -> {
          destroyBluetoothConnection(call, result)
        }
        "establishBluetoothConnection" -> {
          establishBluetoothConnection(call, result)
        }
        "onDiscovery" -> {
          onDiscovery(call, result)
        }
        "onDiscoveryUSB" -> {
          onDiscoveryUSB(call, result)
        }

        "onGetPrinterInfo" -> {
          onGetPrinterInfo(call, result)
        }
        "isPrinterConnected" -> {
          isPrinterConnected(call, result)
        }

        "printOverBluetooth" -> {
          printOverBluetooth(call, result)
        }
        else -> result.notImplemented()
      }
    }
  }

  class MethodResultWrapper(methodResult: Result) : Result {

    private val methodResult: Result = methodResult
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun success(result: Any?) {
      handler.post { methodResult.success(result) }
    }

    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
      handler.post { methodResult.error(errorCode, errorMessage, errorDetails) }
    }

    override fun notImplemented() {
      handler.post { methodResult.notImplemented() }
    }
  }

  private fun createTcpConnect(ip: String, port: Int): TcpConnection {
    return TcpConnection(ip, port)
  }

  private fun destroyBluetoothConnection(@NonNull call: MethodCall, @NonNull result: Result) {
    Thread {
      try {
        // Asumiendo que "conn" es una variable que referencia a tu conexión actual.
        if (conn != null) {
          if (conn!!.isConnected) {
            conn?.close()
            Thread.sleep(1000)  // Ajusta el tiempo de espera según sea necesario.
          }
        }
        Handler(Looper.getMainLooper()).post {
          result.success("Connection destroyed successfully")
        }
      } catch (e: Exception) {
        e.printStackTrace()
        Handler(Looper.getMainLooper()).post {
          result.error("UNEXPECTED_ERROR", "Unexpected error: ${e.message}", null)
        }
      }
    }.start()
  }

private fun establishBluetoothConnection(@NonNull call: MethodCall, @NonNull result: Result) {
    var macAddress: String? = call.argument("mac")
    Log.d(logTag, "onPrintZplDataOverBluetooth $macAddress")
    Thread {
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.cancelDiscovery()
        try {
            val bluetoothDevice: BluetoothDevice =
                bluetoothAdapter.getRemoteDevice(macAddress)
            if (bluetoothAdapter.isEnabled) {
                Log.d(logTag, "${bluetoothDevice.bondState}")
                Log.d(logTag, "${bluetoothDevice.name} ${bluetoothDevice.address}")

                val printer: DiscoveredPrinter? = reflectivelyInstatiateDiscoveredPrinterBluetoothLe(
                    bluetoothDevice.address,
                    bluetoothDevice.name ?: "zebraPrinter",
                    context
                )
                conn = BluetoothLeConnection(printer?.address, context)
                conn!!.open()
              Thread.sleep(2000)
              Handler(Looper.getMainLooper()).post {
                result.success("Connection established successfully")
              }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                result.error("UNEXPECTED_ERROR", "Unexpected error: ${e.message}", null)
            }
        }
    }.start()
}



  private fun reflectivelyInstatiateDiscoveredPrinterBluetoothLe(var0: String, var1: String, var2: Context): DiscoveredPrinter? {
    try {
      val var3 = Class.forName("com.zebra.sdk.btleComm.DiscoveredPrinterBluetoothLe")
      val var4 = var3.getDeclaredConstructor(String::class.java, String::class.java, Context::class.java)
      var4.isAccessible = true
      return var4.newInstance(var0, var1, context) as DiscoveredPrinter
    } catch (var5: Exception) {
      return null;
    }
  }

  private fun printOverBluetooth(@NonNull call: MethodCall, @NonNull result: Result) {
    var data: ByteArray? = call.argument("data")
    var num: Int? = call.argument("copies")
    num = num ?: 1
    Log.d(logTag, "onPrintZplDataOverBluetooth $data")
    if (data == null) {
      result.error("onPrintZplDataOverBluetooth", "Data is required", "Data Content")
    }
    Thread {
        try {
            // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
          if (conn != null) {
            for (number in 1..num) {
              conn!!.write(data)
              Thread.sleep(2000)
            }
          }
          Handler(Looper.getMainLooper()).post {
            result.success("Connection established successfully")
          }
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
              result.error("UNEXPECTED_ERROR", "Unexpected error: ${e.message}", null)
            }
        }
    }.start()
  }

  private fun onGetPrinterInfo(@NonNull call: MethodCall, @NonNull result: Result) {
    var ipE: String? = call.argument("ip")
    var ipPort: Int? = call.argument("port")
    var ipAddress: String = ""
    var port: Int = TcpConnection.DEFAULT_ZPL_TCP_PORT
    if(ipE != null){
      ipAddress = ipE
    } else {
      result.error("PrintZPLOverTCPIP", "IP Address is required", "Data Content")
      return
    }
    if(ipPort != null){
      port = ipPort
    }
    val conn: Connection = createTcpConnect(ipAddress, port)
    try {
      // Open the connection - physical connection is established here.
      conn.open()
      // Send the data to printer as a byte array.
      val dataMap = DiscoveryUtil.getDiscoveryDataMap(conn)
      Log.d(logTag, "onGetIPInfo $dataMap")
      var resp = ZebreResult()
      resp.success = true
      resp.message= "Successfully!"
      var printer: ZebraPrinterInfo = ZebraPrinterInfo()
      printer.serialNumber = dataMap["SERIAL_NUMBER"]
      printer.address = dataMap["ADDRESS"]
      printer.availableInterfaces = dataMap["AVAILABLE_INTERFACES"]
      printer.availableLanguages = dataMap["AVAILABLE_LANGUAGES"]
      printer.darkness = dataMap["DARKNESS"]
      printer.jsonPortNumber = dataMap["JSON_PORT_NUMBER"]
      printer.productName = dataMap["PRODUCT_NAME"]
      resp.content = printer
      result.success(resp.toJSON())
    } catch (e: ConnectionException) {
      // Handle communications error here.
      e.printStackTrace()
      result.error("Error", "onPrintZPLOverTCPIP", e)
    } finally {
      // Close the connection to release resources.
      conn.close()
    }
  }

  private fun isPrinterConnected(@NonNull call: MethodCall, @NonNull result: Result) {
    var ipE: String? = call.argument("ip")
    var ipPort: Int? = call.argument("port")
    var ipAddress: String = ""
    var port: Int = TcpConnection.DEFAULT_ZPL_TCP_PORT
    if(ipE != null){
      ipAddress = ipE
    } else {
      result.error("isPrinterConnected", "IP Address is required", "Data Content")
      return
    }
    if(ipPort != null){
      port = ipPort
    }
    val conn: Connection = createTcpConnect(ipAddress, port)
    var resp = ZebreResult()
    try {
      // Open the connection - physical connection is established here.
      conn.open()
      // Send the data to printer as a byte array.
      val dataMap = DiscoveryUtil.getDiscoveryDataMap(conn)
      Log.d(logTag, "onGetIPInfo $dataMap")
      var isConnected: Boolean = conn.isConnected
      resp.success = isConnected
      resp.message =  "Unconnected"
      if(isConnected){
        resp.message =  "Connected"
      }
      result.success(resp.toJSON())
    } catch (e: ConnectionException) {
      // Handle communications error here.
      e.printStackTrace()
      resp.success = false
      resp.message =  "Unconnected"
      result.success(resp.toJSON())
//      result.error("Error", "onPrintZPLOverTCPIP", e)
    } finally {
      // Close the connection to release resources.
      conn.close()
    }
  }

  private fun onDiscovery(@NonNull call: MethodCall, @NonNull result: Result) {
    var handleNet = object : DiscoveryHandler {
      override fun foundPrinter(p0: DiscoveredPrinter) {
        Log.d(logTag, "foundPrinter $p0")
        var dataMap = p0.discoveryDataMap
        var address = dataMap["ADDRESS"]
        var isExist = printers.any { s -> s.address == address }
        if(!isExist){
          var printer: ZebraPrinterInfo = ZebraPrinterInfo()
          printer.serialNumber = dataMap["SERIAL_NUMBER"]
          printer.address = address
          printer.availableInterfaces = dataMap["AVAILABLE_INTERFACES"]
          printer.availableLanguages = dataMap["AVAILABLE_LANGUAGES"]
          printer.darkness = dataMap["DARKNESS"]
          printer.jsonPortNumber = dataMap["JSON_PORT_NUMBER"]
          printer.productName = dataMap["PRODUCT_NAME"]
          printers.add(printer)
        }
      }

      override fun discoveryFinished() {
        Log.d(logTag, "discoveryFinished $printers")
        var resp = ZebreResult()
        resp.success = true
        resp.message= "Successfully!"
        var printersJSON = Gson().toJson(printers)
        resp.content = printersJSON
        result.success(resp.toJSON())
      }

      override fun discoveryError(p0: String?) {
        Log.d(logTag, "discoveryError $p0")
        result.error("discoveryError", "discoveryError", p0)
      }
    }
    try {
      printers.clear()
      NetworkDiscoverer.findPrinters(handleNet)
    } catch (e: Exception) {
      e.printStackTrace()
      result.error("Error", "onDiscovery", e)
    }
     var net =  DiscoveredPrinterNetwork("a", 1)

  }


  private fun onDiscoveryUSB(@NonNull call: MethodCall, @NonNull result: Result) {
    var handleNet = object : DiscoveryHandler {
      override fun foundPrinter(p0: DiscoveredPrinter) {
        Log.d(logTag, "foundPrinter $p0")
        var dataMap = p0.discoveryDataMap
        var address = dataMap["ADDRESS"]
        var isExist = printers.any { s -> s.address == address }
        if(!isExist){
          var printer: ZebraPrinterInfo = ZebraPrinterInfo()
          printer.serialNumber = dataMap["SERIAL_NUMBER"]
          printer.address = address
          printer.availableInterfaces = dataMap["AVAILABLE_INTERFACES"]
          printer.availableLanguages = dataMap["AVAILABLE_LANGUAGES"]
          printer.darkness = dataMap["DARKNESS"]
          printer.jsonPortNumber = dataMap["JSON_PORT_NUMBER"]
          printer.productName = dataMap["PRODUCT_NAME"]
          printers.add(printer)
        }
      }

      override fun discoveryFinished() {
        Log.d(logTag, "discoveryUSBFinished $printers")
        var resp = ZebreResult()
        resp.success = true
        resp.message= "Successfully!"
        var printersJSON = Gson().toJson(printers)
        resp.content = printersJSON
        result.success(resp.toJSON())
      }

      override fun discoveryError(p0: String?) {
        Log.d(logTag, "discoveryUSBError $p0")
        result.error("discoveryUSBError", "discoveryUSBError", p0)
      }
    }
    try {
      printers.clear()
      UsbDiscoverer.findPrinters(context, handleNet)
    } catch (e: Exception) {
      e.printStackTrace()
      result.error("Error", "onDiscoveryUSB", e)
    }

  }


}

class BluetoothService(context: Context?, private val mHandler: Handler) {
  private val mAdapter = BluetoothAdapter.getDefaultAdapter()
  private var mAcceptThread: AcceptThread? = null
  private var mConnectThread: ConnectThread? = null
  private var mConnectedThread: ConnectedThread? = null
  private var mState = 0

  @get:Synchronized
  val isAvailable: Boolean
    get() = mAdapter != null

  @get:Synchronized
  val isBTopen: Boolean
    get() = mAdapter!!.isEnabled

  @Synchronized
  fun getDevByMac(mac: String?): BluetoothDevice {
    return mAdapter!!.getRemoteDevice(mac)
  }

  @Synchronized
  fun getDevByName(name: String?): BluetoothDevice? {
    var tem_dev: BluetoothDevice? = null
    val pairedDevices: Set<*>? = pairedDev
    if (pairedDevices!!.size > 0) {
      val var5 = pairedDevices.iterator()
      while (var5.hasNext()) {
        val device = var5.next() as BluetoothDevice
        if (device.name.indexOf(name!!) != -1) {
          tem_dev = device
          break
        }
      }
    }
    return tem_dev
  }

  @Synchronized
  fun sendMessage(message: String, charset: String?) {
    if (message.length > 0) {
      val send: ByteArray
      send = try {
        message.toByteArray(charset(charset!!))
      } catch (var5: UnsupportedEncodingException) {
        message.toByteArray()
      }
      write(send)
      val tail = byteArrayOf(10, 13, 0)
      write(tail)
    }
  }

  @get:Synchronized
  val pairedDev: Set<BluetoothDevice>?
    get() {
      var dev: Set<*>? = null
      dev = mAdapter!!.bondedDevices
      return dev
    }

  @Synchronized
  fun cancelDiscovery(): Boolean {
    return mAdapter!!.cancelDiscovery()
  }

  @get:Synchronized
  val isDiscovering: Boolean
    get() = mAdapter!!.isDiscovering

  @Synchronized
  fun startDiscovery(): Boolean {
    return mAdapter!!.startDiscovery()
  }

  @get:Synchronized
  @set:Synchronized
  var state: Int
    get() = mState
    private set(state) {
      mState = state
      mHandler.obtainMessage(1, state, -1).sendToTarget()
    }

  @Synchronized
  fun start() {
    Log.d("BluetoothService", "start")
    if (mConnectThread != null) {
      mConnectThread!!.cancel()
      mConnectThread = null
    }
    if (mConnectedThread != null) {
      mConnectedThread!!.cancel()
      mConnectedThread = null
    }
    if (mAcceptThread == null) {
      mAcceptThread = AcceptThread()
      mAcceptThread!!.start()
    }
    state = 1
  }

  @Synchronized
  fun connect(device: BluetoothDevice) {
    Log.d("BluetoothService", "connect to: $device")
    if (mState == 2 && mConnectThread != null) {
      mConnectThread!!.cancel()
      mConnectThread = null
    }
    if (mConnectedThread != null) {
      mConnectedThread!!.cancel()
      mConnectedThread = null
    }
    mConnectThread = ConnectThread(device)
    mConnectThread!!.start()
    state = 2
  }

  @Synchronized
  fun connected(socket: BluetoothSocket?, device: BluetoothDevice?) {
    Log.d("BluetoothService", "connected")
    if (mConnectThread != null) {
      mConnectThread!!.cancel()
      mConnectThread = null
    }
    if (mConnectedThread != null) {
      mConnectedThread!!.cancel()
      mConnectedThread = null
    }
    if (mAcceptThread != null) {
      mAcceptThread!!.cancel()
      mAcceptThread = null
    }
    mConnectedThread = ConnectedThread(socket)
    mConnectedThread!!.start()
    val msg = mHandler.obtainMessage(4)
    mHandler.sendMessage(msg)
    state = 3
  }

  @Synchronized
  fun stop() {
    Log.d("BluetoothService", "stop")
    state = 0
    if (mConnectThread != null) {
      mConnectThread!!.cancel()
      mConnectThread = null
    }
    if (mConnectedThread != null) {
      mConnectedThread!!.cancel()
      mConnectedThread = null
    }
    if (mAcceptThread != null) {
      mAcceptThread!!.cancel()
      mAcceptThread = null
    }
  }

  fun write(out: ByteArray?) {
    var r: ConnectedThread?
    synchronized(this) {
      if (mState != 3) {
        return
      }
      r = mConnectedThread
    }
    r!!.write(out)
  }

  private fun connectionFailed() {
    state = 1
    val msg = mHandler.obtainMessage(6)
    mHandler.sendMessage(msg)
  }

  private fun connectionLost() {
    val msg = mHandler.obtainMessage(5)
    mHandler.sendMessage(msg)
  }

  private inner class AcceptThread : Thread() {
    private val mmServerSocket: BluetoothServerSocket?

    init {
      var tmp: BluetoothServerSocket? = null
      try {
        tmp = mAdapter!!.listenUsingRfcommWithServiceRecord("BTPrinter", MY_UUID)
      } catch (var4: IOException) {
        Log.e("BluetoothService", "listen() failed", var4)
      }
      mmServerSocket = tmp
    }

    override fun run() {
      Log.d("BluetoothService", "BEGIN mAcceptThread$this")
      this.name = "AcceptThread"
      var socket: BluetoothSocket? = null
      while (mState != 3) {
        Log.d("AcceptThread线程运行", "正在运行......")
        socket = try {
          mmServerSocket!!.accept()
        } catch (var6: IOException) {
          Log.e("BluetoothService", "accept() failed", var6)
          break
        }
        if (socket != null) {
          val e = this@BluetoothService
          synchronized(this@BluetoothService) {
            when (mState) {
              0, 3 -> try {
                socket.close()
              } catch (var4: IOException) {
                Log.e(
                  "BluetoothService",
                  "Could not close unwanted socket",
                  var4
                )
              }

              1, 2 -> connected(socket, socket.remoteDevice)
              else -> {}
            }
          }
        }
      }
      Log.i("BluetoothService", "END mAcceptThread")
    }

    fun cancel() {
      Log.d("BluetoothService", "cancel $this")
      try {
        mmServerSocket!!.close()
      } catch (var2: IOException) {
        Log.e("BluetoothService", "close() of server failed", var2)
      }
    }
  }

  private inner class ConnectThread(private val mmDevice: BluetoothDevice) :
    Thread() {
    private val mmSocket: BluetoothSocket?

    init {
      var tmp: BluetoothSocket? = null
      try {
        tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID)
      } catch (var5: IOException) {
        Log.e("BluetoothService", "create() failed", var5)
      }
      mmSocket = tmp
    }

    override fun run() {
      Log.i("BluetoothService", "BEGIN mConnectThread")
      this.name = "ConnectThread"
      mAdapter!!.cancelDiscovery()
      try {
        mmSocket!!.connect()
      } catch (var5: IOException) {
        connectionFailed()
        try {
          mmSocket!!.close()
        } catch (var3: IOException) {
          Log.e("BluetoothService", "unable to close() socket during connection failure", var3)
        }
        this@BluetoothService.start()
        return
      }
      val e = this@BluetoothService
      synchronized(this@BluetoothService) {
        mConnectThread = null
      }
      connected(mmSocket, mmDevice)
    }

    fun cancel() {
      try {
        mmSocket!!.close()
      } catch (var2: IOException) {
        Log.e("BluetoothService", "close() of connect socket failed", var2)
      }
    }
  }

  private inner class ConnectedThread(socket: BluetoothSocket?) :
    Thread() {
    private val mmSocket: BluetoothSocket?
    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?

    init {
      Log.d("BluetoothService", "create ConnectedThread")
      mmSocket = socket
      var tmpIn: InputStream? = null
      var tmpOut: OutputStream? = null
      try {
        tmpIn = socket!!.inputStream
        tmpOut = socket.outputStream
      } catch (var6: IOException) {
        Log.e("BluetoothService", "temp sockets not created", var6)
      }
      mmInStream = tmpIn
      mmOutStream = tmpOut
    }

    override fun run() {
      Log.d("ConnectedThread线程运行", "正在运行......")
      Log.i("BluetoothService", "BEGIN mConnectedThread")
      try {
        while (true) {
          val e = ByteArray(256)
          val bytes = mmInStream!!.read(e)
          if (bytes <= 0) {
            Log.e("BluetoothService", "disconnected")
            connectionLost()
            if (mState != 0) {
              Log.e("BluetoothService", "disconnected")
              this@BluetoothService.start()
            }
            break
          }
          mHandler.obtainMessage(2, bytes, -1, e).sendToTarget()
        }
      } catch (var3: IOException) {
        Log.e("BluetoothService", "disconnected", var3)
        connectionLost()
        if (mState != 0) {
          this@BluetoothService.start()
        }
      }
    }

    fun write(buffer: ByteArray?) {
      try {
        mmOutStream!!.write(buffer)
        mHandler.obtainMessage(3, -1, -1, buffer).sendToTarget()
      } catch (var3: IOException) {
        Log.e("BluetoothService", "Exception during write", var3)
      }
    }

    fun cancel() {
      try {
        mmSocket!!.close()
      } catch (var2: IOException) {
        Log.e("BluetoothService", "close() of connect socket failed", var2)
      }
    }
  }

  companion object {
    private const val TAG = "BluetoothService"
    private const val D = true
    const val MESSAGE_STATE_CHANGE = 1
    const val MESSAGE_READ = 2
    const val MESSAGE_WRITE = 3
    const val MESSAGE_DEVICE_NAME = 4
    const val MESSAGE_CONNECTION_LOST = 5
    const val MESSAGE_UNABLE_CONNECT = 6
    private const val NAME = "BTPrinter"
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    const val STATE_NONE = 0
    const val STATE_LISTEN = 1
    const val STATE_CONNECTING = 2
    const val STATE_CONNECTED = 3
  }
}
