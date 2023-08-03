import 'dart:async';

import 'package:flutter/services.dart';

class ZebraSdk {
  static const MethodChannel _channel = const MethodChannel('flutter_zebra_sdk');

  static Future<String?> printZPLOverTCPIP(String ipAddress, {int? port, String? data}) async {
    final Map<String, dynamic> params = {"ip": ipAddress};
    if (port != null) {
      params['port'] = port;
    }
    if (data != null) {
      params['data'] = data;
    }
    return await _channel.invokeMethod('printZPLOverTCPIP', params);
  }

  static Future<bool?> printZPLOverBluetooth({List<int>? data, int? copies}) async {
    try {
      final Map<String, dynamic> params = {};
      if (data != null) {
        params['data'] = data;
      }
      if (copies != null) {
        params['copies'] = copies;
      }
      await _channel.invokeMethod('printZPLOverBluetooth', params);
      return true;
    } on PlatformException catch (e) {
      return false;
    }
  }

  static Future<bool?> establishBluetoothConnection(String macAddress) async {
    try {
      final Map<String, dynamic> params = {"mac": macAddress};
      if (macAddress != null) {
        params['copies'] = macAddress;
      }
      return await _channel.invokeMethod('establishBluetoothConnection', params);
    } on PlatformException catch (e) {
      return false;
    }
  }

  static Future<bool?> destroyBluetoothConnection() async {
    try {
      return await _channel.invokeMethod('destroyBluetoothConnection');
    } on PlatformException catch (e) {
      return false;
    }
  }

  static Future<String?> printZPLOverBluetoothInsecure({String? data}) async {
    final Map<String, dynamic> params = {};
    if (data != null) {
      params['data'] = data;
    }
    return await _channel.invokeMethod('printZPLOverBluetoothInsecure', params);
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
