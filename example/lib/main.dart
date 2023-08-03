import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';

// import 'package:flutter/services.dart';
import 'package:flutter_zebra_sdk/flutter_zebra_sdk.dart';

void main() {
  runApp(MaterialApp(home: MyApp()));
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
    initial();
  }

  void initial() async {
    // await Permission.
  }

  Future _ackAlert(BuildContext context, String title) async {
    return showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          // content: const Text('This item is no longer available'),
          actions: [
            TextButton(
              child: Text('Ok'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  Future<void> onDiscovery() async {
    var a = await ZebraSdk.onDiscovery();
    print(a);
    var b = json.decode(a);

    var printers = b['content'];
    if (printers != null) {
      var printObj = json.decode(printers);
      print(printObj);
    }

    print(b);
  }

  Future<void> onDiscoveryUSB(dynamic context) async {
    var a = await ZebraSdk.onDiscoveryUSB();
    _ackAlert(context, 'USB $a');
    print(a);
    var b = json.decode(a);

    var printers = b['content'];
    if (printers != null) {
      var printObj = json.decode(printers);
      print(printObj);
    }
    print(b);
  }

  Future<void> onGetIPInfo() async {
    var a = await ZebraSdk.onGetPrinterInfo('192.168.1.26');
    print(a);
  }

  Future<void> onTestConnect() async {
    var a = await ZebraSdk.isPrinterConnected('192.168.1.26');
    print(a);
    var b = json.decode(a);
    print(b);
  }

  Future<void> onTestTCP() async {
    String data;
    data = '''
    ''
    ^XA~TA000~JSN^LT0^MNW^MTT^PON^PMN^LH0,0^JMA^PR6,6~SD15^JUS^LRN^CI0^XZ
    ^XA
    ^MMT
    ^PW500
    ^LL0240
    ^LS0
    ^FT144,33^A0N,25,24^FB111,1,0,C^FH\^FDITEM TITLE^FS
    ^FT3,61^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDOption 1, Option 2, Option 3, Option 4, Opt^FS^CI0
    ^FT3,84^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDion 5, Option 6 ^FS^CI0
    ^FT34,138^A@N,25,24,TT0003M_^FB331,1,0,C^FH\^CI17^F8^FDOrder: https://eat.chat/phobac^FS^CI0
    ^FT29,173^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FDPromotional Promotional Promotional^FS^CI0
    ^FT29,193^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FD Promotional Promotional ^FS^CI0
    ^FT106,233^A0N,25,24^FB188,1,0,C^FH\^FDPHO BAC HOA VIET^FS
    ^PQ1,0,1,Y^XZ
        ''';

    final rep = ZebraSdk.printZPLOverTCPIP('192.168.1.26', data: data);
    print(rep);
  }

  Future<void> onTestBluetooth() async {
    String data;
    data = '''
      ^XA
      ^MMT
      ^PW832
      ^LL1206
      ^FT215,1174^A0B,214,264^FH^CI28^FD118.327^FS^
      ^FT309,1167^A0B,56,68^FH^CI28^FD10213 - 118327^FS^
      ^FT309,620^A0B,40,52^FH^CI28^FDTipo de palet desconocido^FS^
      ^FO230,10^BY2,3,10^BQN,2,5
      ^FH\^FDHA,{"root": [{"id_product": 10213,"id_product_attribute": 118327,"palet_name": "Tipo de palet desconocido","quantity": 5,"label_date": "2023-07-26T10:09:18.670847","label_guid": "2AA8fz2O4"}]}^FS
      ^FT131,374^A0B,102,134^FB374,1,26,C^FH^CI28^FD5^FS^CI27
      ^FT210,373^A0B,35,40^FB373,1,6,C^FH^CI28^FD2 A A 8 f z 2 O 4^FS^CI27
      ^FT781,1196^A0B,209,245^FB1196,1,38,C^FH^CI28^FDZ1.C5.A3^FS^
      ^FT611,800^A0B,20,32^FB1196,1,38,C^FH^CI28^FD26/07/2023 10:09^FS^
      ^FO596,400
      ^A0B,20,32
      ^FB760,1,,J,0
      ^FO340,400
      ^A0B,75,75
      ^FB760,3,,J,0
      ^FDSillón Colgante de Jardín Bahli  ^FS
      ^FO520,400
      ^A0B,45,45
      ^FB760,1,,J,0
      ^FDNegro^FS
      ^PQ1,0,1,Y
      ^JUS
      ^XZ
        ''';
    String arr = '00:07:4D:DE:75:72';
    if (Platform.isIOS) {
      arr = '50J171201608';
    }
    try {
      final rep = await ZebraSdk.printZPLOverBluetooth(data: utf8.encode(data));
      print('el booleano es: $rep');
    } catch (e) {
      print('llego al ultimo catch del codigo');
    }
  }

  Future<void> onTestBluetoothInsecure() async {
    String data;
    data = '''
    ''
    ^XA~TA000~JSN^LT0^MNW^MTT^PON^PMN^LH0,0^JMA^PR6,6~SD15^JUS^LRN^CI0^XZ
    ^XA
    ^MMC
    ^PW500
    ^LL0240
    ^LS0
    ^FT144,33^A0N,25,24^FB111,1,0,C^FH\^FDITEM TITLE^FS
    ^FT3,61^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDOption 1, Option 2, Option 3, Option 4, Opt^FS^CI0
    ^FT3,84^A@N,20,20,TT0003M_^FB394,1,0,C^FH\^CI17^F8^FDion 5, Option 6 ^FS^CI0
    ^FT34,138^A@N,25,24,TT0003M_^FB331,1,0,C^FH\^CI17^F8^FDOrder: https://eat.chat/phobac^FS^CI0
    ^FT29,173^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FDPromotional Promotional Promotional^FS^CI0
    ^FT29,193^A@N,20,20,TT0003M_^FB342,1,0,C^FH\^CI17^F8^FD Promotional Promotional ^FS^CI0
    ^FT106,233^A0N,25,24^FB188,1,0,C^FH\^FDPHO BAC HOA VIET^FS
    ^PQ1,0,1,Y^XZ
        ''';

    String arr = '00:07:4D:DE:75:72';
    if (Platform.isIOS) {
      arr = '50J171201608';
    }
    final rep = ZebraSdk.printZPLOverBluetoothInsecure(data: data);
    print(rep);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Center(
            child: Column(
              children: [
                TextButton(onPressed: onGetIPInfo, child: Text('onGetPrinterInfo')),
                TextButton(onPressed: onTestConnect, child: Text('onTestConnect')),
                TextButton(onPressed: onDiscovery, child: Text('Discovery')),
                TextButton(onPressed: () => onDiscoveryUSB(context), child: Text('Discovery USB')),
                TextButton(onPressed: onTestTCP, child: Text('Print TCP')),
                TextButton(onPressed: () async => await onTestBluetooth(), child: Text('Print Bluetooth')),
                TextButton(onPressed: onTestBluetoothInsecure, child: Text('Print Bluetooth Insecure')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
