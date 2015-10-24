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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.viewpagerindicator.UnderlinePageIndicator;

import apt.connexus.adapters.StreamAdapter;
import apt.connexus.adapters.NearbyAdapter;
import apt.connexus.adapters.ViewPagerAdapter;
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
    public static final String REQUEST_MySubscription = Domain_name + "/api/my_subscription";
    public static final String REQUEST_SearchStreams = Domain_name + "/api/search_request?term=";
    public static String REQUEST_NearbyStreams = Domain_name + "/api/image_location?";
    public static final String TAG = "ViewAllStreamActivity";
    private Boolean locationOn = false;
    private int nearbyIndex = 0;
    private TextView search_results_textView;

    final Context context = this;
    private ViewPager viewPager;
    private LinearLayout title_linearLayout1, title_linearLayout2, title_linearLayout3;
    private List<View> views;
    private LocationManager locationMgr;
    private int offset = 0;
    private int currIndex = 0;
    private View view1,view2,view3;
    private EditText search_editText;
    private LinearLayout subscribe_linearLayout;

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
        views = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        view1 = inflater.inflate(R.layout.activity_view_all_stream, null);
        subscribe_linearLayout = (LinearLayout) view1.findViewById(R.id.subscribe_linearLayout);
        if(LoginActivity.signedIn)
            subscribe_linearLayout.setVisibility(View.VISIBLE);

        view2 = inflater.inflate(R.layout.activity_search_result, null);
        search_results_textView = (TextView) view2.findViewById(R.id.search_results_textView);
        search_editText = (EditText) view2.findViewById(R.id.search_editText);

        view3 = inflater.inflate(R.layout.activity_nearby, null);
        views.add(view1);
        views.add(view2);
        views.add(view3);

        viewPager.setAdapter(new ViewPagerAdapter(views));
        viewPager.setCurrentItem(0);

        //Bind the title indicator to the adapter
        UnderlinePageIndicator titleIndicator = (UnderlinePageIndicator) findViewById(R.id.titles);
        titleIndicator.setFades(false);
        titleIndicator.setViewPager(viewPager);
        titleIndicator.setOnPageChangeListener(new MyOnPageChangeListener());

    }

    private void InitTextView() {
        title_linearLayout1 = (LinearLayout) findViewById(R.id.title_linearLayout1);
        title_linearLayout2 = (LinearLayout) findViewById(R.id.title_linearLayout2);
        title_linearLayout3 = (LinearLayout) findViewById(R.id.title_linearLayout3);

        title_linearLayout1.setOnClickListener(new MyOnClickListener(0));
        title_linearLayout2.setOnClickListener(new MyOnClickListener(1));
        title_linearLayout3.setOnClickListener(new MyOnClickListener(2));
    }

    public void load_my_subscription(View view) {
        client.setCookieStore(new PersistentCookieStore(getApplicationContext()));
        client.get(REQUEST_MySubscription, view_my_subscription_handler);
    }

    public void load_all_streams(View view) {
        client.setCookieStore(new PersistentCookieStore(getApplicationContext()));
        client.get(REQUEST_ViewAllStreams, view_all_stream_handler);
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


    private static ArrayList<String> nearbyImageURLs, nearbyStreamNames, nearbyDistances, nearbyUserEmails;
    private int currNearbyIndex = 0;

    public void moreNearby(View view) {
        if(nearbyImageURLs == null)
            return;
        currNearbyIndex = currNearbyIndex + 1;
        setNearbyAdapter(currNearbyIndex);
    }

    private void setNearbyAdapter(int currIndex) {
        int start, end;
        start = currIndex * 16;
        if( (currIndex + 1) * 16 > nearbyImageURLs.size()) {
            end = nearbyImageURLs.size();
            currNearbyIndex = -1;
        }
        else {
            end = (currIndex + 1) * 16;
        }
        ArrayList<String> tempNearbyImageURLs = new ArrayList<String>(nearbyImageURLs.subList(start, end));
        ArrayList<String> tempNearbyDistances = new ArrayList<String>(nearbyDistances.subList(start, end));
        ArrayList<String> tempNearbyStreamNames = new ArrayList<String>(nearbyStreamNames.subList(start, end));
        ArrayList<String> tempNearbyUserEmails = new ArrayList<String>(nearbyUserEmails.subList(start, end));
        loadNearbyGridView(view3, R.id.nearby_gridView, tempNearbyImageURLs, tempNearbyDistances, tempNearbyStreamNames, tempNearbyUserEmails);
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

            nearbyImageURLs = new ArrayList<String>();
            nearbyStreamNames = new ArrayList<String>();
            nearbyDistances = new ArrayList<String>();
            nearbyUserEmails = new ArrayList<>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("image_location");

                for (int i = 0; i < streamsDictArr.length(); i++) {

                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    nearbyDistances.add(jObject2.getString("distance"));
                    nearbyStreamNames.add(jObject2.getString("stream_name"));
                    nearbyUserEmails.add(jObject2.getString("user_email"));
                    Log.v(TAG, jObject2.getString("img_url"));
                    String imageURL = jObject2.getString("img_url");
                    if (imageURL.equals("")) {
                        imageURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    else {
                        imageURL = Domain_name + imageURL;
                    }
                    nearbyImageURLs.add(imageURL);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }
            setNearbyAdapter(0);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        }
    };

    private void loadNearbyGridView(View view, int id,
                                    ArrayList<String> urls,
                                    ArrayList<String> distances,
                                    final ArrayList<String> streamNames,
                                    final ArrayList<String> userEmails) {
        GridView gridview = (GridView) view.findViewById(id);
        gridview.setAdapter(new NearbyAdapter(context, urls, distances));

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
                intent.putExtra("userEmail", userEmails.get(position));
                startActivity(intent);
            }
        });
    }



    private static ArrayList<String> searchImageURLs, searchStreamNames, searchUserEmails;
    private int currSearchIndex = 0;

    private void setSearchAdapter(int currIndex) {
        int start, end;
        start = currIndex * 8;
        if( (currIndex + 1) * 8 > searchImageURLs.size()) {
            end = searchImageURLs.size();
            currSearchIndex = 0;
        }
        else {
            end = (currIndex + 1) * 8;
        }
        ArrayList<String> tempSearchImageURLs = new ArrayList<String>(searchImageURLs.subList(start, end));
        ArrayList<String> tempSearchStreamNames = new ArrayList<String>(searchStreamNames.subList(start, end));
        ArrayList<String> tempSearchUserEmails = new ArrayList<String>(searchUserEmails.subList(start, end));
        loadGridView(view2, R.id.search_gridView, tempSearchImageURLs, tempSearchStreamNames, tempSearchUserEmails);
    }

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
            searchImageURLs = new ArrayList<String>();
            searchStreamNames = new ArrayList<String>();
            searchUserEmails = new ArrayList<>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("stream");
                for (int i = 0; i < streamsDictArr.length(); i++) {
                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    searchStreamNames.add(jObject2.getString("stream_name"));
                    searchUserEmails.add(jObject2.getString("user_email"));
                    String coverURL = jObject2.getString("cover_url");
                    if (coverURL.equals("")) {
                        coverURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    searchImageURLs.add(coverURL);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }
            search_results_textView.setText(searchImageURLs.size() + " results found.");
            setSearchAdapter(0);
        }
        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {}
    };

    AsyncHttpResponseHandler view_my_subscription_handler = new AsyncHttpResponseHandler() {
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
            final ArrayList<String> user_emails = new ArrayList<String>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("stream");

                for (int i = 0; i < streamsDictArr.length(); i++) {
                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    streamNames.add(jObject2.getString("stream_name"));
                    user_emails.add(jObject2.getString("user_email"));
                    String coverURL = jObject2.getString("cover_url");
                    if (coverURL.equals("")) {
                        coverURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    imageURLs.add(coverURL);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }

            loadGridView(view1, R.id.gridView, imageURLs, streamNames, user_emails);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
        }
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
            final ArrayList<String> user_emails = new ArrayList<>();
            try {
                JSONObject jObject = new JSONObject(response);
                JSONArray streamsDictArr = jObject.getJSONArray("stream");

                for (int i = 0; i < streamsDictArr.length(); i++) {
                    String streamsDict = streamsDictArr.getString(i);
                    JSONObject jObject2 = new JSONObject(streamsDict);

                    streamNames.add(jObject2.getString("stream_name"));
                    user_emails.add(jObject2.getString("user_email"));

                    String coverURL = jObject2.getString("cover_url");
                    if (coverURL.equals("")) {
                        coverURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                    }
                    imageURLs.add(coverURL);
                }
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }

            loadGridView(view1, R.id.gridView, imageURLs, streamNames, user_emails);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
        }
    };

    private void loadGridView(View view, int id,
                              final ArrayList<String> imageURLs,
                              final ArrayList<String> streamNames,
                              final ArrayList<String> user_emails) {

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
                Intent intent = new Intent(ViewAllStreamActivity.this, ViewSingleActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("streamName", streamNames.get(position));
                intent.putExtra("userEmail", user_emails.get(position));
                startActivity(intent);
            }
        });
    }


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v("Location", provider + status);
        }
        @Override
        public void onProviderEnabled(String provider) {
            Log.v("Location", provider + " enabled.");
        }
        @Override
        public void onProviderDisabled(String provider) {}
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
        locationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 10, mLocationListener);
        Toast.makeText(ViewAllStreamActivity.this, "Activating location service.", Toast.LENGTH_SHORT).show();
        Location location = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());
        if(latitude != null) {
            nearbyIndex = 0;
            client.get(REQUEST_NearbyStreams + "lat=" +  latitude + "&lng=" + longitude, nearby_handler);
        }
    }

    public void search(View view) {
        String term = search_editText.getText().toString();
        client.get(REQUEST_SearchStreams + term, search_handler);
        Toast.makeText(ViewAllStreamActivity.this, "search " + term, Toast.LENGTH_SHORT).show();
    }
}
