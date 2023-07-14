package com.tlt.flutter_zebra_sdk

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.zebra.sdk.btleComm.BluetoothLeConnection
import com.zebra.sdk.comm.BluetoothConnectionInsecure
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.comm.TcpConnection
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveredPrinterNetwork
import com.zebra.sdk.printer.discovery.DiscoveryException
import com.zebra.sdk.printer.discovery.DiscoveryHandler
import com.zebra.sdk.printer.discovery.DiscoveryUtil
import com.zebra.sdk.printer.discovery.NetworkDiscoverer
import com.zebra.sdk.printer.discovery.UsbDiscoverer
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.IOException
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
        "printZPLOverTCPIP" -> {
          onPrintZPLOverTCPIP(call, result)
        }
        "printZPLOverBluetooth" -> {
          onPrintZplDataOverBluetooth(call, result)
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

        "printZPLOverBluetoothInsecure" -> {
          onPrintZplDataOverBluetoothInsecure(call, result)
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

  private fun onPrintZPLOverTCPIP(@NonNull call: MethodCall, @NonNull result: Result) {
    var ipE: String? = call.argument("ip")
    var data: String? = call.argument("data")
    var rep = HashMap<String, Any>()
    var ipAddress: String = ""
    if(ipE != null){
      ipAddress = ipE
    } else {
      result.error("PrintZPLOverTCPIP", "IP Address is required", "Data Content")
      return
    }
    val conn: Connection = createTcpConnect(ipAddress, TcpConnection.DEFAULT_ZPL_TCP_PORT)
    Log.d(logTag, "onPrintZPLOverTCPIP $ipAddress $data ${TcpConnection.DEFAULT_ZPL_TCP_PORT}")
    if (data == null) {
      result.error("PrintZPLOverTCPIP", "Data is required", "Data Content")
    }
    try {
      // Open the connection - physical connection is established here.
      conn.open()
      // Send the data to printer as a byte array.
      conn.write(data?.toByteArray())
      rep["success"] = true
      rep["message"] = "Successfully!"
      result.success(rep.toString())
    } catch (e: ConnectionException) {
      // Handle communications error here.
      e.printStackTrace()
      result.error("Error", "onPrintZPLOverTCPIP", e)
    } finally {
      // Close the connection to release resources.
      conn.close()
    }
  }

  class BluetoothBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      Log.d("BluetoothReceiver", "${intent?.action}")

      when (intent?.action) {
        BluetoothDevice.ACTION_ACL_CONNECTED -> {
          val device: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          Log.d("BluetoothReceiver", "BluetoothDevice ${device.name} connected")
        }
        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
          val device: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          Log.d("BluetoothReceiver", "BluetoothDevice ${device.name} disconnected")
        }

      }
    }
  }

  private fun onPrintZplDataOverBluetooth(@NonNull call: MethodCall, @NonNull result: Result) {
    var macAddress: String? = call.argument("mac")
    var data: String? = call.argument("data")
    var textToPrint = "! 0 200 200 210 1\r\n" +
            "TEXT 4 0 30 40 This is a CPCL test.\r\n" +
            "FORM\r\n" +
            "PRINT\r\n"
    Log.d(logTag, "onPrintZplDataOverBluetooth $macAddress $data")
    if (data == null) {
      result.error("onPrintZplDataOverBluetooth", "Data is required", "Data Content")
    }
    Thread {
      var conn: BluetoothLeConnection? = null
      val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
      bluetoothAdapter.cancelDiscovery()
      try {
        val bluetoothDevice: BluetoothDevice =
          bluetoothAdapter.getRemoteDevice("70:B9:50:8C:C3:07")
        if (bluetoothAdapter.isEnabled) {
          Log.d(logTag, "${bluetoothDevice.bondState}")
          // Conexion comun a cualquier dispositivo bluetooth
          Log.d(logTag, "${bluetoothDevice.name} ${bluetoothDevice.address}")
          // Creacion del objeto discoveredPrinter sin un método discoverer, copiado del sdk .jar
          val printer: DiscoveredPrinter? = reflectivelyInstatiateDiscoveredPrinterBluetoothLe(
            bluetoothDevice.address,
            bluetoothDevice.name ?: "zebraPrinter",
            context
          )
          // Conexion comun a cualquier dispositivo bluetooth
          conn = BluetoothLeConnection(printer?.address,context)
          conn.open()
          if (conn.isConnected) {
            conn.write(textToPrint.toByteArray())
            Thread.sleep(500)
          }
        }
      } catch (e: ConnectionException) {
        e.printStackTrace()
        Handler(Looper.getMainLooper()).post {
            result.error("CONNECTION_ERROR", "Error connecting to device: ${e.message}", null)
        }
      } finally {
        try {
          conn?.close()
        } catch (e: ConnectionException) {
          e.printStackTrace()
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

  private fun onPrintZplDataOverBluetoothInsecure(@NonNull call: MethodCall, @NonNull result: Result) {
    var macAddress: String? = call.argument("mac")
    var data: String? = call.argument("data")
    Log.d(logTag, "onPrintZplDataOverBluetooth $macAddress $data")
    if (data == null) {
      result.error("onPrintZplDataOverBluetooth", "Data is required", "Data Content")
    }
    Thread {
        try {

            // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
            val thePrinterConn: Connection = BluetoothConnectionInsecure(macAddress)

            // Initialize
            Looper.prepare()

            // Open the connection - physical connection is established here.
            thePrinterConn.open()

            // This example prints "This is a ZPL test." near the top of the label.
            val zplData = "^XA^FO20,20^A0N,25,25^FDThis is a ZPL test.^FS^XZ"

            // Send the data to printer as a byte array.
            thePrinterConn.write(zplData.toByteArray())

            // Make sure the data got to the printer before closing the connection
            Thread.sleep(500)

            // Close the insecure connection to release resources.
            thePrinterConn.close()

            Looper.myLooper()?.quit()
        } catch (e: Exception) {
            // Handle communications error here.
            e.printStackTrace()
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
