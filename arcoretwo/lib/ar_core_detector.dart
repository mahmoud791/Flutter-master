import 'package:flutter/services.dart';

class ARCoreDetector {
  static const MethodChannel _channel = MethodChannel('arcore_corner_detection');

  static Future<bool> detectCorner() async {
    try {
      final bool cornerDetected = await _channel.invokeMethod('detectCorner');
      return cornerDetected;
    } catch (e) {
      print("Error detecting corner: $e");
      return false;
    }
  }
}
