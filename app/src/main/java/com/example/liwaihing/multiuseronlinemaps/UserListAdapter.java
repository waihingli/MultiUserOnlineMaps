package com.example.liwaihing.multiuseronlinemaps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by WaiHing on 8/3/2016.
 */
public class UserListAdapter extends BaseAdapter {
    ArrayList<UserProfile> data;
    LayoutInflater layoutInflater;

    static class ViewHolder{
        ImageView userPic;
        TextView googleId;
        TextView status;
    }

    public UserListAdapter(Context context, ArrayList<UserProfile> data){
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
            convertView=layoutInflater.inflate(R.layout.layout_listview, null);
            holder = new ViewHolder();
            holder.userPic = (ImageView) convertView.findViewById(R.id.img_profilePic);
            holder.googleId = (TextView) convertView.findViewById(R.id.tv_googleid);
            holder.status = (TextView) convertView.findViewById(R.id.tv_status);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        String user = CommonUserList.getShareList().get(position);
        UserProfile userPro = null;
        for(UserProfile u : data){
            if(u.getUserProfile(user)!=null){
                userPro = u;
            }
        }
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
