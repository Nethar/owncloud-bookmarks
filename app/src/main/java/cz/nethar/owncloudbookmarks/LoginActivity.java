package cz.nethar.owncloudbookmarks;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import static cz.nethar.owncloudbookmarks.TagList.ACCOUNT_TYPE;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * The default email to populate the email field with.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        
        String text = "";

        Log.d("asdfasdf", "alsdfkjasldkfjaslkfjaskfj");



        // AccountManager accountManager = AccountManager.get(getApplicationContext());
        // Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        /*
        if (accounts.length == 0) {
        	
        }
        
        new Thread(new Runnable() {
            public void run() {
            }
        }).start();
        
        for (Account a : accounts) {
        	text += a.name + " " + a.type + " " + a.toString() + "\n";
        	try {
        		String token = accountManager.blockingGetAuthToken(a, ACCOUNT_TYPE, false);       	
        		text += token;
        		//text += bundle.getResult().getString(AccountManager.KEY_AUTHTOKEN);	
        	} catch (Exception e) {
        		text += e.getMessage();
        	}
            //text += accountManager.getUserData(a, "oc_base_url");           
        }       
        TextView tv = (TextView)this.findViewById(R.id.textView1);
        tv.setText(text);*/      
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);			
		    return true;
		}
		return true;
	}

}
