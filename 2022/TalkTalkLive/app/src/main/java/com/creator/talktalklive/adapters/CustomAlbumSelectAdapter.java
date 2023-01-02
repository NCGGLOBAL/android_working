package com.creator.talktalklive.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.creator.talktalklive.R;
import com.creator.talktalklive.models.Album;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Darshan on 4/14/2015.
 */
public class CustomAlbumSelectAdapter extends CustomGenericAdapter<Album> {
    private HashMap<String, Long> count = new HashMap<String, Long>();

    public CustomAlbumSelectAdapter(Context context, ArrayList<Album> albums, HashMap<String, Long> albumCount) {
        super(context, albums);

        this.count = albumCount;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_view_item_album_select, null);

            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_view_album_image);
            viewHolder.textViewName = (TextView) convertView.findViewById(R.id.text_view_album_name);
            viewHolder.textViewCount = (TextView) convertView.findViewById(R.id.text_view_album_count);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

//        viewHolder.imageView.getLayoutParams().width = size;
//        viewHolder.imageView.getLayoutParams().height = size;
        viewHolder.textViewName.setText(arrayList.get(position).name);
        viewHolder.textViewCount.setText(count.get(arrayList.get(position).name) + "");
        Glide.with(context)
                .load(arrayList.get(position).cover)
                .placeholder(R.drawable.image_placeholder).centerCrop().into(viewHolder.imageView);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView imageView;
        public TextView textViewName;
        public TextView textViewCount;
    }
}