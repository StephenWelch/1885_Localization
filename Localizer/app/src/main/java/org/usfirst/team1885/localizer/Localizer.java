package org.usfirst.team1885.localizer;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;

import org.usfirst.team1885.localizer.comms.RobotConnection;
import org.usfirst.team1885.localizer.rendering.BackgroundRenderer;
import org.usfirst.team1885.localizer.rendering.PlaneRenderer;
import org.usfirst.team1885.localizer.rendering.PointCloudRenderer;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Stephen Welch on 10/24/2017.
 */

public class Localizer extends AppCompatActivity implements GLSurfaceView.Renderer {

    private static final String TAG = Localizer.class.getSimpleName();
    private static final long UDPATE_PERIOD = 50;

    private long lastUpdateTime;

    private GLSurfaceView surfaceView;
    private BackgroundRenderer backgroundRenderer;
    private PlaneRenderer planeRenderer;
    private PointCloudRenderer pointCloud;


    private Config config;
    private Session session;
    private Frame frame;

    private RobotConnection connection;

    public Localizer() {
        this.backgroundRenderer = new BackgroundRenderer();
        this.planeRenderer = new PlaneRenderer();
        this.pointCloud = new PointCloudRenderer();
        this.connection = new RobotConnection(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        enableDeviceAdmin();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);

        session = new Session(/*context=*/this);

        // Create default config, check is supported, create session from that config.
        config = Config.createDefaultConfig();
        config.setUpdateMode(Config.UpdateMode.BLOCKING);
        if (!session.isSupported(config)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        whitelistLockTasks();

        connection.start();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            // Note that order matters - see the note in onPause(), the reverse applies here.
            session.resume(config);
            surfaceView.onResume();
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        surfaceView.onPause();
        session.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }


    private void whitelistLockTasks() {
        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName componentName = ChezyDeviceAdminReceiver.getComponentName(this);

        if (manager.isDeviceOwnerApp(getPackageName())) {
            manager.setLockTaskPackages(componentName, new String[]{getPackageName()});
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        backgroundRenderer.createOnGlThread(/*context=*/this);
        session.setCameraTextureName(backgroundRenderer.getTextureId());

        try {
            planeRenderer.createOnGlThread(/*context=*/this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        pointCloud.createOnGlThread(/*context=*/this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        session.setDisplayGeometry(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            frame = session.update();
            // Draw background.
            backgroundRenderer.draw(frame);

            TextView infoView = (TextView) findViewById(R.id.infoView);
            infoView.setText(String.format("%s\n%s", frame.getPose(), frame.getTrackingState()));
            infoView.setTextColor(Color.RED);

            // If not tracking, exit.
            if (frame.getTrackingState() == Frame.TrackingState.NOT_TRACKING) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            session.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            frame.getViewMatrix(viewmtx, 0);

            // Visualize tracked points.
            pointCloud.update(frame.getPointCloud());
            pointCloud.draw(frame.getPointCloudPose(), viewmtx, projmtx);

            // Visualize planes.
            planeRenderer.drawPlanes(session.getAllPlanes(), frame.getPose(), projmtx);
            planeRenderer.drawPlanes(session.getAllPlanes(), frame.getPose(), projmtx);
            boolean isLatestMode = config.getUpdateMode() == Config.UpdateMode.LATEST_CAMERA_IMAGE;
            if(isLatestMode && System.currentTimeMillis() - lastUpdateTime >= UDPATE_PERIOD) {
                sendPosition2D(frame);
                lastUpdateTime = System.currentTimeMillis();
            } else if(!isLatestMode) {
                sendPosition2D(frame);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void enableDeviceAdmin() {
        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName componentName = ChezyDeviceAdminReceiver.getComponentName(this);

        if(!manager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, 0);
            return;
        }
    }

    private void sendPosition2D(Frame frame) {
        connection.send(String.format("x%sy%s", frame.getPose().tx(), frame.getPose().tz()));
    }

    //Just in case
    public void playAirhorn() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.airhorn);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                mp.release();
            }
        });
        mp.start();
    }

}
