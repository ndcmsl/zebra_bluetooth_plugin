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
    data =
        '^XA ^FX Top section with logo, name and address. ^CF0,60 ^FO50,50^GB100,100,100^FS ^FO75,75^FR^GB100,100,100^FS ^FO93,93^GB40,40,40^FS ^FO220,50^FDIntershipping, Inc.^FS ^CF0,30 ^FO220,115^FD1000 Shipping Lane^FS ^FO220,155^FDShelbyville TN 38102^FS ^FO220,195^FDUnited States (USA)^FS ^FO50,250^GB700,3,3^FS ^FX Second section with recipient address and permit information. ^CFA,30 ^FO50,300^FDJohn Doe^FS ^FO50,340^FD100 Main Street^FS ^FO50,380^FDSpringfield TN 39021^FS ^FO50,420^FDUnited States (USA)^FS ^CFA,15 ^FO600,300^GB150,150,3^FS ^FO638,340^FDPermit^FS ^FO638,390^FD123456^FS ^FO50,500^GB700,3,3^FS ^FX Third section with bar code. ^BY5,2,270 ^FO100,550^BC^FD12345678^FS ^FX Fourth section (the two boxes on the bottom). ^FO50,900^GB700,250,3^FS ^FO400,900^GB3,250,3^FS ^CF0,40 ^FO100,960^FDCtr. X34B-1^FS ^FO100,1010^FDREF1 F00B47^FS ^FO100,1060^FDREF2 BL4H8^FS ^CF0,190 ^FO470,955^FDCA^FS ^XZ';

    String arr = '00:07:4D:DE:75:72';
    if (Platform.isIOS) {
      arr = '50J171201608';
    }
    final rep = ZebraSdk.printZPLOverBluetooth(arr, data: data);
    print(rep);
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
    final rep = ZebraSdk.printZPLOverBluetoothInsecure(arr, data: data);
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
                TextButton(onPressed: onTestBluetooth, child: Text('Print Bluetooth')),
                TextButton(onPressed: onTestBluetoothInsecure, child: Text('Print Bluetooth Insecure')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
