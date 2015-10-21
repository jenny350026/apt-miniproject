package apt.connexus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;

import apt.connexus.adapters.StreamAdapter;
import apt.connexus.adapters.NearbyAdapter;
import apt.connexus.adapters.viewPagerAdapter;
import cz.msebera.android.httpclient.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ViewAllStreamActivity extends Activity {
    private AsyncHttpClient client = new AsyncHttpClient();
    public static final String Domain_name = "http://apt-miniproject-1078.appspot.com";
    public static final String REQUEST_ViewAllStreams = Domain_name + "/api/view_all";
    public static final String REQUEST_SearchStreams = Domain_name + "/api/search_request?term=";
    public static String REQUEST_NearbyStreams = Domain_name + "/api/image_location?";
    public static final String TAG = "ViewAllStreamActivity";
    private Boolean locationOn = false;

    final Context context = this;
    private ViewPager viewPager;
    private TextView title_textView1, title_textView2, title_textView3;
    private List<View> views;
    private LocationManager locationMgr;
    private int offset = 0;
    private int currIndex = 0;
    private View view1,view2,view3;
    private EditText search_editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager_viewall);
        InitTextView();
        InitViewPager();
        client.setCookieStore(new PersistentCookieStore(getApplicationContext()));
        client.get(REQUEST_ViewAllStreams, view_all_stream_handler);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(locationMgr != null)
            locationMgr.removeUpdates(mLocationListener);
    }

    private void InitViewPager() {
        viewPager=(ViewPager) findViewById(R.id.vPager);
        views=new ArrayList<View>();
        LayoutInflater inflater=getLayoutInflater();
        view1=inflater.inflate(R.layout.activity_view_all_stream, null);

        view2=inflater.inflate(R.layout.activity_search_result, null);

        search_editText = (EditText) view2.findViewById(R.id.search_editText);

        view3=inflater.inflate(R.layout.activity_nearby, null);
        views.add(view1);
        views.add(view2);
        views.add(view3);
        viewPager.setAdapter(new viewPagerAdapter(views));
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
    }

    private void InitTextView() {
        title_textView1 = (TextView) findViewById(R.id.title_textView1);
        title_textView2 = (TextView) findViewById(R.id.title_textView2);
        title_textView3 = (TextView) findViewById(R.id.title_textView3);

        title_textView1.setOnClickListener(new MyOnClickListener(0));
        title_textView2.setOnClickListener(new MyOnClickListener(1));
        title_textView3.setOnClickListener(new MyOnClickListener(2));
    }



    private class MyOnClickListener implements View.OnClickListener {
        private int index=0;
        public MyOnClickListener(int i){
            index=i;
        }
        public void onClick(View v) {
            viewPager.setCurrentItem(index);
        }
    }

    public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {


        public void onPageScrollStateChanged(int arg0) {
        }

        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        public void onPageSelected(int arg0) {}
    }



    AsyncHttpResponseHandler nearby_handler = new AsyncHttpResponseHandler(){
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String response = "";
            try {
                response = new String(responseBody, "UTF-8");
                System.out.println("Nearby: " + response);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            final ArrayList<String> imageURLs = new ArrayList<String>();
            final ArrayList<String> streamNames = new ArrayList<String>();
            final ArrayList<String> distances = new ArrayList<String>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("image_location");

                for (int i = 0; i < streamsDictArr.length(); i++) {

                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    distances.add(jObject2.getString("distance"));
                    streamNames.add(jObject2.getString("stream_name"));
                    Log.v(TAG, jObject2.getString("img_url"));
                    String imageURL = jObject2.getString("img_url");
                    if (imageURL.equals("")) {
                        imageURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    else {
                        imageURL = Domain_name + imageURL;
                    }
                    imageURLs.add(imageURL);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }

            GridView gridview = (GridView) view3.findViewById(R.id.nearby_gridView);
            gridview.setAdapter(new NearbyAdapter(context, imageURLs, distances));

            gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                    Toast.makeText(ViewAllStreamActivity.this, streamNames.get(position), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    Intent intent = new Intent(ViewAllStreamActivity.this, ViewSingleActivity.class);
                    intent.putExtra("position", position);
                    intent.putExtra("streamName", streamNames.get(position));
                    startActivity(intent);
                }
            });
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            String response = "";
            try {
                response = new String(responseBody, "UTF-8");
                System.out.println("Nearby: " + response);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    AsyncHttpResponseHandler search_handler = new AsyncHttpResponseHandler(){

        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String response = "";
            try {
                response = new String(responseBody, "UTF-8");
                System.out.println(response);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            final ArrayList<String> imageURLs = new ArrayList<String>();
            final ArrayList<String> streamNames = new ArrayList<String>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("stream");

                for (int i = 0; i < streamsDictArr.length(); i++) {
                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    Log.v(TAG, jObject2.getString("stream_name"));
                    streamNames.add(jObject2.getString("stream_name"));
                    Log.v(TAG, jObject2.getString("cover_url"));
                    String coverURL = jObject2.getString("cover_url");
                    if (coverURL.equals("")) {
                        coverURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    imageURLs.add(coverURL);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }

            loadGridView(view2, R.id.search_gridView, imageURLs, streamNames, ViewSingleActivity.class);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {}
    };


    AsyncHttpResponseHandler view_all_stream_handler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            String response = "";
            try {
                response = new String(responseBody, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            final ArrayList<String> imageURLs = new ArrayList<String>();
            final ArrayList<String> streamNames = new ArrayList<String>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("stream");

                for (int i = 0; i < streamsDictArr.length(); i++) {
                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    streamNames.add(jObject2.getString("stream_name"));
                    String coverURL = jObject2.getString("cover_url");
                    if (coverURL.equals("")) {
                        coverURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    imageURLs.add(coverURL);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }

            loadGridView(view1, R.id.gridView, imageURLs, streamNames, ViewSingleActivity.class);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
        }
    };

    private void loadGridView(View view, int id, final ArrayList<String> imageURLs, final ArrayList<String> streamNames, final Class<?> cls){
        GridView gridview = (GridView) view.findViewById(id);
        gridview.setAdapter(new StreamAdapter(context, imageURLs, streamNames));

        gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(ViewAllStreamActivity.this, streamNames.get(position), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(ViewAllStreamActivity.this, cls);
                intent.putExtra("position", position);
                intent.putExtra("streamName", streamNames.get(position));
                startActivity(intent);
            }
        });
    }




    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            if(!locationOn) {
                Toast.makeText(ViewAllStreamActivity.this, "Activated.", Toast.LENGTH_SHORT).show();
                locationOn = true;
            }
            Log.v("Location latitude", String.valueOf(location.getLatitude()));
            Log.v("Location longitude", String.valueOf(location.getLongitude()));
            latitude = String.valueOf(location.getLatitude());
            longitude = String.valueOf(location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v("Location", provider + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.v("Location", provider + " enabled.");
        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void initLocation() {
        locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(ViewAllStreamActivity.this)
                    .setTitle("Location service")
                    .setMessage("Start Location service?")
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivity(intent);
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(ViewAllStreamActivity.this, "Location service unavailable.", Toast.LENGTH_SHORT).show();
                                }
                            }
                    ).show();
        }
    }
    private String longitude, latitude;

    public void getNearbyImages(View view) {
        initLocation();
        locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 10, mLocationListener);
        locationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 10, mLocationListener);
        if(latitude != null) {
            Log.v(TAG, "GET IMAGE LOCATION FROM SERVER");
            client.get(REQUEST_NearbyStreams + "lat=" +  latitude + "&lng=" + longitude, nearby_handler);
        }
        else
            Toast.makeText(ViewAllStreamActivity.this, "Activating location service.", Toast.LENGTH_SHORT).show();
    }

    public void search(View view) {
        String term = search_editText.getText().toString();
        client.get(REQUEST_SearchStreams + term, search_handler);
        Toast.makeText(ViewAllStreamActivity.this, "search " + term, Toast.LENGTH_SHORT).show();
    }
}
