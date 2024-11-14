import 'package:flutter/material.dart';
import 'package:arcore_flutter_plugin/arcore_flutter_plugin.dart';
import 'package:vector_math/vector_math_64.dart' as vector;

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ARCore Corner Detection Demo',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: ARCoreExample(),
    );
  }
}

class ARCoreExample extends StatefulWidget {
  @override
  _ARCoreExampleState createState() => _ARCoreExampleState();
}

class _ARCoreExampleState extends State<ARCoreExample> {
  late ArCoreController arCoreController;

  @override
  void dispose() {
    arCoreController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('ARCore Corner Detection Example')),
      body: ArCoreView(
        onArCoreViewCreated: _onArCoreViewCreated,
        enableTapRecognizer: true,
      ),
    );
  }

  void _onArCoreViewCreated(ArCoreController controller) {
    arCoreController = controller;
    arCoreController.onPlaneTap = _onPlaneTapHandler;
  }

  void _onPlaneTapHandler(List<ArCoreHitTestResult> hits) {
    final hit = hits.first;
    _addCube(hit);
    _detectCorners(hit);
  }

  void _addCube(ArCoreHitTestResult hit) {
    final material = ArCoreMaterial(color: Colors.red);
    final cube = ArCoreCube(
      size: vector.Vector3(0.2, 0.2, 0.2),
      materials: [material],
    );

    final node = ArCoreNode(
      shape: cube,
      position: hit.pose.translation,
      rotation: hit.pose.rotation,
    );

    arCoreController.addArCoreNodeWithAnchor(node);
  }

  void _detectCorners(ArCoreHitTestResult hit) {
    final sphereMaterial = ArCoreMaterial(color: Colors.blue);
    final sphere = ArCoreSphere(
      radius: 0.02, // Small radius for corner markers
      materials: [sphereMaterial],
    );

    // Define small offsets to simulate corners around the tapped point
    final offsets = [
      vector.Vector3(0.1, 0.0, 0.1),
      vector.Vector3(-0.1, 0.0, 0.1),
      vector.Vector3(0.1, 0.0, -0.1),
      vector.Vector3(-0.1, 0.0, -0.1),
    ];

    // Add a sphere at each offset position around the hit point
    for (var offset in offsets) {
      final position = hit.pose.translation + offset;
      final cornerNode = ArCoreNode(
        shape: sphere,
        position: position,
      );
      arCoreController.addArCoreNodeWithAnchor(cornerNode);
    }
  }
}
