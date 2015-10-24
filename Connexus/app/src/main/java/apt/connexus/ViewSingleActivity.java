package apt.connexus;

import android.app.ActionBar;
import android.app.Activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import apt.connexus.adapters.ImageAdapter;
import apt.connexus.adapters.StreamAdapter;
import cz.msebera.android.httpclient.Header;

public class ViewSingleActivity extends ActionBarActivity {
    private AsyncHttpClient client = new AsyncHttpClient();
    public static final String Domain_name = "http://apt-miniproject-1078.appspot.com";
    public String REQUEST_ViewSingleStream = Domain_name + "/api/stream?stream_name=";
    public static final String TAG = "ViewSingleStream";
    Context context = this;
    private String user_email;


    private ArrayList<String> imageURLs;
    private int currImageIndex = 0;

    public void moreSingle(View view) {
        if(imageURLs == null)
            return;
        currImageIndex += 1;
        setImageAdapter(currImageIndex);
    }

    private void setImageAdapter(int currIndex) {
        int start, end;
        start = currIndex * 16;
        if( (currIndex + 1) * 16 > imageURLs.size()) {
            end = imageURLs.size();
            currImageIndex = -1;
        }
        else {
            end = (currIndex + 1) * 16;
        }
        ArrayList<String> tempImageURLs = new ArrayList<String>(imageURLs.subList(start, end));
        loadGridView(R.id.singleStreamGridView, tempImageURLs);
    }

    AsyncHttpResponseHandler getSingleStreamHandler =  new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String response = "";
            try {
                response = new String(responseBody, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            imageURLs = new ArrayList<String>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("images");

                stream_id = jObject.getString("stream_id");

                for (int i = 0; i < streamsDictArr.length(); i++) {

                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    Log.v(TAG, jObject2.getString("img_url"));
                    String image_url = jObject2.getString("img_url");
                    if (image_url.equals("")) {
                        image_url = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    else {
                        image_url = Domain_name + image_url;
                    }
                    Log.v(TAG, image_url);
                    imageURLs.add(image_url);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }
            setImageAdapter(0);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
        }
    };

    private void loadGridView(int id, final ArrayList<String> urls) {
        GridView gridview = (GridView) findViewById(id);
        gridview.setAdapter(new ImageAdapter(context, urls));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                Dialog imageDialog = new Dialog(context);
                imageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                imageDialog.setContentView(R.layout.thumbnail_layout);
                ImageView image = (ImageView) imageDialog.findViewById(R.id.thumbnail_imageview);

                Picasso.with(context).load(urls.get(position)).resize(500, 500).centerCrop().into(image);
                imageDialog.show();
            }
        });
    }

    private static String stream_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_single);

        final String streamName = getIntent().getStringExtra("streamName");
        REQUEST_ViewSingleStream += streamName;

        client.setCookieStore(new PersistentCookieStore(getApplicationContext()));
        client.get(REQUEST_ViewSingleStream, getSingleStreamHandler);

        setTitle(streamName);

        Button back_to_streams = (Button) findViewById(R.id.back_to_streams);
        user_email = getIntent().getStringExtra("userEmail");

        Button upload_img = (Button) findViewById(R.id.upload_img);
        if(user_email.equals(LoginActivity.userEmail))
            upload_img.setVisibility(View.VISIBLE);
        back_to_streams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        upload_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewSingleActivity.this, UploadActivity.class);
                intent.putExtra("stream_id", stream_id);
                intent.putExtra("stream_name", streamName);
                startActivity(intent);
            }
        });


    }

        @Override
        public void onResume(){
            super.onResume();
            client.get(REQUEST_ViewSingleStream, getSingleStreamHandler);
        }


}
