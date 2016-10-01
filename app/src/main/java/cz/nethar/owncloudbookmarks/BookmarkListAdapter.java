package cz.nethar.owncloudbookmarks;

import java.util.ArrayList;
import java.util.SortedMap;

import cz.nethar.owncloudbookmarks.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BookmarkListAdapter extends BaseAdapter {
    
    private Activity activity;
    private SortedMap<String, BookmarkData> data;
    private static LayoutInflater inflater = null;
    ArrayList<Drawable> images;
    
    public BookmarkListAdapter(Activity a, SortedMap<String, BookmarkData> d) {
        activity = a;
        data = d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        images = new ArrayList<Drawable>();
        if (data != null) {
	        for (int i = 0; i < data.size(); i++) {
	        	images.add(null);
	        }
        }
    }

    public int getCount() {
    	if (data != null) {
    		return data.size();
    	} else {
    		return 0;
    	}
    }
    
    public boolean areAllItemsEnabled () {
    	return false;
    }
    
    public boolean isEnabled (int position) {
    	return true;
    }

    private int dataPosition(int position) {
    	if (SettingsActivity.sort == TagList.ALPHA_DESC || SettingsActivity.sort == TagList.ALPHA_NOTAGFIRST_DESC) {
        	position = data.size() - 1 - position;    		    		
    	}
    	return position;
    }
    
    public Object getItem(int position) {
    	String key = (String)data.keySet().toArray()[dataPosition(position)];    		
    	return data.get(key);
    }

    public long getItemId(int position) {
        return position;
    }
    
    public void setImage(int index, Drawable image)
    {
    	images.set(index, image);
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        
        if (convertView == null) {
        	// first call
            vi = inflater.inflate(R.layout.bookmark_item, null);
        }

        TextView mainView = (TextView)vi.findViewById(R.id.textMain);
        String key = (String)data.keySet().toArray()[dataPosition(position)];
        mainView.setText(key);

        TextView descView = (TextView)vi.findViewById(R.id.textDescription);
        String url = data.get(key).url;
        descView.setText(url);
        
    	ImageView imageView = (ImageView)vi.findViewById(R.id.imageView1);
    	
        if (images.get(position) != null) {
        	//mainView.setCompoundDrawablesWithIntrinsicBounds(null, null, images.get(position), null);
        	imageView.setImageDrawable(images.get(position));
        } else {
        	//mainView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);        	
        	imageView.setImageDrawable(null);
        }

        return vi;
    }
}
