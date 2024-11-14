import 'package:flutter/material.dart';
import 'ar_core_detector.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: CornerDetectionScreen(),
    );
  }
}

class CornerDetectionScreen extends StatefulWidget {
  @override
  _CornerDetectionScreenState createState() => _CornerDetectionScreenState();
}

class _CornerDetectionScreenState extends State<CornerDetectionScreen> {
  bool _cornerDetected = false;

  void _checkForCorner() async {
    final cornerDetected = await ARCoreDetector.detectCorner();
    setState(() {
      _cornerDetected = cornerDetected;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Corner Detection")),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              _cornerDetected ? "Corner Detected!" : "No Corner Detected",
              style: TextStyle(fontSize: 24),
            ),
            ElevatedButton(
              onPressed: _checkForCorner,
              child: Text("Check for Corner"),
            ),
          ],
        ),
      ),
    );
  }
}
