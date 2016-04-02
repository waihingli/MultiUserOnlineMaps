package com.example.liwaihing.multiuseronlinemaps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
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
import java.util.ArrayList;

public class ShareActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView lv_shareList;
    private ShareListAdapter listAdapter;

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
        setUpListener();
        lv_shareList = (ListView) findViewById(R.id.lv_sharelist);
        listAdapter = new ShareListAdapter(this, CommonUserList.getUserProfileList());
        lv_shareList.setAdapter(listAdapter);
        lv_shareList.setOnItemClickListener(onListItemClickListener);
        this.registerForContextMenu(lv_shareList);
    }

    private AdapterView.OnItemClickListener onListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String user = CommonUserList.getShareList().get(position);
            for(UserProfile u : CommonUserList.getUserProfileList()) {
                final UserProfile uClone = u;
                if (u.getUserProfile(user) != null) {
                    String msg = "Invite " + u.getDisplayName() + " to share location?";
                    AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);
                    builder.setMessage(msg)
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(@SuppressWarnings("unused") DialogInterface dialog, @SuppressWarnings("unused") int id) {
                                    Firebase ref = dbHelper.getUserInvitationPath(uClone.getGoogleID());
                                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.hasChild(dbHelper.getGoogleID())){
                                                if(dataSnapshot.child(dbHelper.getGoogleID()).getValue(String.class).equals("Pending")) {
                                                    Toast.makeText(ShareActivity.this, "Invitation has been made.", Toast.LENGTH_SHORT).show();
                                                }
                                            }else if(uClone.getIsSharing()){
                                                Toast.makeText(ShareActivity.this, "Already sharing location.", Toast.LENGTH_SHORT).show();
                                            }else{
                                                dbHelper.inviteUserSharing(uClone.getGoogleID());
                                            }
                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(FirebaseError firebaseError) {

                                        }
                                    });
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
        }
    };

    private void setUpListener(){
        final Firebase shareListRef = dbHelper.getUserShareListPath();
        shareListRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final String user = dataSnapshot.getValue(String.class);
                boolean exist = false;
                for (String name : CommonUserList.getShareList()) {
                    if (name.equals(user)) {
                        exist = true;
                    }
                }
                if (!exist) {
                    CommonUserList.addShareList(user);
                }
                boolean userExist = false;
                for (UserProfile u : CommonUserList.getUserProfileList()) {
                    if (u.getUserProfile(user) != null) {
                        userExist = true;
                    }
                }
                if (!userExist) {
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
                listAdapter.notifyDataSetChanged();
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
        String user = CommonUserList.getShareList().get(position);
        if (item.getItemId() == R.id.action_delete) {
            for (UserProfile u : CommonUserList.getUserProfileList()) {
                final UserProfile uClone = u;
                if (u.getUserProfile(user) != null) {
                    if (u.getIsSharing()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);
                        builder.setMessage("Delete " + uClone.getDisplayName() + "? Sharing location will be stopped. ")
                                .setCancelable(false)
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    public void onClick(@SuppressWarnings("unused") DialogInterface dialog, @SuppressWarnings("unused") int id) {
                                        onStopSharing(uClone);
                                        onDeleteShareList(uClone);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, @SuppressWarnings("unused") int id) {
                                        dialog.cancel();
                                    }
                                });
                        final AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        onDeleteShareList(uClone);
                    }
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    private void onStopSharing(UserProfile u){
        CommonUserList.removeUserSharingList(u.getGoogleID());
        CommonUserList.removeSharingProfileList(u);
        u.setIsSharing(false);
        listAdapter.notifyDataSetChanged();
        dbHelper.removeSharingUser(dbHelper.getGoogleID(), u.getGoogleID());
        dbHelper.removeSharingUser(u.getGoogleID(), dbHelper.getGoogleID());
    }

    private void onDeleteShareList(UserProfile u){
        boolean exist = false;
        for(String name : CommonUserList.getShareList()){
            if(name.equals(u.getGoogleID())){
                exist = true;
            }
        }
        if(exist){
            CommonUserList.removeShareList(u.getGoogleID());
        }
        CommonUserList.removeUserProfileList(u);
        listAdapter.notifyDataSetChanged();
        dbHelper.updateShareList(CommonUserList.getShareList());
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
                            if(!googleid.equals(dbHelper.getGoogleID())){
                                if (dataSnapshot.hasChild(googleid)) {
                                    boolean exist = false;
                                    for(String s : CommonUserList.getShareList()){
                                        if(s.equals(googleid)){
                                            exist = true;
                                        }
                                    }
                                    if(!exist){
                                        dbHelper.addShareList(googleid);
                                        dialog.dismiss();
                                    }else{
                                        tv_msg.setText("Already added user.");
                                    }
                                }else{
                                    tv_msg.setText("User does not exist.");
                                }
                            }else{
                                tv_msg.setText("Google ID is same as your account.");
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
        super.onDestroy();
    }

    public class ShareListAdapter extends BaseAdapter {
        ArrayList<UserProfile> data;
        LayoutInflater layoutInflater;

        class ViewHolder{
            ImageView userPic;
            TextView googleId;
            TextView status;
        }

        public ShareListAdapter(Context context, ArrayList<UserProfile> data){
            this.data = data;
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView==null){
                convertView=layoutInflater.inflate(R.layout.layout_sharelistitem, null);
                holder = new ViewHolder();
                holder.userPic = (ImageView) convertView.findViewById(R.id.img_profilePic);
                holder.googleId = (TextView) convertView.findViewById(R.id.tv_googleid);
                holder.status = (TextView) convertView.findViewById(R.id.tv_status);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }
            UserProfile userPro = data.get(position);
            holder.userPic.setImageBitmap(userPro.getProfilePic());
            holder.googleId.setText(userPro.getDisplayName());
            String status = "";
            if(data.get(position).getIsSharing()){
                status = "Sharing";
            }
            holder.status.setText(status);
            return convertView;
        }
    }

}
