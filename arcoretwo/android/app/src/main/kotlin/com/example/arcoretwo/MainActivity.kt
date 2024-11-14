package com.example.arcore_corner_detection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "arcore_corner_detection"
    private val CAMERA_PERMISSION_CODE = 100
    private var arSession: Session? = null
    private lateinit var surfaceView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request camera permission
        if (!isCameraPermissionGranted()) {
            requestCameraPermission()
        } else {
            initializeARSession()
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "detectCorner") {
                Log.d("ARCore", "detectCorner method called")
                val success = detectCorner()
                result.success(success)
            } else {
                result.notImplemented()
            }
        }
    }

    // Check if camera permission is granted
    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // Request camera permission
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                initializeARSession()
            } else {
                Toast.makeText(this, "Camera permission is required for AR features", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Initialize AR session
    private fun initializeARSession() {
        try {
            arSession = Session(this)
            val config = Config(arSession)
            arSession?.configure(config)
            Toast.makeText(this, "AR Session initialized", Toast.LENGTH_SHORT).show()

            // Set up the SurfaceView for AR camera display
            surfaceView = SurfaceView(this)
            setContentView(surfaceView) // Display the SurfaceView to show camera feed

            // Connect the AR session to the SurfaceView
            surfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                    try {
                        // Set the AR session's surface
                        arSession?.setCameraTextureName(holder.surface)
                    } catch (e: Exception) {
                        Log.e("ARCore", "Error setting camera surface", e)
                    }
                }

                override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                    arSession?.pause()
                }
            })

        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e("ARCore", "ARCore not installed", e)
            Toast.makeText(this, "ARCore not installed", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e("ARCore", "Device not compatible with ARCore", e)
            Toast.makeText(this, "Device not compatible with ARCore", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableSdkTooOldException) {
            Log.e("ARCore", "ARCore SDK too old", e)
            Toast.makeText(this, "ARCore SDK too old", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableApkTooOldException) {
            Log.e("ARCore", "ARCore APK too old", e)
            Toast.makeText(this, "ARCore APK too old", Toast.LENGTH_LONG).show()
        }
    }

    // Detect corner in AR session
    private fun detectCorner(): Boolean {
        if (arSession == null) {
            Log.e("ARCore", "AR Session not initialized")
            Toast.makeText(this, "AR Session not initialized", Toast.LENGTH_SHORT).show()
            return false
        }

        // Ensure the session is resumed before updating
        if (arSession?.isPaused == true) {
            try {
                Log.d("ARCore", "Resuming AR session")
                arSession?.resume() // Try to resume the session if it's paused
            } catch (e: Exception) {
                Log.e("ARCore", "Failed to resume AR session", e)
                return false
            }
        }

        try {
            // Update the AR session and get the frame
            val frame = arSession?.update() ?: return false

            // Get updated trackables (planes)
            val planes = frame.getUpdatedTrackables(Plane::class.java)

            // Find the Z-plane (vertical plane)
            val zPlane = planes.firstOrNull { isVerticalPlane(it) }
            if (zPlane != null) {
                // Identify perpendicular X and Y planes
                val xPlane = planes.firstOrNull { plane -> isPerpendicularPlane(plane, zPlane) }
                val yPlane = planes.firstOrNull { plane -> plane != xPlane && isPerpendicularPlane(plane, zPlane) }

                // Check if X and Y planes are perpendicular to each other
                if (xPlane != null && yPlane != null && arePlanesPerpendicular(xPlane, yPlane)) {
                    Log.d("ARCore", "Corner detected")
                    return true // Corner detected
                }
            }
        } catch (e: SessionPausedException) {
            Log.e("ARCore", "AR Session is paused. Cannot update frame.", e)
        } catch (e: Exception) {
            Log.e("ARCore", "Error updating AR session", e)
        }

        Log.d("ARCore", "No corner detected")
        return false // No corner detected
    }

    // Check if the plane is vertical
    private fun isVerticalPlane(plane: Plane): Boolean {
        val normal = plane.centerPose.yAxis
        return Math.abs(normal[2]) > 0.9
    }

    // Check if the plane is perpendicular to a reference plane
    private fun isPerpendicularPlane(plane1: Plane, referencePlane: Plane): Boolean {
        val normal1 = plane1.centerPose.yAxis
        val normal2 = referencePlane.centerPose.yAxis
        val dotProduct = normal1[0] * normal2[0] + normal1[1] * normal2[1] + normal1[2] * normal2[2]
        return Math.abs(dotProduct) < 0.1
    }

    // Check if two planes are perpendicular
    private fun arePlanesPerpendicular(plane1: Plane, plane2: Plane): Boolean {
        val normal1 = plane1.centerPose.yAxis
        val normal2 = plane2.centerPose.yAxis
        val dotProduct = normal1[0] * normal2[0] + normal1[1] * normal2[1] + normal1[2] * normal2[2]
        return Math.abs(dotProduct) < 0.1
    }

    // Handle the AR session lifecycle
    override fun onResume() {
        super.onResume()
        try {
            if (arSession?.isPaused == true) {
                Log.d("ARCore", "Resuming AR session onResume")
                arSession?.resume()
            }
        } catch (e: Exception) {
            Log.e("ARCore", "Error resuming AR session", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            arSession?.pause()
            Log.d("ARCore", "AR session paused onPause")
        } catch (e: Exception) {
            Log.e("ARCore", "Error pausing AR session", e)
        }
    }
}
