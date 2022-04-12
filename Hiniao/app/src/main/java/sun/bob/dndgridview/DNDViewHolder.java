package sun.bob.dndgridview;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by bob.sun on 15/8/22.
 */
public class DNDViewHolder {
    public ImageView imageView;
    public TextView textView;
    public View view;

    int posistion;

    public DNDViewHolder(int posistion){
        this.posistion = posistion;
    }
//    Object data;
//    ImageView imageView;
//    TextView textView;
}
