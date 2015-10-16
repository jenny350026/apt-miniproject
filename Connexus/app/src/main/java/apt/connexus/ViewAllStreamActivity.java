package apt.connexus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ViewAllStreamActivity extends Activity {
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    public static final String REQUEST_ViewAllStreams = "http://apt-miniproject-1078.appspot.com/api/view_all";
    public static final String TAG = "ViewAllStreamActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_stream);

        httpClient.get(REQUEST_ViewAllStreams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                Log.v(TAG, String.valueOf(responseBody));

                final ArrayList<String> imageURLs = new ArrayList<String>();
                final ArrayList<String> streamNames = new ArrayList<String>();
                try {
                    JSONObject jObject = new JSONObject(String.valueOf(responseBody));
                    JSONArray streamsDictArr = jObject.getJSONArray("stream");

                    for (int i = 0; i < streamsDictArr.length(); i++) {

                        String streamsDict = streamsDictArr.getString(i);
                        JSONObject jObject2 = new JSONObject(streamsDict);
                        Log.v(TAG, jObject2.getString("cover_url"));
                        imageURLs.add(jObject2.getString("cover_url"));
                        streamNames.add(jObject2.getString("stream_name"));
                    }

                    GridView gridview = (GridView) findViewById(R.id.gridView);
                    gridview.setAdapter(new ImageAdapter(ViewAllStreamActivity.this, imageURLs));
                    gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View v,
                                                       int position, long id) {
                            Toast.makeText(ViewAllStreamActivity.this, streamNames.get(position), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });

                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v,
                                                int position, long id) {

                            Intent intent = new Intent(ViewAllStreamActivity.this, ViewSingleActivity.class);
                            intent.putExtra("position", position);
                            intent.putExtra("streamName", streamNames.get(position));
                            startActivity(intent);

                        }
                    });

                } catch (JSONException j) {
                    Log.v(TAG, j.toString());
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
            }
        });


        ImageButton stream1btn = (ImageButton) findViewById(R.id.stream1);
        Button nearby_btn = (Button) findViewById(R.id.nearby_btn);
        Button search_btn = (Button) findViewById(R.id.search_btn);

        stream1btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewAllStreamActivity.this, ViewSingleActivity.class);
                startActivity(intent);
            }
        });

        nearby_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewAllStreamActivity.this, NearbyActivity.class);
                startActivity(intent);
            }
        });

        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewAllStreamActivity.this, SearchResultActivity.class);
                startActivity(intent);
            }
        });

    }

}
