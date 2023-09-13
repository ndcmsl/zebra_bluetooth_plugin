import 'dart:async';

import 'package:flutter/services.dart';

class ZebraSdk {
  static const MethodChannel _channel = const MethodChannel('flutter_zebra_sdk');

  static Future<String?> destroyBluetoothConnection() async {
    return await _channel.invokeMethod('destroyBluetoothConnection');
  }

  static Future<bool?> establishBluetoothConnection(String macAddress) async {
    try {
      final Map<String, dynamic> params = {"mac": macAddress};
      await _channel.invokeMethod('establishBluetoothConnection', params);
      return true;
    } on PlatformException catch (e) {
      return false;
    }
  }

  static Future<String?> printOverBluetooth(List<int>? data, int? copies) async {
    final Map<String, dynamic> params = {"data": data};
    if (data != null) {
      params['data'] = data;
    }
    if (copies != null) {
        params['copies'] = copies;
    }
    return await _channel.invokeMethod('printOverBluetooth', params);
  }

  static Future<dynamic> onDiscovery() async {
    final Map<String, dynamic> params = {};
    return await _channel.invokeMethod('onDiscovery', params);
  }

  static Future<dynamic> onDiscoveryUSB() async {
    final Map<String, dynamic> params = {};
    return await _channel.invokeMethod('onDiscoveryUSB', params);
  }

  static Future<dynamic> onGetPrinterInfo(String ip, {int? port}) async {
    final Map<String, dynamic> params = {"ip": ip};
    if (port != null) {
      params['port'] = port;
    }
    return await _channel.invokeMethod('onGetPrinterInfo', params);
  }

  static Future<dynamic> isPrinterConnected(String ip, {int? port}) async {
    final Map<String, dynamic> params = {"ip": ip};
    if (port != null) {
      params['port'] = port;
    }
    return await _channel.invokeMethod('isPrinterConnected', params);
  }
}
