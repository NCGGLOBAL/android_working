package com.mallup.bizwelstore;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mallup.bizwelstore.common.CODE;
import com.mallup.bizwelstore.delegator.HNSharedPreference;

public class TutorialActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        Log.e("jj", "TutorialActivity");

        HNSharedPreference.putSharedPreference(TutorialActivity.this, CODE.PREF_TUTORIAL, "1");
        initTutorial();
    }

    private void initTutorial() {
        ViewPager pager = findViewById(R.id.viewPager);
        CustomViewPagerAdapter pagerAdapter = new CustomViewPagerAdapter(this);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }
}

class CustomViewPagerAdapter extends PagerAdapter {

    // LayoutInflater 서비스 사용을 위한 Context 참조 저장.
    private Context mContext = null ;

    public CustomViewPagerAdapter() {

    }

    // Context를 전달받아 mContext에 저장하는 생성자 추가.
    public CustomViewPagerAdapter(Context context) {
        mContext = context ;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = null ;

        if (mContext != null) {
            // LayoutInflater를 통해 "/res/layout/page.xml"을 뷰로 생성.
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_viewpager, container, false);

            ImageView imageView = (ImageView) view.findViewById(R.id.tutorialImageView) ;
            imageView.setImageResource(R.drawable.btn_register_camera);
        }

        // 뷰페이저에 추가.
        container.addView(view) ;

        return view ;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // 뷰페이저에서 삭제.
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        // 전체 페이지 수는 10개로 고정.
        return 3;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (view == (View)object);
    }
}