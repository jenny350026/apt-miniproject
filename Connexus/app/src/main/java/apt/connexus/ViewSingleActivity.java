package apt.connexus;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class ViewSingleActivity extends Activity {
    private AsyncHttpClient client = new AsyncHttpClient();
    public static final String Domain_name = "http://apt-miniproject-1078.appspot.com";
    public String REQUEST_ViewSingleStream = Domain_name + "/api/stream?stream_name=";
    public static final String TAG = "ViewSingleStream";
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_single);

        String streamName = getIntent().getStringExtra("streamName");
        REQUEST_ViewSingleStream += streamName;


        client.get(REQUEST_ViewSingleStream, new AsyncHttpResponseHandler() {

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

                GridView gridview = (GridView) findViewById(R.id.singleStreamGridView);
                gridview.setAdapter(new ImageAdapter(context, imageURLs));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
            }
        });

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
                      startActivity(intent);
                  }
              }
            );
        }

    }
