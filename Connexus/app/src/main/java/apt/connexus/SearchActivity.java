package apt.connexus;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import apt.connexus.adapters.StreamAdapter;
import cz.msebera.android.httpclient.Header;


public class SearchActivity extends ActionBarActivity {

    private static final String TAG = "SearchActivity";
    private AsyncHttpClient client = new AsyncHttpClient();
    private TextView search_results_textView;
    private static ArrayList<String> searchImageURLs, searchStreamNames, searchUserEmails;
    private int currSearchIndex = 0;
    private Button more_search_button;

    public final String REQUEST_SearchStreams = "http://apt-miniproject-1078.appspot.com/api/search_request?term=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_result);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Log.v(TAG, "Activity started");
        more_search_button = (Button) findViewById(R.id.more_search_button);

        search_results_textView = (TextView) findViewById(R.id.search_results_textView);

        Intent intent = getIntent();

        Bundle appData = getIntent().getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            client = (AsyncHttpClient) appData.getSerializable("client");
            Log.v(TAG, "received client");
        }

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.v(TAG, "received query");
//            client = intent.getSerializableExtra();
            setTitle(Html.fromHtml("Search Results for <i>" + query + "</i>"));
            search(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void search(String term) {
//        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        client.get(REQUEST_SearchStreams + term, search_handler);
        Toast.makeText(SearchActivity.this, "search " + term, Toast.LENGTH_SHORT).show();
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
                if(searchImageURLs.size() < number_of_images_in_search_page + 1)
                    more_search_button.setVisibility(View.GONE);
            } catch (JSONException j) {
                Log.v(TAG, j.toString());
            }
            search_results_textView.setText(searchImageURLs.size() + " results found.");
            setSearchAdapter(0);
        }
        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {}
    };

    public void moreSearch(View view) {
        currSearchIndex++;
        setSearchAdapter(currSearchIndex);
    }

    private final int number_of_images_in_search_page = 9;

    private void setSearchAdapter(int currIndex) {
        int start, end;
        start = currIndex * number_of_images_in_search_page;
        if( (currIndex + 1) * number_of_images_in_search_page > searchImageURLs.size()) {
            end = searchImageURLs.size();
            currSearchIndex = -1;
        }
        else {
            end = (currIndex + 1) * number_of_images_in_search_page;
        }
        ArrayList<String> tempSearchImageURLs = new ArrayList<String>(searchImageURLs.subList(start, end));
        ArrayList<String> tempSearchStreamNames = new ArrayList<String>(searchStreamNames.subList(start, end));
        ArrayList<String> tempSearchUserEmails = new ArrayList<String>(searchUserEmails.subList(start, end));
        loadGridView(R.id.search_gridView, tempSearchImageURLs, tempSearchStreamNames, tempSearchUserEmails);
    }

    private void loadGridView(int id,
                              final ArrayList<String> imageURLs,
                              final ArrayList<String> streamNames,
                              final ArrayList<String> user_emails) {

        GridView gridview = (GridView) findViewById(id);
        gridview.setAdapter(new StreamAdapter(this, imageURLs, streamNames));

        gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(SearchActivity.this, streamNames.get(position), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(SearchActivity.this, ViewSingleActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("streamName", streamNames.get(position));
                intent.putExtra("userEmail", user_emails.get(position));
                startActivity(intent);
            }
        });
    }


}
