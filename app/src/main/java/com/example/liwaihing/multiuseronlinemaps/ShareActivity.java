package com.example.liwaihing.multiuseronlinemaps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ShareActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ArrayList<String> shareList = null;
    private ListView lv_shareList;
    private ArrayAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        dbHelper = DatabaseHelper.getInstance(this);
        ImageButton btn_back = (ImageButton) findViewById(R.id.btn_back);
        ImageButton btn_add = (ImageButton) findViewById(R.id.btn_add);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddShareList();
            }
        });
        shareList = dbHelper.getShareList();
        lv_shareList = (ListView) findViewById(R.id.lv_sharelist);
        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shareList);
        lv_shareList.setAdapter(listAdapter);
        this.registerForContextMenu(lv_shareList);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_share, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int id = item.getItemId();
        String name = shareList.get(id);
        switch (id){
            case R.id.action_share:
                return true;
            case R.id.action_delete:

                listAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void onAddShareList(){
        final Dialog dialog = new Dialog(ShareActivity.this);
        dialog.setTitle("Add New Share List");
        dialog.setContentView(R.layout.layout_addsharelist);
        final EditText et_googleid = (EditText) dialog.findViewById(R.id.et_googleid);
        Button btn_addshare = (Button) dialog.findViewById(R.id.btn_addshare);
        Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel);
        btn_addshare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String googleid = et_googleid.getText().toString();
                if(googleid!=null) {
                    shareList.add(googleid);
                    dbHelper.updateShareList(shareList);
                    updateShareList();
                    dialog.dismiss();
                }
            }
        });
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void updateShareList(){
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_share, menu);
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
