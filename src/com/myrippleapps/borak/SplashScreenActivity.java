/*
 * The MIT License (MIT)
 * 
 * Copyright � 2013 Clover Studio Ltd. All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.myrippleapps.borak;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.myrippleapps.borak.R;
import com.myrippleapps.borak.couchdb.CouchDB;
import com.myrippleapps.borak.couchdb.ResultListener;
import com.myrippleapps.borak.couchdb.model.User;
import com.myrippleapps.borak.extendables.SideBarActivity;
import com.myrippleapps.borak.management.UsersManagement;
import com.myrippleapps.borak.utils.Const;
import com.myrippleapps.borak.utils.Preferences;
//import com.winsontan520.wversionmanager.library.WVersionManager;
import com.crittercism.app.Crittercism;

/**
 * SplashScreenActivity
 * 
 * Displays splash screen for 2 seconds.
 */

public class SplashScreenActivity extends Activity {

	private String mSavedEmail;
	private String mSavedPassword;
	public static SplashScreenActivity sInstance = null;
	private User mUser;
	public static String mUpdateUrl = "http://chat.myrippleapps.me/assets/version.json";
	public static String mApkUrl = "http://chat.myrippleapps.me/assets/milytalk.apk";
//	public static final int mRemindMe = 1;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	    {
		super.onCreate(savedInstanceState);

		SplashScreenActivity.sInstance = this;
		setContentView(R.layout.activity_splash_screen);

		/* Initiate Crittercism */
		Crittercism.init(getApplicationContext(), Const.CRITTERCISM_APP_ID);

		new CouchDB();
		// new UsersManagement();

		if ( SpikaApp.hasNetworkConnection() )
		    {

			if ( checkIfUserSignIn() )
			    {
				mSavedEmail = SpikaApp.getPreferences().getUserEmail();
				mSavedPassword = SpikaApp.getPreferences().getUserPassword();

				mUser = new User();

				CouchDB.authAsync(mSavedEmail, mSavedPassword, new AuthListener(), SplashScreenActivity.this, false);
			    }else
			    {
				new Handler().postDelayed(new Runnable() {

					    @Override
					    public void run()
						{
						    startActivity(new Intent(SplashScreenActivity.this, SignInActivity.class));
//						checkVersion();
						    finish();
						}
					}, 2000);
			    }
		    }else
		    {
			new Handler().postDelayed(new Runnable() {
				    @Override
				    public void run()
					{
					    Intent intent = new Intent(SplashScreenActivity.this, RecentActivityActivity.class);
					    intent.putExtra(Const.SIGN_IN, true);
					    SplashScreenActivity.this.startActivity(intent);
					    Toast.makeText(SplashScreenActivity.this, getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();
					}
				}, 2000);
		    }
	    }

//	public void checkVersion() {
//                WVersionManager versionManager = new WVersionManager(this);
//		versionManager.setVersionContentUrl(mUpdateUrl);
//		versionManager.setUpdateUrl(mApkUrl);
//		versionManager.setReminderTimer(mRemindMe);
//		versionManager.setTitle("New BETA Available");
//		versionManager.checkVersion();
//	}

	private boolean checkIfUserSignIn()
	    {
		boolean isSessionSaved = false;
		Preferences prefs = SpikaApp.getPreferences();
		if ( prefs.getUserEmail() == null && prefs.getUserPassword() == null )
		    {
			isSessionSaved = false;
		    }else if ( prefs.getUserEmail().equals("") && prefs.getUserPassword().equals("") )
		    {
			isSessionSaved = false;
		    }else
		    {
			isSessionSaved = true;
		    }
		return isSessionSaved;
	    }

	private void signIn(User u)
	    {

		UsersManagement.setLoginUser(u);
		UsersManagement.setToUser(u);
		UsersManagement.setToGroup(null);

		boolean openPushNotification = getIntent().getBooleanExtra(Const.PUSH_INTENT, false);

		Intent intent = new Intent(SplashScreenActivity.this, RecentActivityActivity.class);
		if ( openPushNotification )
		    {
			intent = getIntent();
			intent.setClass(SplashScreenActivity.this, RecentActivityActivity.class);
		    }

		//parse URI hookup://user/[ime korisnika] and hookup://group/[ime grupe]
		Uri userUri = getIntent().getData();
		//If opened from link
		if ( userUri != null )
		    {
        		String scheme = userUri.getScheme(); 		// "hookup"
        		String host = userUri.getHost(); 		// "user" or "group"
        		if ( host.equals("user") )
			    {
                		List<String> params = userUri.getPathSegments();
                		String userName = params.get(0); 	// "ime korisnika"
        		    	intent.putExtra(Const.USER_URI_INTENT, true);
        		    	intent.putExtra(Const.USER_URI_NAME, userName);
			    }else if ( host.equals("group") )
			    {
                		List<String> params = userUri.getPathSegments();
                		String groupName = params.get(0); 	// "ime grupe"
        		    	intent.putExtra(Const.GROUP_URI_INTENT, true);
        		    	intent.putExtra(Const.GROUP_URI_NAME, groupName);
			    }
		    }

		intent.putExtra(Const.SIGN_IN, true);
		SplashScreenActivity.this.startActivity(intent);
//		checkVersion();
		finish();
	    }

	private void checkPassProtect(User user)
	    {

		if ( SpikaApp.getPreferences().getPasscodeProtect() ) 
		    {
			Intent passcode = new Intent(SplashScreenActivity.this, PasscodeActivity.class);
			passcode.putExtra("protect", true);
			SplashScreenActivity.this.startActivityForResult(passcode, 0);
//			checkVersion();
		    }else
		    {
			signIn(user);
		    }
	    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	    {
		if ( resultCode == Activity.RESULT_OK )
		    {
			signIn(mUser);
		    }else
		    {
			finish();
		    }
		super.onActivityResult(requestCode, resultCode, data);
	    }

	private boolean authentificationOk(User user)
	    {

		boolean authentificationOk = false;

		if ( user.getEmail() != null && !user.getEmail().equals("") )
		    {
			if ( user.getEmail().equals(mSavedEmail) )
			    {
				authentificationOk = true;
			    }
		    }
		return authentificationOk;
	    }

	private class AuthListener implements ResultListener<String>
	    {
		@Override
		public void onResultsSucceded(String result)
		    {
			boolean tokenOk = result.equals(Const.LOGIN_SUCCESS);
			mUser = UsersManagement.getLoginUser();
			if ( tokenOk && mUser != null )
			    {
				if ( authentificationOk(mUser) )
				    {
					new Handler().postDelayed(new Runnable() {

						    @Override
						    public void run()
							{
							    checkPassProtect(mUser);
							}
						}, 2000);
				    }
			    }else
			    {
				SideBarActivity.appLogout(false, false, true);
			    }
		    }
		@Override
		public void onResultsFail()
		    {
			SideBarActivity.appLogout(false, true, false);
		    }
	    }
    }
