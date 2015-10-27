package apt.connexus;


import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.support.v7.widget.SearchView;
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

public class ViewAllStreamActivity extends ActionBarActivity {
    private AsyncHttpClient client = new AsyncHttpClient();
    public static final String Domain_name = "http://apt-miniproject-1078.appspot.com";
    public static final String REQUEST_ViewAllStreams = Domain_name + "/api/view_all";
    public static final String REQUEST_MySubscription = Domain_name + "/api/my_subscription";
//    public static final String REQUEST_SearchStreams = Domain_name + "/api/search_request?term=";
    public static final String REQUEST_NearbyStreams = Domain_name + "/api/image_location?";
    public static final String TAG = "ViewAllStreamActivity";
    private Boolean locationOn = false;
    private int nearbyIndex = 0;
    private TextView search_results_textView;

    final Context context = this;
    private ViewPager viewPager;
    private LinearLayout title_linearLayout1, title_linearLayout2, title_linearLayout3, title_linearLayout4;
    private ImageView title_imageView1, title_imageView2, title_imageView3, title_imageView4;
    private List<View> views;
    private LocationManager locationMgr;
    private int offset = 0;
    private int currIndex = 0;
    private View view_viewAllStreams, view_search, view_nearby, view_subscribed;
//    private EditText search_editText;

    private SwipeRefreshLayout swipeContainerAllStreams;
    private SwipeRefreshLayout swipeContainerNearby;
    private SwipeRefreshLayout swipeContainerSubscribed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager_viewall);
        title_imageView1 = (ImageView) findViewById(R.id.title_imageView1);
//        title_imageView2 = (ImageView) findViewById(R.id.title_imageView2);
        title_imageView3 = (ImageView) findViewById(R.id.title_imageView3);
        title_imageView4 = (ImageView) findViewById(R.id.title_imageView4);
        InitTextView();
        InitViewPager();
        initLocation();
        load_my_subscription();
        setTitle(title_strings[0]);
//        getSupportActionBar().setDisplayShowTitleEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(true);
//        mActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.theme_color)));
//        mActionBar.setElevation(4f);

        swipeContainerAllStreams = (SwipeRefreshLayout) view_viewAllStreams.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainerAllStreams.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                client.setCookieStore(new PersistentCookieStore(getApplicationContext()));
                client.get(REQUEST_ViewAllStreams, view_all_stream_handler);
            }
        });
        // Configure the refreshing colors
        swipeContainerAllStreams.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        swipeContainerNearby = (SwipeRefreshLayout) view_nearby.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainerNearby.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                getNearbyImages();
            }
        });
        // Configure the refreshing colors
        swipeContainerNearby.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        swipeContainerSubscribed = (SwipeRefreshLayout) view_subscribed.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainerSubscribed.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                if (LoginActivity.signedIn)
                    load_my_subscription();
                else {
                    Toast.makeText(ViewAllStreamActivity.this, "You are not logged in!", Toast.LENGTH_SHORT).show();
                    swipeContainerSubscribed.setRefreshing(false);
                }

            }
        });
        // Configure the refreshing colors
        swipeContainerSubscribed.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

//        getNearbyImages();
        client.setResponseTimeout(10000);
        client.setCookieStore(new PersistentCookieStore(getApplicationContext()));
        client.get(REQUEST_ViewAllStreams, view_all_stream_handler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final MenuItem searchMenuItem = menu.findItem(R.id.search);
        final SearchView searchView =
                (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean queryTextFocused) {
                if (!queryTextFocused) {
                    searchMenuItem.collapseActionView();
                    searchView.setQuery("", false);
                }
            }
        });

        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(locationMgr != null)
            locationMgr.removeUpdates(mLocationListener);
    }

    private void InitViewPager() {
        viewPager = (ViewPager) findViewById(R.id.vPager);
        views = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        view_viewAllStreams = inflater.inflate(R.layout.activity_view_all_stream, null);

//        view_search = inflater.inflate(R.layout.activity_search_result, null);
//        search_results_textView = (TextView) view_search.findViewById(R.id.search_results_textView);

        view_nearby = inflater.inflate(R.layout.activity_nearby, null);
        view_subscribed = inflater.inflate(R.layout.activity_view_subscribed_stream, null);
        views.add(view_viewAllStreams);
//        views.add(view_search);

        views.add(view_subscribed);
        views.add(view_nearby);

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
//        title_linearLayout2 = (LinearLayout) findViewById(R.id.title_linearLayout2);
        title_linearLayout3 = (LinearLayout) findViewById(R.id.title_linearLayout3);
        title_linearLayout4 = (LinearLayout) findViewById(R.id.title_linearLayout4);

        title_linearLayout1.setOnClickListener(new MyOnClickListener(0));
//        title_linearLayout2.setOnClickListener(new MyOnClickListener(1));
        title_linearLayout3.setOnClickListener(new MyOnClickListener(1));
        title_linearLayout4.setOnClickListener(new MyOnClickListener(2));
    }

    public void load_my_subscription() {
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
            changePage(index);
        }
    }

    private void changePage(int index){
        setTitle(title_strings[index]);
        title_imageView1.setImageDrawable(getResources().getDrawable(R.drawable.ic_image_black_24dp));
//        title_imageView2.setImageDrawable(getResources().getDrawable(R.drawable.ic_search_black_24dp));
        title_imageView3.setImageDrawable(getResources().getDrawable(R.drawable.ic_collections_bookmark_black_24dp));
        title_imageView4.setImageDrawable(getResources().getDrawable(R.drawable.ic_language_black_24dp));
        if(index == 0)
            title_imageView1.setImageDrawable(getResources().getDrawable(R.drawable.ic_image_white_24dp));
//        else if(index == 1)
//            title_imageView2.setImageDrawable(getResources().getDrawable(R.drawable.ic_search_white_24dp));
        else if(index == 1)
            title_imageView3.setImageDrawable(getResources().getDrawable(R.drawable.ic_collections_bookmark_white_24dp));
        else if(index == 2)
            title_imageView4.setImageDrawable(getResources().getDrawable(R.drawable.ic_language_white_24dp));
        viewPager.setCurrentItem(index);
    }

    private ImageView[] title_imageViews;

    private static final String[] title_strings = {"All Streams", "Subscribed Streams", "Nearby Images"};

    public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
        public void onPageScrollStateChanged(int state) {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
            changePage(position);
        }
    }


    private static ArrayList<String> nearbyImageURLs, nearbyStreamNames, nearbyDistances, nearbyUserEmails;
    private int currNearbyIndex = 0;

    public void moreNearby(View view) {
        if(nearbyImageURLs == null)
            return;
        currNearbyIndex = currNearbyIndex + 1;
        setNearbyAdapter(currNearbyIndex);
    }

    private final int number_of_images_in_a_nearby_page = 9;


    private void setNearbyAdapter(int currIndex) {
        int start, end;
        start = currIndex * number_of_images_in_a_nearby_page;
        if( (currIndex + 1) * number_of_images_in_a_nearby_page > nearbyImageURLs.size()) {
            end = nearbyImageURLs.size();
            currNearbyIndex = -1;
        }
        else {
            end = (currIndex + 1) * number_of_images_in_a_nearby_page;
        }
        ArrayList<String> tempNearbyImageURLs = new ArrayList<String>(nearbyImageURLs.subList(start, end));
        ArrayList<String> tempNearbyDistances = new ArrayList<String>(nearbyDistances.subList(start, end));
        ArrayList<String> tempNearbyStreamNames = new ArrayList<String>(nearbyStreamNames.subList(start, end));
        ArrayList<String> tempNearbyUserEmails = new ArrayList<String>(nearbyUserEmails.subList(start, end));
        loadNearbyGridView(view_nearby, R.id.nearby_gridView, tempNearbyImageURLs, tempNearbyDistances, tempNearbyStreamNames, tempNearbyUserEmails);
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
                        imageURL = getString(R.string.no_image_url);
                    }
                    else {
                        imageURL = Domain_name + imageURL;
                    }
                    nearbyImageURLs.add(imageURL);
                }
                swipeContainerNearby.setRefreshing(false);
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



//    private static ArrayList<String> searchImageURLs, searchStreamNames, searchUserEmails;
    private int currSearchIndex = 0;

//    private void setSearchAdapter(int currIndex) {
//        int start, end;
//        start = currIndex * 8;
//        if( (currIndex + 1) * 8 > searchImageURLs.size()) {
//            end = searchImageURLs.size();
//            currSearchIndex = 0;
//        }
//        else {
//            end = (currIndex + 1) * 8;
//        }
//        ArrayList<String> tempSearchImageURLs = new ArrayList<String>(searchImageURLs.subList(start, end));
//        ArrayList<String> tempSearchStreamNames = new ArrayList<String>(searchStreamNames.subList(start, end));
//        ArrayList<String> tempSearchUserEmails = new ArrayList<String>(searchUserEmails.subList(start, end));
//        loadGridView(view_search, R.id.search_gridView, tempSearchImageURLs, tempSearchStreamNames, tempSearchUserEmails);
//    }

//    AsyncHttpResponseHandler search_handler = new AsyncHttpResponseHandler(){
//        @Override
//        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//            String response = "";
//            try {
//                response = new String(responseBody, "UTF-8");
//                System.out.println(response);
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//            searchImageURLs = new ArrayList<String>();
//            searchStreamNames = new ArrayList<String>();
//            searchUserEmails = new ArrayList<>();
//            try {
//                JSONObject jObject = new JSONObject(response);
//                JSONArray streamsDictArr = jObject.getJSONArray("stream");
//                for (int i = 0; i < streamsDictArr.length(); i++) {
//                    String streamsDict = streamsDictArr.getString(i);
//                    JSONObject jObject2 = new JSONObject(streamsDict);
//
//                    searchStreamNames.add(jObject2.getString("stream_name"));
//                    searchUserEmails.add(jObject2.getString("user_email"));
//                    String coverURL = jObject2.getString("cover_url");
//                    if (coverURL.equals("")) {
//                        coverURL = "https://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
//                    }
//                    searchImageURLs.add(coverURL);
//                }
//            } catch (JSONException j) {
//                Log.v(TAG, j.toString());
//            }
//            search_results_textView.setText(searchImageURLs.size() + " results found.");
//            setSearchAdapter(0);
//        }
//        @Override
//        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {}
//    };

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
                        coverURL = getString(R.string.no_image_url);
                    }
                    imageURLs.add(coverURL);
                }
                swipeContainerSubscribed.setRefreshing(false);
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }

            loadGridView(view_subscribed, R.id.gridView, imageURLs, streamNames, user_emails);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
            swipeContainerSubscribed.setRefreshing(false);
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
                        coverURL = getString(R.string.no_image_url);
                    }
                    imageURLs.add(coverURL);
                }
                swipeContainerAllStreams.setRefreshing(false);
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }

            loadGridView(view_viewAllStreams, R.id.gridView, imageURLs, streamNames, user_emails);
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

    public void getNearbyImages() {
        if (!locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            initLocation();
            return;
        }
        locationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 10, mLocationListener);
        Location location = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());
        if(latitude != null) {
            nearbyIndex = 0;
            client.get(REQUEST_NearbyStreams + "lat=" +  latitude + "&lng=" + longitude, nearby_handler);
        }
    }

//    public void search(View view) {
//        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//        String term = search_editText.getText().toString();
//        client.get(REQUEST_SearchStreams + term, search_handler);
//        Toast.makeText(ViewAllStreamActivity.this, "search " + term, Toast.LENGTH_SHORT).show();
//    }

}
