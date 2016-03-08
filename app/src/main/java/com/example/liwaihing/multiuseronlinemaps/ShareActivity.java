package com.example.liwaihing.multiuseronlinemaps;

import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;

public class ShareActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ArrayList<String> shareList;
    private ListView lv_shareList;
    private UserListAdapter listAdapter;

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
        listAdapter = new UserListAdapter(this, CommonUserList.getUserProfileList());
        lv_shareList.setAdapter(listAdapter);
        this.registerForContextMenu(lv_shareList);
    }

    private void setUpListener(){
        Firebase shareListRef = dbHelper.getUserShareListPath();
        shareListRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String user = dataSnapshot.getValue(String.class);
                shareList.add(user);
                boolean userExist = false;
                for (UserProfile u : CommonUserList.getUserProfileList()) {
                    if (u.getUserProfile(user) != null) {
                        userExist = true;
                    }
                }
                if(!userExist){
                    Firebase ref = dbHelper.getUserProfilePath(user);
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot d) {
                            UserProfile userPro = new UserProfile(user);
                            userPro.setDisplayName(d.child("Name").getValue(String.class));
                            userPro.setProfilePic(d.child("Picture").getValue(String.class));
                            CommonUserList.addUserProfileList(userPro);
                            listAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });
                }
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
        MenuItem share = menu.findItem(R.id.action_share);
        MenuItem delete = menu.findItem(R.id.action_delete);
        MenuItem stopShare = menu.findItem(R.id.action_stopShare);
        for(UserProfile u : CommonUserList.getUserProfileList()){
            if(u.getIsSharing()){
                share.setVisible(false);
                delete.setVisible(false);
                stopShare.setVisible(true);
            }else{
                share.setVisible(true);
                delete.setVisible(true);
                stopShare.setVisible(false);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        String user = shareList.get(position);
        switch (item.getItemId()){
            case R.id.action_share:
                for(UserProfile u : CommonUserList.getUserProfileList()){
                    if(u.getUserProfile(user)!=null){
                        u.setIsSharing(true);
                    }
                }
                dbHelper.addSharingUser(user);
                finish();
                break;
            case R.id.action_delete:
                shareList.remove(position);
                UserProfile userPro = null;
                for(UserProfile u : CommonUserList.getUserProfileList()){
                    if(u.getUserProfile(user)!=null){
                        userPro = u;
                    }
                }
//                CommonUserList.removeUserProfileList(userPro);
                dbHelper.updateShareList(shareList);
                listAdapter.notifyDataSetChanged();
                break;
            case R.id.action_stopShare:
//                CommonUserList.removeUserSharingList(user);
//                for(UserProfile u : CommonUserList.getUserProfileList()){
//                    if(u.getUserProfile(user)!=null){
//                        u.setIsSharing(false);
//                    }
//                }
//                dbHelper.updateSharing(CommonUserList.getUserSharingList());
//                listAdapter.notifyDataSetChanged();
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
                                boolean exist = false;
                                for(int i = 0; i<shareList.size(); i++){
                                    if(shareList.get(i).equals(googleid)){
                                        exist = true;
                                    }
                                }
                                if(!exist){
                                    shareList.add(googleid);
                                    dbHelper.updateShareList(shareList);
                                    dialog.dismiss();
                                }else{
                                    tv_msg.setText("Already added user.");
                                }
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
    protected void onDestroy() {
        stopService(LocationService.class);
        stopService(SensorService.class);
        super.onDestroy();
    }

    private void stopService(Class c){
        Intent i = new Intent(this, c);
        stopService(i);
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
