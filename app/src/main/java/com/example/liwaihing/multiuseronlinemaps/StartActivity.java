package com.example.liwaihing.multiuseronlinemaps;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

//        startService();
//        permissionCheck();
        startUpFinish();
    }

    private void startService(){
        startActivity(LocationService.class);
        startActivity(SensorService.class);
    }
    /*
        private void permissionCheck(){
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused")  DialogInterface dialog, @SuppressWarnings("unused") int id) {
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
            if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                onGPS = true;
            }
        }
    */
    private void startUpFinish(){
        startActivity(MapsActivity.class);
    }

    private void startActivity(Class c){
        Intent i = new Intent(this, c);
        startActivity(i);
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
