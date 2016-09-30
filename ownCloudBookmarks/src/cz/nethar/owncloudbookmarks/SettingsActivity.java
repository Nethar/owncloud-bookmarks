package cz.nethar.owncloudbookmarks;

import cz.nethar.owncloudbookmarks.R;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SettingsActivity extends Activity {

	private String url, user, pass;
	private boolean trust;
	public static int sort;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
        SharedPreferences sp = getSharedPreferences(TagList.PREF_KEY, MODE_PRIVATE);      
		url = sp.getString(TagList.OWNCLOUD_ADDRESS, "");
		trust = sp.getBoolean(TagList.OWNCLOUD_ACCEPTWRONGCERT, false);
		user = sp.getString(TagList.OWNCLOUD_USERNAME, "");
		pass = sp.getString(TagList.OWNCLOUD_PASSWORD, "");
		
		if (url.length() == 0 && user.length() == 0) {
	        AccountManager accountManager = AccountManager.get(getApplicationContext());
	        Account[] accounts = accountManager.getAccountsByType("owncloud");
	        
	        if (accounts.length > 0) {
	        	Account a = accounts[0];
	        	String[] all = a.name.split("@");
	        	if (all.length == 2) {
	        		user = all[0];
	        		url = all[1];
	        		if (!url.contains("http")) {
	        			url = "http://" + url;
	        		}
	        	}
	        }
		}
        
        EditText et = (EditText)this.findViewById(R.id.editTextUrl);
        et.setText(url);
        
        CheckBox trustCH = (CheckBox)this.findViewById(R.id.trustCheckBox);
        trustCH.setChecked(trust);
        
        et = (EditText)this.findViewById(R.id.editTextUser);
        et.setText(user);
        
        et = (EditText)this.findViewById(R.id.editTextPass);
        et.setText(pass);
        
        Spinner spinner = (Spinner)this.findViewById(R.id.SpinnerSort);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sorting_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        sort = sp.getInt(TagList.OWNCLOUD_SORTING, TagList.ALPHA_ASC);
        spinner.setSelection(sort);
	}

	// two for spinner
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    }
    public void onNothingSelected(AdapterView<?> parent) {
    }
    
	public void saveClick(View view) {
        SharedPreferences sp = getSharedPreferences(TagList.PREF_KEY, MODE_PRIVATE);      
        Editor editor = sp.edit();
        
        EditText et = (EditText)this.findViewById(R.id.editTextUrl);
        String newUrl = et.getText().toString();
		if (!newUrl.endsWith("/")) {
			newUrl += "/";
		}
		
		int i1 = newUrl.lastIndexOf(':');
		int i2 = newUrl.indexOf('/', i1);
		int httpsPort = 443;
		
		try {
			if (i1 != -1) {
				String port = newUrl.substring(i1 + 1, i2);
				httpsPort = Integer.parseInt(port);
				newUrl = newUrl.substring(0, i1) + newUrl.substring(i2);
				Toast.makeText(this, "HTTPS port retrieved: " + port, Toast.LENGTH_SHORT).show();				
			}
		} catch (Exception e) {
			// nothing to do, bad format, use 443
		}
		
		editor.putString(TagList.OWNCLOUD_ADDRESS, newUrl);
		editor.putInt(TagList.OWNCLOUD_HTTPSPORT, httpsPort);
		
		CheckBox ch = (CheckBox)this.findViewById(R.id.trustCheckBox);
		boolean newTrust = ch.isChecked();
		editor.putBoolean(TagList.OWNCLOUD_ACCEPTWRONGCERT, newTrust);
        
        et = (EditText)this.findViewById(R.id.editTextUser);
        String newUser = et.getText().toString();
        editor.putString(TagList.OWNCLOUD_USERNAME, newUser);
        
        et = (EditText)this.findViewById(R.id.editTextPass);
        String newPass = et.getText().toString();
        editor.putString(TagList.OWNCLOUD_PASSWORD, newPass);
        
        Spinner spinner = (Spinner)this.findViewById(R.id.SpinnerSort);
        int newSort = spinner.getSelectedItemPosition();
        editor.putInt(TagList.OWNCLOUD_SORTING, newSort);
        sort = newSort;
        
        editor.commit();
        
        if (!url.equals(newUrl) || trust != newTrust || !user.equals(newUser) || !pass.equals(newPass) || sort != newSort) {
        	TagList.forceRefresh = true;
        }

		finish();
	}

}
