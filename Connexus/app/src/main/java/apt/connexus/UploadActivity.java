package apt.connexus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class UploadActivity extends Activity {

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_LIBRARY_PHOTO = 2;

    static final String TAG = "UploadActivity";

    static final String upload_url = "http://apt-miniproject-1078.appspot.com/api/upload";
//    static final String upload_url = "http://localhost:11080/api/upload";


    ImageView selectedImageView;
    String mCameraPhotoPath;
    Bitmap myBitmap = null;

    private AsyncHttpClient client = new AsyncHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Button upload = (Button) findViewById(R.id.upload_btn);
        Button camera_btn = (Button) findViewById(R.id.camera_btn);
        Button library_btn = (Button) findViewById(R.id.library_btn);

        selectedImageView = (ImageView) findViewById(R.id.imageView);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                myBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);

                Log.v(TAG, "num bytes" + stream.toByteArray().length);

                RequestParams params = new RequestParams();
                params.put("file", new ByteArrayInputStream(stream.toByteArray()));
                params.put("comment", ((TextView) findViewById(R.id.photoComment)).getText().toString());
                params.put("longitude", 0);
                params.put("latitude", 0);
                params.put("stream_id", getIntent().getStringExtra("stream_id"));

                Log.v(TAG, "posting to " + upload_url);
                Log.v(TAG, "stream_id " + getIntent().getStringExtra("stream_id"));

                Toast.makeText(UploadActivity.this, "Uploading...", Toast.LENGTH_SHORT).show();

                client.post(upload_url, params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Toast.makeText(UploadActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "Upload Successful");
                        finish();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.e(TAG, "There was a problem posting to url : " + error.toString());
                        Toast.makeText(UploadActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
                    }

                });
              }
        });

        library_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, REQUEST_LIBRARY_PHOTO);
            }
        });


        camera_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        ex.getStackTrace();
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    }
                }
            }
        });
    }

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
            mCameraPhotoPath = image.getAbsolutePath();
//        Log.v(TAG, mCurrentPhotoPath);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    myBitmap = BitmapFactory.decodeFile(mCameraPhotoPath);
                    selectedImageView.setImageBitmap(myBitmap);
                }
                break;
            case REQUEST_LIBRARY_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    myBitmap = BitmapFactory.decodeStream(imageStream);
//                    mCurrentPhotoPath = (new File(selectedImage.toString())).getAbsolutePath();
//                    mCurrentPhotoPath = selectedImage.getEncodedPath();
                    selectedImageView.setImageURI(selectedImage);

                }
                break;
        }

//        Log.v(TAG, "photoPath " + mCurrentPhotoPath);
    }



}
