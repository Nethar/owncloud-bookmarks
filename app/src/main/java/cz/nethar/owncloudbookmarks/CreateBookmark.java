package cz.nethar.owncloudbookmarks;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CreateBookmark extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_bookmark);
		
		Intent intent = getIntent();
		String action = intent.getAction();
		if (Intent.ACTION_SEND.equalsIgnoreCase(action) && (intent.hasExtra(Intent.EXTRA_TEXT))) {
		    String s = intent.getStringExtra(Intent.EXTRA_TEXT); 
    		EditText et = (EditText)this.findViewById(R.id.editTextUrl);
    		et.setText(s);
    		 		
			// try to load page title
    		LongOperation task = new LongOperation();
    		task.execute(s);
		}		
	}
	
	private class LongOperation extends AsyncTask<String, Void, String> {
		
		String errorMessage = "";
		
        @Override
        protected String doInBackground(String... params) {
			try {
				URL u = new URL(params[0]);
				URLConnection conn = u.openConnection();

				InputStream in = conn.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				int n = 0, totalRead = 0;
				char[] buf = new char[1024];
				StringBuilder content = new StringBuilder();

				// read until EOF or first 8192 characters
				while (totalRead < 8192 && (n = reader.read(buf, 0, buf.length)) != -1) {
					content.append(buf, 0, n);
					totalRead += n;
				}
				reader.close();

				// extract the title
				Pattern titleTag = Pattern.compile("\\<title>(.*)\\</title>", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
				Matcher matcher = titleTag.matcher(content);
				if (matcher.find()) {
					String title = matcher.group(1).replaceAll("[\\s\\<>]+", " ").trim();
					return title;
				} 
			} catch (Exception e) {
				errorMessage = e.getMessage();
				return "";
			}
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
        	if (result.equals("")) {
        		Toast.makeText(CreateBookmark.this, "Cannot retrieve page title automatically: " + errorMessage, Toast.LENGTH_SHORT).show();
        	}
			EditText et2 = (EditText)CreateBookmark.this.findViewById(R.id.editTextTitle);
    		et2.setText(result);
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_settings, menu);
		return true;
	}
	
	public void saveClick(View view) {
		EditText etUrl = (EditText)this.findViewById(R.id.editTextUrl);
		EditText etTitle = (EditText)this.findViewById(R.id.editTextTitle);
		EditText etDesc = (EditText)this.findViewById(R.id.editTextDescription);
		EditText etTag = (EditText)this.findViewById(R.id.editTextTag);
		
		String url = etUrl.getText().toString();
		String title = etTitle.getText().toString();
		String desc = etDesc.getText().toString();
		String tag = etTag.getText().toString();
		
		if (url == null || url.length() == 0) {
			Toast.makeText(this, "URL of new bookmark cannot be empty", Toast.LENGTH_SHORT).show();
			return;
		}
		if (title == null || title.length() == 0) {
			Toast.makeText(this, "Title of new bookmark cannot be empty", Toast.LENGTH_SHORT).show();
			return;
		}
		
		new EditBookmark(url, title, desc, tag).execute();
	}
	
	class EditBookmark extends AsyncTask<String, Void, String> {
		String bookmarkurl, title, desc, tag;
		ProgressDialog progressDialog;
		
		EditBookmark(String url, String title, String desc, String tag)
		{
			this.bookmarkurl = url;
			this.title = title;
			this.desc = desc;
			this.tag = tag;
		}
		
        @Override
        protected void onPreExecute()
        {
       		progressDialog = ProgressDialog.show(CreateBookmark.this, "", "Creating bookmark...");
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
	    	        
	    	        requesttoken = "";
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
    	        
			    httppost = new HttpPost(url + "index.php/apps/bookmarks/ajax/editBookmark.php");
		        nameValuePairs.add(new BasicNameValuePair("url", bookmarkurl));
		        nameValuePairs.add(new BasicNameValuePair("description", desc));
		        nameValuePairs.add(new BasicNameValuePair("title", title));
		        nameValuePairs.add(new BasicNameValuePair("item[tags][]", tag));
		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		        response = httpclient.execute(httppost, localContext);
		        
		        responseCode = response.getStatusLine().getStatusCode();
		        if (responseCode != 200) { 
					return "Cannot create bookmark, HTTP error " + Integer.toString(responseCode) + ".";
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
				return "Cannot add bookmark: " + e.getMessage();			
			}
		}
		
		protected void onPostExecute(String errorMessage) {
			progressDialog.dismiss();
			
			if (errorMessage.length() > 0) {
				Toast.makeText(CreateBookmark.this, errorMessage, Toast.LENGTH_LONG).show();				
			} else {
				Toast.makeText(CreateBookmark.this, "Bookmark \"" + title + "\" successfully created.", Toast.LENGTH_LONG).show();
				TagList.forceRefresh = true;
				finish();
			}
		}
	}

}
