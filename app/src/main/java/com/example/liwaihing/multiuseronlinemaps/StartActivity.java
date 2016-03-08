package com.example.liwaihing.multiuseronlinemaps;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.common.api.Status;
import java.io.InputStream;
import java.util.List;


public class StartActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks {
    private LocationManager locationManager;
    private SharedPreferences settings;
    private String username = "waihingli3";
    private GoogleApiClient googleApiClient;
    private GoogleApiAvailability googleApiAvailability;
    private SignInButton btn_SignIn;
    private static final int SIGN_IN_CODE = 0;
    private static final int PROFILE_PIC_SIZE = 120;
    private ConnectionResult connectionResult;
    private boolean isIntentInprogress;
    private boolean isSignInBtnClicked;
    private int requestCode;
    private ProgressDialog progress_dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Firebase.setAndroidContext(this);
        settings = getSharedPreferences("user_auth", MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setUpService();
        permissionCheck();
        setUpGPlusService();
    }

    private void setUpService(){
        if(!isMyServiceRunning(LocationService.class)) {
            startService(LocationService.class);
        }
        if(!isMyServiceRunning(SensorService.class)) {
            startService(SensorService.class);
        }
    }

    private void permissionCheck(){
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") DialogInterface dialog, @SuppressWarnings("unused") int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, @SuppressWarnings("unused") int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void setUpGPlusService(){
        googleApiClient =  new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        btn_SignIn = (SignInButton) findViewById(R.id.btn_signIn);
        btn_SignIn.setSize(SignInButton.SIZE_STANDARD);
        btn_SignIn.setScopes(new Scope[]{Plus.SCOPE_PLUS_LOGIN});
        btn_SignIn.setOnClickListener(this);
        findViewById(R.id.btn_signOut).setOnClickListener(this);
        findViewById(R.id.btn_proceed).setOnClickListener(this);
        progress_dialog = new ProgressDialog(this);
        progress_dialog.setMessage("Signing in....");
    }

    private void gPlusSignIn() {
        if (!googleApiClient.isConnecting()) {
            Log.d("user connected","connected");
            isSignInBtnClicked = true;
            progress_dialog.show();
            resolveSignInError();
        }
    }

    private void resolveSignInError() {
        if (connectionResult.hasResolution()) {
            try {
                isIntentInprogress = true;
                connectionResult.startResolutionForResult(this, SIGN_IN_CODE);
                Log.d("resolve error", "sign in error resolved");
            } catch (IntentSender.SendIntentException e) {
                isIntentInprogress = false;
                googleApiClient.connect();
            }
        }
    }

    private void gPlusSignOut() {
        if (googleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(googleApiClient);
            googleApiClient.disconnect();
            googleApiClient.connect();
            updateUI(false);
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.btn_signIn).setVisibility(View.GONE);
            findViewById(R.id.layout_signedIn).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.btn_signIn).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_signedIn).setVisibility(View.GONE);
        }
    }

    private void getProfileInfo() {
        try {
            if (Plus.PeopleApi.getCurrentPerson(googleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi.getCurrentPerson(googleApiClient);
                setPersonalInfo(currentPerson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPersonalInfo(Person currentPerson){
        String personName = currentPerson.getDisplayName();
        String personPhotoUrl = currentPerson.getImage().getUrl();
        String email = Plus.AccountApi.getAccountName(googleApiClient);
        TextView tv_username = (TextView) findViewById(R.id.tv_username);
        tv_username.setText("Name: "+personName);
        TextView tv_email = (TextView)findViewById(R.id.tv_email);
        tv_email.setText("Email: " + email);
        setProfilePic(personPhotoUrl);
        String[] googleID = email.split("@");
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("username", googleID[0]);
        editor.commit();
        progress_dialog.dismiss();
    }

    private void setProfilePic(String profile_pic){
        profile_pic = profile_pic.substring(0,
                profile_pic.length() - 2)
                + PROFILE_PIC_SIZE;
        ImageView user_picture = (ImageView)findViewById(R.id.img_proPic);
        new LoadProfilePic(user_picture).execute(profile_pic);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_signIn:
                gPlusSignIn();
                break;
            case R.id.btn_signOut:
                gPlusSignOut();
                break;
            case R.id.btn_proceed:
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    startActivity(MapsActivity.class);
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(LocationService.class);
        stopService(SensorService.class);
        super.onDestroy();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfo = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startActivity(Class c){
        Intent i = new Intent(this, c);
        startActivity(i);
    }

    private void startService(Class c){
        Intent i = new Intent(this, c);
        startService(i);
    }

    private void stopService(Class c){
        Intent i = new Intent(this, c);
        stopService(i);
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        googleApiClient.connect();
        updateUI(false);
    }

    @Override
    public void onConnected(Bundle arg0) {
        isSignInBtnClicked = false;
        getProfileInfo();
        updateUI(true);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            googleApiAvailability.getErrorDialog(this, result.getErrorCode(), requestCode).show();
            return;
        }
        if (!isIntentInprogress) {
            connectionResult = result;
            if (isSignInBtnClicked) {
                resolveSignInError();
            }
        }
    }

    private class LoadProfilePic extends AsyncTask<String, Void, Bitmap> {
        ImageView bitmap_img;

        public LoadProfilePic(ImageView bitmap_img) {
            this.bitmap_img = bitmap_img;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap new_icon = null;
            try {
                InputStream in_stream = new java.net.URL(url).openStream();
                new_icon = BitmapFactory.decodeStream(in_stream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new_icon;
        }

        protected void onPostExecute(Bitmap result_img) {
            bitmap_img.setImageBitmap(result_img);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
