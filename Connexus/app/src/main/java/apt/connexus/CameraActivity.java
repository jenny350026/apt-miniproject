package apt.connexus;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;


public class CameraActivity extends ActionBarActivity {
    private Camera mCamera;
    private CameraPreview mPreview;

    private Bitmap bitmap;

    private File pictureFile = null;

    Button captureButton;
    Button confirmButton;

    private static final String TAG = "CameraActivity";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        setTitle("Take a Photo");

        // Create an instance of Camera
        mCamera = getCameraInstance();

        //set camera to continually auto-focus
        Camera.Parameters params = mCamera.getParameters();
//*EDIT*//params.setFocusMode("continuous-picture");
//It is better to use defined constraints as opposed to String, thanks to AbdelHady
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Add a listener to the Capture button
        captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        Button cancelButton = (Button) findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.startPreview();
                        captureButton.setEnabled(true);
                        confirmButton.setEnabled(false);
                    }
                }
        );

        confirmButton = (Button) findViewById(R.id.button_confirm);
        confirmButton.setEnabled(false);
        confirmButton.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        if(bitmap == null)
                            Toast.makeText(CameraActivity.this, "No picture taken!", Toast.LENGTH_SHORT).show();
                        else {
                            Toast.makeText(CameraActivity.this, "Saving image...", Toast.LENGTH_SHORT).show();
                            FileOutputStream fos = null;
                            try {
                                pictureFile = createImageFile();
                                fos = new FileOutputStream(pictureFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                            Log.v(TAG, "file written to " + pictureFile.getAbsolutePath());
                            Intent intent = new Intent();
                            intent.putExtra("pictureFile", pictureFile);
                            setResult(Activity.RESULT_OK, intent);
                            mCamera.release();
                            finish();
                        }
                    }
                }
        );
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance

        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }



    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.

        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here
            mCamera.setDisplayOrientation(90);

            Camera.Parameters parameters = mCamera.getParameters();
            HashSet<Camera.Size> available_sizes = new HashSet<Camera.Size>(mCamera.getParameters().getSupportedPictureSizes());
            HashSet<Camera.Size> preview_sizes = new HashSet<Camera.Size>(mCamera.getParameters().getSupportedPreviewSizes());
            available_sizes.retainAll(preview_sizes);

            Camera.Size size = (new ArrayList<Camera.Size>(available_sizes)).get(0);

            for(Camera.Size s : available_sizes)
            {
                Log.v(TAG, "available size " + s.width + " " + s.height);
                if(s.width > size.width)
                    size = s;
            }

            Log.v(TAG, "selected size " + size.width + " " + size.height);
            parameters.setPictureSize(size.width, size.height);
            mCamera.setParameters(parameters);

            //landscape
            //float ratio = (float)size.width/size.height;

            //portrait
            float ratio = (float)size.height/size.width;

            int new_width = Math.round(mPreview.getHeight()*ratio);
            int new_height = mPreview.getHeight();
            Log.v(TAG, "old width and height " + mPreview.getWidth() + " " + mPreview.getHeight());
//
            Log.v(TAG, "new width and height " + new_width + " " + new_height);

            mPreview.setLayoutParams(new FrameLayout.LayoutParams(new_width, new_height));

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {


            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            mCamera.stopPreview();
            captureButton.setEnabled(false);
            confirmButton.setEnabled(true);

        }
    };

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
//        mCameraPhotoPath = image.getAbsolutePath();
//        Log.v(TAG, mCurrentPhotoPath);
        return image;
    }


}
