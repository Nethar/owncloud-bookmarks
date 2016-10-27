package cz.nethar.owncloudbookmarks;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import cz.nethar.owncloudbookmarks.R;

public class BookmarkList extends AppCompatActivity {

    public static Activity thisActivity = null;
    protected static SortedMap<String, BookmarkData> bookmarkMap = null;
    private Boolean faviconsEnabled;
    BookmarkListAdapter adapter;
    protected String toDeleteBookmarkId;
    
    public static Boolean forceRefresh = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark_list);
		thisActivity = this;
		
		Intent intent = getIntent();
		String key = intent.getStringExtra(TagList.KEY);
		bookmarkMap = TagList.getBookmarkMap(key);
		
		if (bookmarkMap == null) {
			Toast.makeText(this, "no bookmarks", Toast.LENGTH_SHORT).show();				
			finish();
			return;
		}
	    
		refreshList();
		
		loadFavicons();
	}
    
	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first
	    if (forceRefresh) {
	    	forceRefresh = false;
	    	refreshList();
	    }
	}
	
	@Override
	public void onPause() {
	    super.onPause();  // Always call the superclass method first
	    faviconsEnabled = false;
	}
	
	private void loadFavicons()
	{
		faviconsEnabled = true;
		
		Object[] arr = bookmarkMap.keySet().toArray();
		for (int i = 0; i < arr.length; i++) {
			new LoadFavicons(i, (String)arr[i]).execute();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_settings, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			forceRefresh = true;
		    return true;
		}
		return true;
	}

	protected void refreshList()
	{
		ListView lv = (ListView)findViewById(R.id.bookmarkView);	
		
		adapter = new BookmarkListAdapter(thisActivity, bookmarkMap);
		lv.setAdapter(adapter);
	
		lv.setOnItemClickListener(new OnItemClickListener() {
			  @Override
			  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				  String url = ((BookmarkData)parent.getItemAtPosition(position)).url;
				  if (!url.startsWith("http://") && !url.startsWith("https://")) {
					  url = "http://" + url;
				  }
				  Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				  startActivity(browserIntent);
				  //thisActivity.finish();
			  	}
		}); 

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							new DeleteBookmark(toDeleteBookmarkId).execute();
							break;

						case DialogInterface.BUTTON_NEGATIVE:
							// No button clicked
							break;
						}
					}
				};

				toDeleteBookmarkId = ((BookmarkData)parent.getItemAtPosition(position)).id;
				  
				AlertDialog.Builder builder = new AlertDialog.Builder(BookmarkList.this);
				builder.setMessage("Do you really want to delete this bookmark?")
						.setPositiveButton("Yes", dialogClickListener)
						.setNegativeButton("No", dialogClickListener).show();
				return true;
              }
          });		
	}
	
	public class DeleteBookmark extends AsyncTask<String, Void, String> {
		String id;
		ProgressDialog progressDialog;
		
		DeleteBookmark(String id)
		{
			this.id = id;
		}
		
        @Override
        protected void onPreExecute()
        {
       		progressDialog = ProgressDialog.show(BookmarkList.this, "", "Deleting bookmark...");
        }
        
		protected String doInBackground(String... urls) {
			try {
				SharedPreferences sp = getSharedPreferences(TagList.PREF_KEY, MODE_PRIVATE);
				String url = sp.getString(TagList.OWNCLOUD_ADDRESS, "");
		        String user = sp.getString(TagList.OWNCLOUD_USERNAME, "");
		        String password = sp.getString(TagList.OWNCLOUD_PASSWORD, "");
		        boolean acceptCert = sp.getBoolean(TagList.OWNCLOUD_ACCEPTWRONGCERT, false);
		        int httpsPort = sp.getInt(TagList.OWNCLOUD_HTTPSPORT, 443);

		        HttpClient httpclient;
		        if (acceptCert) {
		        	httpclient = TagList.httpClientTrustingAllSSLCerts(httpsPort);
		        } else {
		        	httpclient = new DefaultHttpClient();
		        }
		        CookieStore cookieStore = new BasicCookieStore();
    	    	HttpContext localContext = new BasicHttpContext();
    	        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    	        
    	        String requesttoken = "";
    	    	int tries = 0;
    	    	HttpPost httppost;
    	    	HttpResponse response;
    	    	BufferedReader reader;
    	    	String result, line;
    	    	int responseCode;
    	    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

    	    	while (tries < 2) {
    	    		tries++;
	    	        nameValuePairs = new ArrayList<NameValuePair>(2);
	        	    httppost = new HttpPost(url + "index.php");
	    	        nameValuePairs.add(new BasicNameValuePair("user", user));
	    	        nameValuePairs.add(new BasicNameValuePair("password", password));
	    	        nameValuePairs.add(new BasicNameValuePair("password-clone", password));
	    	        nameValuePairs.add(new BasicNameValuePair("timezone-offset", "1"));
	    	        nameValuePairs.add(new BasicNameValuePair("requesttoken", requesttoken));
	    	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));     
	    	        response = httpclient.execute(httppost, localContext);
	    	        
	    	        responseCode = response.getStatusLine().getStatusCode();
	    	        if (responseCode != 200) { 
	    	        	return "Cannot connect to server. Check hostname and username/password (response code  " + responseCode + ").";
	    	        }
	    	        
	    	        reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	        	    result = "";
	    	        while ((line = reader.readLine()) != null) {
	    	        	result += line + "\n";
	    	        }
	    	        
	    	        try {
	    	    		Document doc;
	    	    		doc = Jsoup.parse(result);
	    	    		
	    	    		Elements heads = doc.select("head");
	    	    		if (heads != null) {
	    	    			for (Element head : heads) {
	    	    				requesttoken = head.attr("data-requesttoken");
	    	    			}
	    	    		}    	        	
	    	        } catch (Exception e) {
	    	        	// don't worry, old version probably
	    	        }

	    	        if (requesttoken.equals("") && tries == 1) { // first round, let's assume it is owncloud 7 or higher
		        	    httppost = new HttpPost(url + "index.php");
		    	        response = httpclient.execute(httppost, localContext);  	        
		    	        responseCode = response.getStatusLine().getStatusCode();
		    	        if (responseCode != 200) { 
		    	        	return "Cannot connect to server. Check hostname and username/password.";
		    	        }
		    	        
		    	        if (isCancelled()) {
		    	        	return "cancelled";
		    	        }
		    	        
		    	        reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		        	    result = "";
		    	        while ((result = reader.readLine()) != null) {
		    	        	String[] split = result.trim().split("=");
		    	        	if (split.length > 0 && split[0].equals("<head data-requesttoken")) {
		    	        		requesttoken = split[1].substring(1, split[1].length() - 2);
		    	        	}
		    	        	result += "";
		    	        }	    	        	
	    	        } else if (tries != 1) {
	    	        	break;
	    	        }
    	    	}
    	        
    	        if (requesttoken.equals("")) {
    	        	// old ownCloud version, token not in header but in config.js
	        	    httppost = new HttpPost(url + "index.php/core/js/config.js");
	    	        response = httpclient.execute(httppost, localContext);  	        
	    	        responseCode = response.getStatusLine().getStatusCode();
	    	        if (responseCode != 200) { 
	    	        	return "Cannot connect to server. Check hostname and username/password.";
	    	        }
	    	        
	    	        reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	        	    result = "";
	    	        while ((result = reader.readLine()) != null) {
	    	        	String[] split = result.split("=");
	    	        	if (split.length > 0 && split[0].equals("var oc_requesttoken")) {
	    	        		requesttoken = split[1].substring(1, split[1].length() - 2);
	    	        	}
	    	        	result += "";
	    	        }
    	        }
    	        
    	        // save token
    	        nameValuePairs.add(new BasicNameValuePair("requesttoken", requesttoken));
				
			    httppost = new HttpPost(url + "index.php/apps/bookmarks/ajax/delBookmark.php");
		        nameValuePairs.add(new BasicNameValuePair("id", id));
		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		        response = httpclient.execute(httppost, localContext);
		        
		        responseCode = response.getStatusLine().getStatusCode();
		        if (responseCode != 200) { 
					return "Cannot delete bookmark, HTTP error " + Integer.toString(responseCode) + ".";
		        }
		        
		        reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
    	        String answer = "";
    	        while ((line = reader.readLine()) != null) {
    	        	answer += line + "\n";
    	        }
    	        
   	        	JSONObject jObject = new JSONObject(answer);
   	        	String status = jObject.get("status").toString(); 
   	        	if (status.equalsIgnoreCase("success")) {
   	        		return ""; // OK
   	        	} else {
   	        		return status;
   	        	}

			} catch (Exception e) {
				return "Cannot delete bookmark: " + e.getMessage();	
			}
		}
		
		protected void onPostExecute(String errorMessage) {
			progressDialog.dismiss();
			
			if (errorMessage.length() > 0) {
				Toast.makeText(BookmarkList.this, errorMessage, Toast.LENGTH_LONG).show();				
			} else {
				TagList.forceRefresh = true;
				finish();
			}
		}
	}

	
	class LoadFavicons extends AsyncTask<String, Void, String> {
		
		Drawable image; 
		int index;
		String key;

		LoadFavicons(int index, String key) {
	    	image = null;
	    	this.index = index;
	    	this.key = key;
	    }

		protected String doInBackground(String... urls) {
    		if (!faviconsEnabled) {
    			return "";
    		}
	    		
    	    try {
	        	String url = bookmarkMap.get(key).url;
	        	
	        	if (!url.startsWith("http")) {
	        		url = "http://" + url;
	        	}
	        	
	        	int start = url.indexOf('.');
	        	if (start != -1) {
    	        	start = url.indexOf('/', start + 1);
    	        	if (start != -1) {
    	        		url = url.substring(0, start);
    	        	}
	        	}
	        	
	        	if (url.length() > 1) {   	        	
    	        	if (!url.endsWith("/")) {
    	        		url += "/";
    	        	}
    	        	url += "favicon.ico";

	    			DefaultHttpClient client = new DefaultHttpClient();
	    			HttpParams params = client.getParams();
	    			params.setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
	    			params.setParameter(HttpConnectionParams.SO_TIMEOUT, 5000);
	    			HttpGet httpGet = new HttpGet(url);
	    			HttpResponse httpResponse = client.execute(httpGet);
	    			InputStream is = (java.io.InputStream)
	    			httpResponse.getEntity().getContent();
	    			image = Drawable.createFromStream(is, "src");	    			
	        	}
    	    } catch (Exception e) {
    	    }
    	    
    	    return "";
	    }

		@SuppressWarnings("deprecation")
		protected void onPostExecute(String bookmark) {
	    	if (faviconsEnabled && image != null) {
		    	ListView lv = (ListView)findViewById(R.id.bookmarkView);
			    View firstItemView = lv.getChildAt(0);
			    int iconSize = firstItemView.getHeight();
    			Bitmap d = ((BitmapDrawable)image).getBitmap();
    		    Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, iconSize, iconSize, false); 
    		    image = new BitmapDrawable(bitmapOrig);

    		    if (image != null) {
			    	if (index >= lv.getFirstVisiblePosition() && index <= lv.getLastVisiblePosition()) {
					    View itemView = lv.getChildAt(index - lv.getFirstVisiblePosition());		    		
					    ImageView imageView = (ImageView)itemView.findViewById(R.id.imageView1);				    					
					    imageView.setImageDrawable(image);
			    	} 
    		    	adapter.setImage(index, image);
    		    }
	    	}
	    }
	 }

	
}
