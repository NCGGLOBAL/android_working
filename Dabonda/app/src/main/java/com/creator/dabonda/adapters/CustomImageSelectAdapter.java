package com.creator.dabonda.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.creator.dabonda.R;
import com.creator.dabonda.models.Image;

import java.util.ArrayList;

/**
 * Created by Darshan on 4/18/2015.
 */
public class CustomImageSelectAdapter extends CustomGenericAdapter<Image> {
    private boolean mIsSelectedCheck = true;

    public CustomImageSelectAdapter(Context context, ArrayList<Image> images) {
        super(context, images);
    }

    public CustomImageSelectAdapter(Context context, ArrayList<Image> images, boolean isSelectedCheck) {
        super(context, images);

        mIsSelectedCheck = isSelectedCheck;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_view_item_image_select, null);

            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_view_image_select);
            viewHolder.textView = (TextView) convertView.findViewById(R.id.image_view_image_sequence);
            viewHolder.view = convertView.findViewById(R.id.view_alpha);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.imageView.getLayoutParams().width = size;
        viewHolder.imageView.getLayoutParams().height = size;

        viewHolder.view.getLayoutParams().width = size;
        viewHolder.view.getLayoutParams().height = size;

        if (mIsSelectedCheck && arrayList.get(position).isSelected) {
            viewHolder.textView.setVisibility(View.VISIBLE);
            viewHolder.textView.setText(arrayList.get(position).sequence + "");

            viewHolder.view.setAlpha(0.5f);
            ((FrameLayout) convertView).setForeground(context.getResources().getDrawable(R.drawable.ic_done_white));
        } else {
            viewHolder.textView.setVisibility(View.INVISIBLE);

            viewHolder.view.setAlpha(0.0f);
            ((FrameLayout) convertView).setForeground(null);
        }

        Glide.with(context)
                .load(arrayList.get(position).path)
                .placeholder(R.drawable.image_placeholder).into(viewHolder.imageView);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public View view;
    }
}
