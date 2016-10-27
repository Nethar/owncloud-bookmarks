package cz.nethar.owncloudbookmarks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class TagList extends AppCompatActivity {

    public static final String OWNCLOUD_ADDRESS = "oc_url";
    public static final String OWNCLOUD_USERNAME = "oc_user";
    public static final String OWNCLOUD_PASSWORD = "oc_pass";
    public static final String OWNCLOUD_CACHED = "oc_cache";
    public static final String OWNCLOUD_SORTING = "oc_sort";
    public static final String OWNCLOUD_HTTPSPORT = "oc_httpsport";
    public static final String OWNCLOUD_LASTVERSION = "oc_version";
    public static final String OWNCLOUD_ACCEPTWRONGCERT = "oc_accept_cert";
    public static final String ACCOUNT_TYPE = "owncloud";
    public static final String PREF_KEY = "owncloud_bookmarks";
    public static final String KEY = "key";
    public static final String NOTAG = "(no tag)";
    
    // SORTING STYLE
    public static final int ALPHA_ASC = 0;
    public static final int ALPHA_DESC = 1;
    public static final int ALPHA_NOTAGFIRST_ASC = 2;
    public static final int ALPHA_NOTAGFIRST_DESC = 3;
    
    private RetreiveBookmarkTask task = null;
    private int scrollYindex = 0;
    private int scrollYexact = 0;
    
    public class BookmarkMaps {
    	public SortedMap<String, SortedMap<String, BookmarkData>> bookmarks;
    	public SortedMap<String, BookmarkData> noTagBookmarks;
    	
    	BookmarkMaps() {
    		bookmarks = new TreeMap<String, SortedMap<String, BookmarkData>>(String.CASE_INSENSITIVE_ORDER);
    		noTagBookmarks = new TreeMap<String, BookmarkData>(String.CASE_INSENSITIVE_ORDER);
    	}
    	
    	Boolean isEmpty() {
    		return (bookmarks.isEmpty() && noTagBookmarks.isEmpty());
    	}
    	
    	int count() {
    		int c = noTagBookmarks.size();
    		
    		Object[] arr = bookmarks.keySet().toArray();
    		for (int i = 0; i < arr.length; i++) {
    			c += bookmarks.get((String)arr[i]).size();
    		}
    		
    		return c;
    	}
    }
    
    public static BookmarkMaps loadedBookmarks = null; 
    public static BookmarkMaps loadedBookmarksUpdated = null;
    
    public static Boolean forceRefresh = false;
    
    private static String toDeleteBookmarkId;
    
    /*Comparator<String> tagComparator = new Comparator<String>() {
        @Override public int compare(String s1, String s2) {
        	char first = NOTAG.charAt(0);
        	
        	if (s1.length() > 0 && s1.charAt(0) == first) {
        		if (s2.length() == 0 || s2.charAt(0) != first) {
        			return 1;
        		}
        	}
        	if (s2.length() > 0 && s2.charAt(0) == first) {
        		if (s1.length() == 0 || s1.charAt(0) != first) {
        			return -1;
        		}
        	}
        	
            return s1.compareToIgnoreCase(s2);
        }           
    };*/

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tag_list);
		
        SharedPreferences sp = getSharedPreferences(PREF_KEY, MODE_PRIVATE);      
        int ver = sp.getInt(OWNCLOUD_LASTVERSION, 0);
        
        int VERSION = 0;
        try {
	        PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
	        if (pInfo != null) {
	        	VERSION = pInfo.versionCode;
	        }
	    } catch (Exception e) {
        }
       	if (ver < VERSION) {
        	Editor edit = sp.edit();
        	edit.putInt(OWNCLOUD_LASTVERSION, VERSION);
        	edit.commit();

        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle("New in this version");
    		builder.setMessage("Fixed login issue in oc 7.0.3")
    				.setPositiveButton("OK", new DialogInterface.OnClickListener() {  
    					   public void onClick(DialogInterface dialog, int id) {  
    						     dialog.dismiss(); 
    						}  
    						}).show();
        }
        
        SettingsActivity.sort = sp.getInt(OWNCLOUD_SORTING, ALPHA_ASC);
    }
    
	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first
	    if (loadedBookmarks == null || forceRefresh) {
	    	refreshList(forceRefresh);
	    	forceRefresh = false;
	    } else {
	    	if (loadedBookmarksUpdated != null) {
	    		loadedBookmarks = loadedBookmarksUpdated;
	    		loadedBookmarksUpdated = null;
	    	}
	    	fillListView();
	    }
	}
	
	@Override
	public void onStop() {
	    super.onStop();
	    if (task != null) {
	    	if (task.progressDialog != null) {
	    		task.progressDialog.dismiss();
	    		task.progressDialog = null;
	    	}
	    	task.cancel(false);
	    	task = null;
	    }
	}

	/*@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig); // Always call the superclass method first
	    fillListView();
	}*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_taglist_settings, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings: 
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		}
		    return true;
		case R.id.menu_new_bookmark:
		{
			Intent intent = new Intent(this, CreateBookmark.class);
			startActivity(intent);
		}
			return true;
		case R.id.menu_refresh:
			refreshList(true);
		    return true;
		}
		return true;
	}

	/*// Loads from HTML
	protected SortedMap<String, SortedMap<String, BookmarkData>> loadBookmarks(String bookmarks)
	{
		SortedMap<String, SortedMap<String, BookmarkData>> map = new TreeMap<String, SortedMap<String, BookmarkData>>(String.CASE_INSENSITIVE_ORDER);
		
		Document doc;
		doc = Jsoup.parse(bookmarks);
		
		Elements links = doc.select("a");
		if (links != null) {
			for (Element link : links) {
				String linkHref = link.attr("HREF");
				String linkTags = link.attr("TAGS");
				String linkText = link.text();
				if (linkTags.startsWith("{") && linkTags.endsWith("}")) {
					linkTags = linkTags.substring(1, linkTags.length() - 1);
				}
				String[] tags = linkTags.split(",");
				for (String tag : tags) {
					tag = tag.trim();
					if (tag.length() == 0) {
						tag = "(no tag)";
					}
					SortedMap<String, BookmarkData> sm = map.get(tag);
					if (sm != null) {
						sm.put(linkText, linkHref);
					} else {
						sm = new TreeMap<String, BookmarkData>(String.CASE_INSENSITIVE_ORDER);
						sm.put(linkText, linkHref);
						map.put(tag, sm);					
					}
				}
			}
		}
		
		return map;
	}*/
	
	public static SortedMap<String, BookmarkData> getBookmarkMap(String key)
	{
		if (loadedBookmarks == null) {
			return null; // something is wrong
		}
		
		if (key.equalsIgnoreCase(NOTAG)) {
			return loadedBookmarks.noTagBookmarks;
		} else {
			if (loadedBookmarks.bookmarks != null) {
				return loadedBookmarks.bookmarks.get(key);
			} else {
				return null; // something is wrong
			}
		}
	}

	protected BookmarkMaps loadBookmarks(JSONArray bookmarks)
	{
		BookmarkMaps maps = new BookmarkMaps();
		
		try {
			for (int i = 0; i < bookmarks.length(); i++) {
				JSONObject field = bookmarks.getJSONObject(i);
				String title = field.getString("title");
				String url = field.getString("url");
				String id = field.getString("id");
				int added = field.getInt("added");
				JSONArray tags = field.getJSONArray("tags");
				for (int j = 0; j < tags.length(); j++) {
					String tag = tags.getString(j).trim();
					if (tag.length() == 0) {
						//tag = NOTAG;
						maps.noTagBookmarks.put(title, new BookmarkData(id, url, added));
					} else {
						addBookmark(maps.bookmarks, tag, title, url, id, added);
					}
				}
			}
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();			
		}
		
		return maps;
	}

	protected void addBookmark(SortedMap<String, SortedMap<String, BookmarkData>> map, String tag, String title, String url, String id, int added) 
	{
		SortedMap<String, BookmarkData> sm = map.get(tag);
		if (sm != null) {
			sm.put(title, new BookmarkData(id, url, added));
		} else {
			sm = new TreeMap<String, BookmarkData>(String.CASE_INSENSITIVE_ORDER);
			sm.put(title, new BookmarkData(id, url, added));
			map.put(tag, sm);					
		}
	}
	
	protected void refreshList(Boolean forceReload)
	{
        SharedPreferences sp = getSharedPreferences(PREF_KEY, MODE_PRIVATE);      
        String url = sp.getString(OWNCLOUD_ADDRESS, "");
        String user = sp.getString(OWNCLOUD_USERNAME, "");
        String password = sp.getString(OWNCLOUD_PASSWORD, "");
        String cachedBookmarks = sp.getString(OWNCLOUD_CACHED, "");
        boolean acceptCert = sp.getBoolean(OWNCLOUD_ACCEPTWRONGCERT, false);
        int httpsPort = sp.getInt(OWNCLOUD_HTTPSPORT, 443);
        
        if (url.length() == 0 || user.length() == 0 || password.length() == 0) {
			Toast.makeText(this, "You must provide valid server url, username and password in options dialog", Toast.LENGTH_LONG).show();
			return;
        }
        
        if (!forceReload && cachedBookmarks != null && cachedBookmarks.length() > 0) {
        	try {
        		JSONArray array = new JSONArray(cachedBookmarks);     	
        		loadedBookmarks = loadBookmarks(array);
        		loadedBookmarksUpdated = null;
        		fillListView();
        		if (task != null) {
        			task.cancel(false);
        		}
        		task = new RetreiveBookmarkTask(url, user, password, acceptCert, true, httpsPort);
        		task.execute();
        	} catch (Exception e) {
        		if (task != null) {
        			task.cancel(false);
        		}
        		task = new RetreiveBookmarkTask(url, user, password, acceptCert, false, httpsPort);
        		task.execute();        	
        	}
        } else {
    		if (task != null) {
    			task.cancel(false);
    		}
            task = new RetreiveBookmarkTask(url, user, password, acceptCert, false, httpsPort);
            task.execute();        	
        }
	}
	
	@SuppressLint("TrulyRandom")
	public static DefaultHttpClient httpClientTrustingAllSSLCerts(int httpsPort)
			throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException {
		DefaultHttpClient httpclient = new DefaultHttpClient();

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, getTrustingManager(), new java.security.SecureRandom());

		SSLSocketFactory socketFactory = new MySSLSocketFactory(sc);
		Scheme sch = new Scheme("https", socketFactory, httpsPort);
		httpclient.getConnectionManager().getSchemeRegistry().register(sch);
		return httpclient;
	}

	private static TrustManager[] getTrustingManager() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
				// Do nothing
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
				// Do nothing
			}

		} };
		return trustAllCerts;
	}
   
    class RetreiveBookmarkTask extends AsyncTask<String, Void, String> {

	    private String url;
	    private String user;
	    private String password;
	    private boolean acceptCert;
	    private Exception exception;   
	    ProgressDialog progressDialog;
	    private Boolean background;
	    private int httpsPort;

        @Override
        protected void onPreExecute()
        {
        	if (!background) {
        		progressDialog = ProgressDialog.show(TagList.this, "", "Loading bookmarks...");
        	}
        }
        
        RetreiveBookmarkTask(String url, String user, String password, boolean acceptCert, Boolean background, int httpsPort) {
	    	this.url = url;
	    	this.user = user;
	    	this.password = password;
	    	this.acceptCert = acceptCert;
	    	exception = null;
	    	this.background = background;
	    	this.httpsPort = httpsPort;
	    }

	    protected String doInBackground(String... urls) {
    	    try {
    	    	HttpClient httpclient;   	    	
    	    	if (acceptCert) {
    	    		httpclient = httpClientTrustingAllSSLCerts(httpsPort);
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
	    	        
	    	        if (isCancelled()) {
	    	        	return "cancelled";
	    	        }
	    	        
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
	    	        	// don't worry, old version probably (or new version)
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
    	        	// token not in header, old ownCloud version, read config.js
	        	    httppost = new HttpPost(url + "index.php/core/js/config.js");
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
	    	        	String[] split = result.split("=");
	    	        	if (split.length > 0 && split[0].equals("var oc_requesttoken")) {
	    	        		requesttoken = split[1].substring(1, split[1].length() - 2);
	    	        	}
	    	        	result += "";
	    	        }
    	        }

    	        // save token
    	        nameValuePairs.add(new BasicNameValuePair("requesttoken", requesttoken));
    	        
    	        if (isCancelled()) {
    	        	return "cancelled";
    	        }
    	        
    	        String currList = "";
    	        int index = 0;
    	        Boolean run = true;
    	        JSONArray completeData = new JSONArray();
    	        do {
        	        if (isCancelled()) {
        	        	return "cancelled";
        	        }
        	       
	    	        httppost = new HttpPost(url + "index.php/apps/bookmarks/ajax/updateList.php?type=bookmark&requesttoken=" + requesttoken + "&page=" + Integer.toString(index));
	    	        // this request not working with post arguments 
	    	        //nameValuePairs = new ArrayList<NameValuePair>(2);
	    	        //nameValuePairs.add(new BasicNameValuePair("type", "bookmark"));
	    	        //nameValuePairs.add(new BasicNameValuePair("page", "2"));
	    	        //httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));     
	    	        response = httpclient.execute(httppost, localContext);  	        
	    	        responseCode = response.getStatusLine().getStatusCode();
	    	        if (responseCode != 200) { 
	    	        	return "Cannot update bookmarks list.";
	    	        }  	        
	    	        reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	    	        currList = "";
	    	        while ((line = reader.readLine()) != null) {
	    	        	currList += line + "\n";
	    	        }
	    	        index++;
	    	        
	    	        try {
	    	        	JSONObject jObject = new JSONObject(currList);
	    	        	JSONArray arr = jObject.getJSONArray("data");
	    	        	if (arr == null || arr.length() == 0) {
	    	        		run = false; // last iteration reached
	    	        	} else {
	    	        		for (int i = 0; i < arr.length(); i++) {
	    	        			JSONObject field = arr.getJSONObject(i);
		    	        		completeData.put(field);
	    	        		}
	    	        	}
	    	        } catch (Exception e) {
	    	        	return "Cannot parse JSON answer (" + Integer.toString(index) + "). Wrong username/password?";	    	        	
	    	        }
	    	        
    	        } while (run);
    	           	        
    	        if (isCancelled()) {
    	        	return "cancelled";
    	        }
    	        
    	        loadedBookmarksUpdated = loadBookmarks(completeData);
    	        
    	        // NEW VERSION OF BOOKMARKS LOADED
    	        SharedPreferences sp = getSharedPreferences(PREF_KEY, MODE_PRIVATE);      
    	        Editor editor = sp.edit();   
    	        editor.putString(OWNCLOUD_CACHED, completeData.toString());
    	        editor.commit();

    	        if (loadedBookmarksUpdated.isEmpty()) {
    	        	return "No bookmarks received. Do you have any? File export.php not fixed?";
    	        }
    	        
    	        if (loadedBookmarksUpdated.count() == 1) {
    	        	return "Only one bookmark received. Wrong login data?";    	        	
    	        }
    	        
    	    } catch (Exception e) {
    	    	exception = e;
    	    	loadedBookmarksUpdated = null;
    	    }
    	    
    	    return "";
	    }
	    
	    protected void onCancelled(String errorMessage) {
	    	if (!background && progressDialog != null) {
	    		progressDialog.dismiss();
	    		progressDialog = null;
	    	}	    	
	    	//Toast.makeText(TagList.this, errorMessage, Toast.LENGTH_SHORT).show();
	    }

	    protected void onPostExecute(String errorMessage) {
	    	if (!background && progressDialog != null) {
	    		progressDialog.dismiss();
	    		progressDialog = null;
	    	}
	    	
	    	if (exception != null) {
	    		if (exception.getClass() == SSLPeerUnverifiedException.class) {
	    			Toast.makeText(TagList.this, exception.getMessage() + " (import certificate to your Android trusted credentials storage or accept it in app settings)", Toast.LENGTH_LONG).show();	    			
	    		} else {
	    			Toast.makeText(TagList.this, exception.getMessage() + " " + exception.getClass().toString(), Toast.LENGTH_LONG).show();
	    		}
	    	} else if (errorMessage != null && errorMessage.length() > 0) {
    			Toast.makeText(TagList.this, errorMessage, Toast.LENGTH_LONG).show();	    		
	    	} else {
	    		if (!background) {
	    			loadedBookmarks = loadedBookmarksUpdated;
	    			loadedBookmarksUpdated = null;
	    			fillListView();
	    		}
	    	}
	    }
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
       		progressDialog = ProgressDialog.show(TagList.this, "", "Deleting bookmark...");
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
				Toast.makeText(TagList.this, errorMessage, Toast.LENGTH_LONG).show();				
			} else {
				refreshList(true);
			}
		}
	}

	void fillListView()
	{
		ListView lv = (ListView)findViewById(R.id.tagView);	
		
		TagListAdapter adapter = new TagListAdapter(TagList.this, loadedBookmarks);
		lv.setAdapter(adapter);
		if (scrollYindex != 0 || scrollYexact != 0) {
			lv.setSelectionFromTop(scrollYindex, scrollYexact);
		}
	
		lv.setOnItemClickListener(new OnItemClickListener() {
			  @Override
			  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				  scrollYindex = parent.getFirstVisiblePosition();
				  View v = parent.getChildAt(0);
				  scrollYexact = (v == null) ? 0 : v.getTop();
				  
				  TagListAdapter adapter = (TagListAdapter)parent.getAdapter();
				  if (adapter.isTag(position)) {
						Intent intent = new Intent(TagList.this, BookmarkList.class);
						intent.putExtra(KEY, adapter.getKey(position));
						startActivity(intent);					  
				  } else {
					  TextView desc = (TextView)view.findViewById(R.id.textDescription);
					  String url = desc.getText().toString();
					  if (!url.startsWith("http://") && !url.startsWith("https://")) {
						  url = "http://" + url;
					  }
					  Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					  startActivity(browserIntent);
				  }
			  }
			}); 		

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

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
			
			@Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				  TagListAdapter adapter = (TagListAdapter)parent.getAdapter();
				  if (adapter.isTag(position)) {
					  return false; // nothing to do (or open all?)
				  } else {
					  //Toast.makeText(TagList.this, "deleting bookmarks without tag not yet implemented", Toast.LENGTH_SHORT).show();
					  
					  BookmarkData bd = adapter.getNTData(position);
					  toDeleteBookmarkId = bd.id;
						AlertDialog.Builder builder = new AlertDialog.Builder(TagList.this);
						builder.setMessage("Do you really want to delete this bookmark?")
								.setPositiveButton("Yes", dialogClickListener)
								.setNegativeButton("No", dialogClickListener).show();
					  return true;
				  }
            }
        });		
	}


}
