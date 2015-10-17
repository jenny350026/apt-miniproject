package apt.connexus;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<String> imageURLs;
    public static final String TAG = "ImageAdapter";

    public ImageAdapter(Context c, ArrayList<String> imageURLs) {
        mContext = c;
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
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(200, 200));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

//        ImageLoader imageLoader = ImageLoader.getInstance();
//        imageLoader.init(ImageLoaderConfiguration.createDefault(mContext));
//        imageLoader.displayImage("http://apt-miniproject-1078.appspot.com/img?img_id=ahZzfmFwdC1taW5pcHJvamVjdC0xMDc4chILEgVJbWFnZRiAgICA-Ja1CAw.png", imageView);
//

        Log.v(TAG, imageURLs.get(position));
//        Picasso.with(mContext).load("http://apt-miniproject-1078.appspot.com/img?img_id=ahZzfmFwdC1taW5pcHJvamVjdC0xMDc4chILEgVJbWFnZRiAgICA-Ja1CAw").into(imageView);
        Picasso.with(mContext).load(imageURLs.get(position)).into(imageView);
        return imageView;
    }
}
