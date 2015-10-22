package apt.connexus;

import android.app.Activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

public class ViewSingleActivity extends Activity {
    private AsyncHttpClient client = new AsyncHttpClient();
    public static final String Domain_name = "http://apt-miniproject-1078.appspot.com";
    public String REQUEST_ViewSingleStream = Domain_name + "/api/stream?stream_name=";
    public static final String TAG = "ViewSingleStream";
    Context context = this;

    AsyncHttpResponseHandler getSingleStreamHandler =  new AsyncHttpResponseHandler() {

        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String response = "";
            try {
                response = new String(responseBody, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            final ArrayList<String> imageURLs = new ArrayList<String>();
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
            Log.v(TAG, "imageUrl size = " + String.valueOf(imageURLs.size()));
            GridView gridview = (GridView) findViewById(R.id.singleStreamGridView);
            gridview.setAdapter(new ImageAdapter(context, imageURLs));
            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v,
                                        int position, long id) {

                    Dialog imageDialog = new Dialog(context);
                    imageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    imageDialog.setContentView(R.layout.thumbnail_layout);
                    ImageView image = (ImageView) imageDialog.findViewById(R.id.thumbnail_imageview);

                    Picasso.with(context).load(imageURLs.get(position)).resize(500, 500).centerCrop().into(image);

                    imageDialog.show();
                }
            });
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
        }
    };

    private static String stream_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_single);

        final String streamName = getIntent().getStringExtra("streamName");
        REQUEST_ViewSingleStream += streamName;

        client.setCookieStore(new PersistentCookieStore(getApplicationContext()));
        client.get(REQUEST_ViewSingleStream, getSingleStreamHandler);

        TextView view_single_textView = (TextView) findViewById(R.id.view_single_textView);
        view_single_textView.setText("View A Stream: " + streamName);
        Button back_to_streams = (Button) findViewById(R.id.back_to_streams);
        Button upload_img = (Button) findViewById(R.id.upload_img);

            back_to_streams.setOnClickListener(new View.OnClickListener()  {
                   @Override
                   public void onClick(View v) {
                       finish();
                   }
               }
            );

            upload_img.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                      Intent intent = new Intent(ViewSingleActivity.this, UploadActivity.class);
                      intent.putExtra("stream_id", stream_id);
                      intent.putExtra("stream_name", streamName);
                      startActivity(intent);
                  }
              }
            );
        }

        @Override
        public void onResume(){
            super.onResume();
            client.get(REQUEST_ViewSingleStream, getSingleStreamHandler);
        }
    }
