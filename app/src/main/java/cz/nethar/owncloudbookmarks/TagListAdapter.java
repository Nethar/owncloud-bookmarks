package cz.nethar.owncloudbookmarks;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TagListAdapter extends BaseAdapter {
    
	private List<View> views = null;
	
    private Activity activity;
    BookmarkMaps data;
    private static LayoutInflater inflater = null;
    int count;
    
    public TagListAdapter(Activity a, BookmarkMaps d) {
        activity = a;
        data = d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        if (data == null) {
        	count = 0;
        } else {
        	count = data.getTagedBookmarks().size() + data.getNoTagBookmakrs().size();
        }
        views = new ArrayList<View>(count);
        for (int i = 0; i < count; i++) {
        	views.add(null);
        }
    }

    @Override
    public int getCount() {
        return count;
    }
    
    @Override
    public int getViewTypeCount() {
      return getCount();
    }
    
	@Override
	public int getItemViewType(int position) {
		return position;
	}

    public boolean areAllItemsEnabled () {
    	return false;
    }
    
    public boolean isEnabled (int position) {
    	return true;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }
    
    public Boolean isTag(int position) {
    	if (position < data.getTagedBookmarks().size() && (SettingsActivity.sort == TagList.ALPHA_ASC || SettingsActivity.sort == TagList.ALPHA_DESC)) {
    		return true;
    	} else if (position >= data.getNoTagBookmakrs().size() && (SettingsActivity.sort == TagList.ALPHA_NOTAGFIRST_ASC || SettingsActivity.sort == TagList.ALPHA_NOTAGFIRST_DESC)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    BookmarkData getNTData(int position) {
    	return data.getNoTagBookmakrs().get(getKey(position));
    }
    
    public String getKey(int position) {
    	if (isTag(position)) {
	    	if (SettingsActivity.sort == TagList.ALPHA_NOTAGFIRST_ASC || SettingsActivity.sort == TagList.ALPHA_NOTAGFIRST_DESC) {
	    		position -= data.getNoTagBookmakrs().size();
	    	}
	    	
	    	if (SettingsActivity.sort == TagList.ALPHA_ASC || SettingsActivity.sort == TagList.ALPHA_NOTAGFIRST_ASC) {
	    		return (String)data.getTagedBookmarks().keySet().toArray()[position];
	    	} else {
	    		return (String)data.getTagedBookmarks().keySet().toArray()[data.getTagedBookmarks().size() - 1 - position];
	    	}
    	} else {
	    	if (SettingsActivity.sort == TagList.ALPHA_ASC || SettingsActivity.sort == TagList.ALPHA_DESC) {
	    		position -= data.getTagedBookmarks().size();
	    	}
	    	
	    	if (SettingsActivity.sort == TagList.ALPHA_ASC || SettingsActivity.sort == TagList.ALPHA_NOTAGFIRST_ASC) {
	        	return (String)data.getNoTagBookmakrs().keySet().toArray()[position];
	    	} else {
	        	return (String)data.getNoTagBookmakrs().keySet().toArray()[data.getNoTagBookmakrs().size() - 1 - position];
	    	}    		
    	}
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
    	View result = views.get(position);
        
        if (result == null) {
        	if (isTag(position)) {
       			result = inflater.inflate(R.layout.tag_item, null);
        	} else {
        		result = inflater.inflate(R.layout.bookmark_item, null);
        	}
        	views.set(position, result);
        }
        
        if (isTag(position)) {
            String key = getKey(position);
            TextView main = (TextView)result.findViewById(R.id.textMain);
            main.setText(key);        	
        } else {
            String key = getKey(position);
            TextView main = (TextView)result.findViewById(R.id.textMain);
            main.setText(key);        	
            TextView desc = (TextView)result.findViewById(R.id.textDescription);
            desc.setText(data.getNoTagBookmakrs().get(key).url);

        	ImageView imageView = (ImageView)result.findViewById(R.id.imageView1);       	
           	imageView.setImageDrawable(null); // TODO favicon
        }
        
        return result;
    }
}
