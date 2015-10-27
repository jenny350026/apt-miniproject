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

public class StreamAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<String> imageURLs, streamNames;
    public static final String TAG = "ImageAdapter";
    private TextView textView;

    public StreamAdapter(Context c, ArrayList<String> imageURLs) {
        mContext = c;
        this.imageURLs = imageURLs;
    }

    public StreamAdapter(Context c, ArrayList<String> imageURLs, ArrayList<String> streamNames) {
        mContext = c;
        this.streamNames = streamNames;
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
            convertView = a.inflate(R.layout.image_adapter, parent, false);
        }
        imageView = (ImageView) convertView.findViewById(R.id.adapter_imageView);
        textView = (TextView) convertView.findViewById(R.id.adapter_textView);


//        Log.v(TAG, imageURLs.get(position));
        Picasso.with(mContext).load(imageURLs.get(position)).resize(200, 200).centerCrop().into(imageView);
        if(streamNames != null) {
            textView.setText(streamNames.get(position));
        }
        return convertView;
    }
}
