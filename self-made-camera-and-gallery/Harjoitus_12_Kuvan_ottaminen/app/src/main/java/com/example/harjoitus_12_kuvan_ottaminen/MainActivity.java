package com.example.harjoitus_12_kuvan_ottaminen;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "kamera softa: ";

    private Button btnTake;
    private Button btnGallery;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private File folder;
    private String folderName = "MyPhotoDir";
    private static final int REQUEST_CAMERA_PERMISSION = 123;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.texture);
        if (textureView != null) {
            textureView.setSurfaceTextureListener(textureListener);
        }
        btnTake = findViewById(R.id.btnTake);
        btnGallery = findViewById(R.id.btnGallery);
        if (btnTake != null) {
            btnTake.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    takePicture();
                }
            });
        }
        if (btnGallery != null) {
            btnGallery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, CustomGalleryActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

        TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        };

        private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d(TAG, "onOpened");
                cameraDevice = camera;
                createCameraPreview();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                cameraDevice.close();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                cameraDevice.close();
                cameraDevice = null;
            }
        };

        protected void startBackgroundThread() {
            mBackgroundThread = new HandlerThread("Camera Background");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }

        protected void stopBackgroundThread() {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        protected void takePicture() {
            if (cameraDevice == null) {
                Log.d(TAG, "cameraDevice on null");
                return;
            }
            if (!isExternalStorageAvailableForRW() || isExternalStorageReadOnly()) {
                btnTake.setEnabled(false);
            }
            if (isStoragePermissionGranted()) {
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                try {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
                    Size[] jpegSizes = null;
                    if (characteristics != null) {
                        jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                    }
                    int width = 640;
                    int height = 480;
                    if (jpegSizes != null && jpegSizes.length > 0) {
                        width = jpegSizes[0].getWidth();
                        height = jpegSizes[0].getHeight();
                    }
                    ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                    List<Surface> outputSurfaces = new ArrayList<>(2);
                    outputSurfaces.add(reader.getSurface());
                    outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
                    final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureBuilder.addTarget(reader.getSurface());
                    captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                    int rotation = getWindowManager().getDefaultDisplay().getRotation();
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
                    file = null;
                    folder = new File(folderName);
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "IMG_" + timeStamp + ".jpg";
                    file = new File(getExternalFilesDir(folderName), "/" + imageFileName);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            Image image = null;
                            try {
                                image = reader.acquireLatestImage();
                                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                                byte[] bytes = new byte[buffer.capacity()];
                                buffer.get(bytes);
                                save(bytes);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (image != null) {
                                    image.close();
                                }
                            }
                        }

                        private void save(byte[] bytes) throws IOException {
                            OutputStream output = null;
                            try {
                                output = new FileOutputStream(file);
                                output.write(bytes);
                            } finally {
                                if (null != output) {
                                    output.close();
                                }
                            }
                        }
                    };
                    reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
                    final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            Toast.makeText(MainActivity.this, "Tallennettu: " + file, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, ""+file);
                            createCameraPreview();
                        }
                    };
                    cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        }
                    }, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        private static boolean isExternalStorageReadOnly() {
            String extStorageState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
                return true;
            }
            return false;
        }

        private boolean isExternalStorageAvailableForRW() {
            String extStorageState = Environment.getExternalStorageState();
            if (extStorageState.equals(Environment.MEDIA_MOUNTED)) {
                return true;
            }
            return false;
        }

        private boolean isStoragePermissionGranted() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            } else {
                return true;
            }
        }

        protected void createCameraPreview() {
            try {
                SurfaceTexture texture = textureView.getSurfaceTexture();
                assert texture != null;
                texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
                Surface surface = new Surface(texture);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(surface);
                cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        if (null == cameraDevice) {
                            return;
                        }
                        cameraCaptureSessions = session;
                        updatePreview();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Toast.makeText(MainActivity.this, "Konfiguraation muutos", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        private void openCamera() {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            Log.d(TAG, "Kamera avattu.");
            try {
                cameraId = manager.getCameraIdList()[0];
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                    return;
                }
                manager.openCamera(cameraId, stateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "kameran avaus");
        }

        protected void updatePreview() {
            if (null == cameraDevice) {
                Log.d(TAG, "updatePreview virhe");
            }
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            try {
                cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(MainActivity.this, "Sovellusta ei voi käyttää antamatta oikeuksia kameraan", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        stopBackgroundThread();
        super.onPause();
    }
}



