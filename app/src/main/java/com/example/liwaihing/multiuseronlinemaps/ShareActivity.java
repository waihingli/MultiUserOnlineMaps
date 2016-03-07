package com.example.liwaihing.multiuseronlinemaps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class ShareActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ArrayList<String> shareList;
    private ListView lv_shareList;
    private ArrayAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

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
        dbHelper = new DatabaseHelper(this);
        shareList = new ArrayList<>();
        setUpListener();
        lv_shareList = (ListView) findViewById(R.id.lv_sharelist);
        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shareList);
        lv_shareList.setAdapter(listAdapter);
        this.registerForContextMenu(lv_shareList);
    }

    private void setUpListener(){
        Firebase shareListRef = dbHelper.getUserShareListPath();
        shareListRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String user = dataSnapshot.getValue(String.class);
                shareList.add(user);
                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
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
        int position = info.position;
        String user = shareList.get(position);
        switch (item.getItemId()){
            case R.id.action_share:
                dbHelper.updateSharing(user);
                finish();
                break;
            case R.id.action_delete:
                shareList.remove(position);
                dbHelper.updateShareList(shareList);
                listAdapter.notifyDataSetChanged();
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void onAddShareList(){
        final Dialog dialog = new Dialog(ShareActivity.this);
        dialog.setTitle("Add New Share List");
        dialog.setContentView(R.layout.layout_addsharelist);
        final EditText et_googleid = (EditText) dialog.findViewById(R.id.et_googleid);
        final TextView tv_msg = (TextView) dialog.findViewById(R.id.tv_message);
        Button btn_addshare = (Button) dialog.findViewById(R.id.btn_addshare);
        Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel);
        btn_addshare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_msg.setText("");
                final String googleid = et_googleid.getText().toString();
                if(!googleid.isEmpty()) {
                    Firebase ref = dbHelper.getUserPath();
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(googleid)) {
                                shareList.add(googleid);
                                dbHelper.updateShareList(shareList);
                                listAdapter.notifyDataSetChanged();
                                dialog.dismiss();
                            }else{
                                tv_msg.setText("User does not exist.");
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });
                }else{
                    tv_msg.setText("Please enter the email address.");
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
