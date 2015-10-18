package apt.connexus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ViewAllStreamActivity extends Activity {
    private AsyncHttpClient client = new AsyncHttpClient();
    public static final String REQUEST_ViewAllStreams = "http://apt-miniproject-1078.appspot.com/api/view_all";
    public static final String REQUEST_SearchStreams = "http://apt-miniproject-1078.appspot.com/search_request?term=";
    public static final String TAG = "ViewAllStreamActivity";

    final Context context = this;
    private ViewPager viewPager;
    private TextView title_textView1, title_textView2, title_textView3;
    private List<View> views;
    private int offset = 0;
    private int currIndex = 0;
    private View view1,view2,view3;
    private Button search_btn;
    private EditText search_editText;


    private void InitViewPager() {
        viewPager=(ViewPager) findViewById(R.id.vPager);
        views=new ArrayList<View>();
        LayoutInflater inflater=getLayoutInflater();
        view1=inflater.inflate(R.layout.activity_view_all_stream, null);

        view2=inflater.inflate(R.layout.activity_search_result, null);

        search_btn = (Button) view2.findViewById(R.id.search_btn);
        search_btn.setOnClickListener(search_onclick);
        search_editText = (EditText) view2.findViewById(R.id.search_editText);

        view3=inflater.inflate(R.layout.activity_nearby, null);
        views.add(view1);
        views.add(view2);
        views.add(view3);
        viewPager.setAdapter(new MyViewPagerAdapter(views));
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

    public class MyViewPagerAdapter extends PagerAdapter {
        private List<View> mListViews;

        public MyViewPagerAdapter(List<View> mListViews) {
            this.mListViews = mListViews;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) 	{
            container.removeView(mListViews.get(position));
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mListViews.get(position), 0);
            return mListViews.get(position);
        }

        @Override
        public int getCount() {
            return  mListViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0==arg1;
        }
    }

    public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {


        public void onPageScrollStateChanged(int arg0) {
        }

        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        public void onPageSelected(int arg0) {
//            Toast.makeText(ViewAllStreamActivity.this, viewPager.getCurrentItem()+"selected", Toast.LENGTH_SHORT).show();
        }
    }





    View.OnClickListener search_onclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String term = search_editText.getText().toString();
            client.get(REQUEST_SearchStreams+term, search_handler);
            Toast.makeText(ViewAllStreamActivity.this, "search " + term, Toast.LENGTH_SHORT).show();
        }
    };

    AsyncHttpResponseHandler search_handler = new AsyncHttpResponseHandler(){

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

            GridView gridview = (GridView) view2.findViewById(R.id.search_gridView);
            gridview.setAdapter(new ImageAdapter(context, imageURLs));

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

            GridView gridview = (GridView) view1.findViewById(R.id.gridView);
            gridview.setAdapter(new ImageAdapter(context, imageURLs));

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
            Log.e(TAG, "There was a problem in retrieving the url : " + error.toString());
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager_viewall);
        InitTextView();
        InitViewPager();
        client.get(REQUEST_ViewAllStreams, view_all_stream_handler);
    }
}
