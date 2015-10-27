package apt.connexus.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import apt.connexus.R;

public class ImageAdapter extends BaseAdapter {
    public static final String TAG = "ImageAdapter";
    private Context mContext;
    private ArrayList<String> imageURLs;

    public ImageAdapter(Context c, ArrayList<String> imageURLs) {
        mContext = c;
        this.imageURLs = imageURLs;
    }

    public int getCount() {
        return imageURLs.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.image_view, parent, false);

        }
        imageView = (ImageView) convertView.findViewById(R.id.single_image_view);
//        Log.v(TAG, imageURLs.get(position));
        Picasso.with(mContext).load(imageURLs.get(position)).resize(300,300).centerCrop().into(imageView);
        return convertView;
    }

}
