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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class StartActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks {
    private LocationManager locationManager;
    private SharedPreferences settings;
    private GoogleApiClient googleApiClient;
    private GoogleApiAvailability googleApiAvailability;
    private SignInButton btn_SignIn;
    private ImageView img_appIcon;
    private static final int SIGN_IN_CODE = 0;
    private static final int PROFILE_PIC_SIZE = 120;
    private ConnectionResult connectionResult;
    private boolean isIntentInProgress;
    private boolean isSignInBtnClicked;
    private int requestCode;
    private ProgressDialog progress_dialog;
    private LinearLayout connection_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        settings = getSharedPreferences("user_auth", MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setUpGoogleApiClient();
        progress_dialog = new ProgressDialog(this);
        btn_SignIn = (SignInButton) findViewById(R.id.btn_signIn);
        btn_SignIn.setSize(SignInButton.SIZE_STANDARD);
        btn_SignIn.setScopes(new Scope[]{Plus.SCOPE_PLUS_LOGIN});
        btn_SignIn.setOnClickListener(this);
        img_appIcon = (ImageView) findViewById(R.id.img_appIcon);
        if (!settings.getString("googleID", "").isEmpty()){
            findViewById(R.id.btn_signIn).setVisibility(View.GONE);
        }
        progress_dialog.setMessage("Signing in....");
    }

    private void setUpService(){
        if(!isMyServiceRunning(LocationService.class)) {
            startService(LocationService.class);
        }
        if (!isMyServiceRunning(SensorService.class)) {
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

    private void connectionCheck(){
        img_appIcon.setVisibility(View.VISIBLE);
        connection_layout = (LinearLayout) findViewById(R.id.layout_connection);
        connection_layout.setVisibility(View.GONE);
        btn_SignIn.setVisibility(View.GONE);
        new NetworkConnection().execute();
    }

    public void onRetry(View v){
        connectionCheck();
    }

    private void setUpGPlusLayout(){
        btn_SignIn.setVisibility(View.VISIBLE);
    }

    private void setUpGoogleApiClient(){
        googleApiClient =  new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
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
                isIntentInProgress = true;
                connectionResult.startResolutionForResult(this, SIGN_IN_CODE);
                Log.d("resolve error", "sign in error resolved");
            } catch (IntentSender.SendIntentException e) {
                isIntentInProgress = false;
                googleApiClient.connect();
            }
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            btn_SignIn.setVisibility(View.GONE);
            img_appIcon.setVisibility(View.VISIBLE);
        } else {
            btn_SignIn.setVisibility(View.VISIBLE);
            img_appIcon.setVisibility(View.VISIBLE);
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
        setProfilePic(personPhotoUrl);
        String[] googleID = email.split("@");
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("name", personName);           //save user info in local
        editor.putString("googleID", googleID[0]);
        editor.commit();
        progress_dialog.dismiss();
    }

    private void setProfilePic(String profilePic){
        profilePic = profilePic.substring(0,
                profilePic.length() - 2)
                + PROFILE_PIC_SIZE;
        new LoadProfilePic().execute(profilePic);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_signIn:
                gPlusSignIn();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
        connectionCheck();
    }

    private void onStartMaps(){
        permissionCheck();
        if (!settings.getBoolean("signedIn", false)){
            setUpGPlusLayout();
        }else{
            startFinish();
        }
    }

    private void startFinish(){
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            setUpService();
            Intent i = new Intent(this, MapsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            if (googleApiClient.isConnected()) {
                googleApiClient.disconnect();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    private void startService(Class c){
        Intent i = new Intent(this, c);
        startService(i);
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        googleApiClient.connect();
        updateUI(false);
    }

    @Override
    public void onConnected(Bundle arg0) {
        progress_dialog.dismiss();
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
        if (!isIntentInProgress) {
            connectionResult = result;
            if (isSignInBtnClicked) {
                resolveSignInError();
            }
        }
    }

    @Override
    protected void onActivityResult(int mRequestCode, int mResponseCode, Intent intent) {
        if (mRequestCode == SIGN_IN_CODE) {
            requestCode = mRequestCode;
            if (mResponseCode != RESULT_OK) {
                isSignInBtnClicked = false;
                progress_dialog.dismiss();
            }
            isIntentInProgress = false;
            if (!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        }
    }

    private class NetworkConnection extends AsyncTask<String, Void, Boolean>{
        protected Boolean doInBackground(String... params) {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                try {
                    URL url = new URL("http://www.google.com/");
                    HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
                    urlc.setRequestProperty("User-Agent", "test");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(1000);
                    urlc.connect();
                    if (urlc.getResponseCode() == 200) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return false;
        }

        protected void onPostExecute(Boolean isConnected) {
            if(!isConnected){
                img_appIcon.setVisibility(View.GONE);
                connection_layout.setVisibility(View.VISIBLE);
            }else{
                connection_layout.setVisibility(View.GONE);
                img_appIcon.setVisibility(View.VISIBLE);
                onStartMaps();
                googleApiClient.connect();
            }
        }
    }

    private class LoadProfilePic extends AsyncTask<String, Void, Bitmap> {
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

        protected void onPostExecute(Bitmap resultImg) {
            ByteArrayOutputStream baos=new  ByteArrayOutputStream();
            resultImg.compress(Bitmap.CompressFormat.PNG,100, baos);
            byte[] b=baos.toByteArray();
            if(!settings.getBoolean("signedIn", false)){            //first time to sign in
                String temp=Base64.encodeToString(b, Base64.DEFAULT);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("profilePic", temp);
                editor.putBoolean("signedIn", true);
                editor.commit();
                updateUI(true);
                startFinish();
            }
        }
    }
}
