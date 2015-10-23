package apt.connexus.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import apt.connexus.R;

public class NearbyAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<String> imageURLs, distances;
    public static final String TAG = "ImageAdapter";
    private TextView textView;

    public NearbyAdapter(Context c, ArrayList<String> imageURLs) {
        mContext = c;
        this.imageURLs = imageURLs;
    }

    public NearbyAdapter(Context c, ArrayList<String> imageURLs, ArrayList<String> distances) {
        mContext = c;
        this.distances = distances;
        this.imageURLs = imageURLs;
    }

    @Override
    public int getCount() {
        return imageURLs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {  // if it's not recycled, initialize some attributes
            LayoutInflater a = LayoutInflater.from(mContext);
            convertView = a.inflate(R.layout.image_adapter, null);
        }
        imageView = (ImageView) convertView.findViewById(R.id.adapter_imageView);
        textView = (TextView) convertView.findViewById(R.id.adapter_textView);


//        Log.v(TAG, imageURLs.get(position));
        Picasso.with(mContext).load(imageURLs.get(position)).resize(200, 200).into(imageView);
        if(distances != null) {
            String distance = distances.get(position);
            textView.setText(distance.substring(0, distance.lastIndexOf(".")));
        }
        return convertView;
    }
}
