package com.tlt.flutter_zebra_sdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
/*

class BluetoothService(context: Context?) {
    private val mAdapter: BluetoothAdapter
    private var mConnectedThread: ConnectedThread? = null

    @get:Synchronized
    var state: Int
        private set

    init {
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        state = STATE_NONE
    }

    @Synchronized
    private fun setState(state: Int, bundle: Map<String, Any>?) {
        if (DEBUG) Log.d(
            TAG, "setState() " + getStateName(
                this.state
            ) + " -> " + getStateName(state)
        )
        this.state = state
        infoObervers(state, bundle)
    }

    private fun getStateName(state: Int): String {
        var name = "UNKNOW:$state"
        if (STATE_NONE == state) {
            name = "STATE_NONE"
        } else if (STATE_CONNECTED == state) {
            name = "STATE_CONNECTED"
        } else if (STATE_CONNECTING == state) {
            name = "STATE_CONNECTING"
        }
        return name
    }

    @Synchronized
    fun stop() {
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
    }

    @Synchronized
    fun connect(device: BluetoothDevice) {
        if (DEBUG) Log.d(TAG, "connect to: $device")
        var connectedDevice: BluetoothDevice? = null
        if (mConnectedThread != null) {
            connectedDevice = mConnectedThread!!.bluetoothDevice()
        }
        if (state == STATE_CONNECTED && connectedDevice != null && connectedDevice.address == device.address) {
            // connected already
            val bundle: MutableMap<String, Any> = HashMap()
            bundle[DEVICE_NAME] = device.name
            bundle[DEVICE_ADDRESS] = device.address
            setState(STATE_CONNECTED, bundle)
        } else {
            // Cancel any thread currently running a connection
            stop()
            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = ConnectedThread(device)
            mConnectedThread!!.start()
            setState(STATE_CONNECTING, null)
        }
    }

    private fun connectionFailed() {
        setState(STATE_NONE, null)
        infoObervers(MESSAGE_UNABLE_CONNECT, null)
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        setState(STATE_NONE, null)
        infoObervers(MESSAGE_CONNECTION_LOST, null)
    }

    private inner class ConnectedThread(private val mmDevice: BluetoothDevice) : Thread() {
        private var mmSocket: BluetoothSocket? = null
        private var mmInStream: InputStream? = null
        private var mmOutStream: OutputStream? = null
        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread")
            name = "ConnectThread"
            var bundle: MutableMap<String, Any> = HashMap()

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery()
            var tmp: BluetoothSocket? = null

            // try to connect with socket inner method firstly.
            for (i in 1..3) {
                try {
                    tmp = mmDevice.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )
                        .invoke(mmDevice, i) as BluetoothSocket
                } catch (e: Exception) {
                }
                if (tmp != null) {
                    mmSocket = tmp
                    break
                }
            }

            // try with given uuid
            if (mmSocket == null) {
                try {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(TAG, "create() failed", e)
                }
                if (tmp == null) {
                    Log.e(TAG, "create() failed: Socket NULL.")
                    connectionFailed()
                    return
                }
            }
            mmSocket = tmp

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
            } catch (e: Exception) {
                e.printStackTrace()
                connectionFailed()
                // Close the socket
                try {
                    mmSocket!!.close()
                } catch (e2: Exception) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2)
                }
                return
            }
            Log.d(TAG, "create ConnectedThread")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket!!.inputStream
                tmpOut = mmSocket!!.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
            bundle[DEVICE_NAME] = mmDevice.name
            bundle[DEVICE_ADDRESS] = mmDevice.address
            setState(STATE_CONNECTED, bundle)
            Log.i(TAG, "Connected")
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    val buffer = ByteArray(256)
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)
                    if (bytes > 0) {
                        // Send the obtained bytes to the UI Activity
                        bundle = HashMap()
                        bundle["bytes"] = bytes
                        infoObervers(MESSAGE_READ, bundle)
                    } else {
                        Log.e(TAG, "disconnected")
                        connectionLost()
                        break
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
            Log.i(TAG, "ConnectedThread End")
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray) {
            try {
                mmOutStream!!.write(buffer)
                mmOutStream!!.flush() // 清空缓存
                /*
                 * if (buffer.length > 3000) // { byte[] readata = new byte[1];
                 * SPPReadTimeout(readata, 1, 5000); }
                 */Log.i("BTPWRITE", String(buffer, "GBK"))
                val bundle: MutableMap<String, Any> = HashMap()
                bundle["bytes"] = buffer
                infoObervers(MESSAGE_WRITE, bundle)
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun bluetoothDevice(): BluetoothDevice? {
            return if (mmSocket != null && mmSocket!!.isConnected) {
                mmSocket!!.remoteDevice
            } else {
                null
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
                connectionLost()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothService"
        private const val DEBUG = true
        private const val NAME = "BTPrinter"
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val STATE_NONE = 0 // we're doing nothing

        // public static final int STATE_LISTEN = 1; // now listening for incoming
        // connections //feathure removed.
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3 // now connected to a remote device
        const val MESSAGE_STATE_CHANGE = 4
        const val MESSAGE_READ = 5
        const val MESSAGE_WRITE = 6
        const val MESSAGE_DEVICE_NAME = 7
        const val MESSAGE_CONNECTION_LOST = 8
        const val MESSAGE_UNABLE_CONNECT = 9

        // Key names received from the BluetoothService Handler
        const val DEVICE_NAME = "device_name"
        const val DEVICE_ADDRESS = "device_address"
        const val TOAST = "toast"
        var ErrorMessage = "No_Error_Message"
    }
}
*/