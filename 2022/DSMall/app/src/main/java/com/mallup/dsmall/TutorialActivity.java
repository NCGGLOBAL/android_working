package com.mallup.dsmall;

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

import com.mallup.dsmall.common.CODE;
import com.mallup.dsmall.delegator.HNSharedPreference;

public class TutorialActivity extends Activity {
    int currentSelectedPosition = 0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        HNSharedPreference.putSharedPreference(TutorialActivity.this, CODE.PREF_TUTORIAL, "1");
        initTutorial();
    }

    private void initTutorial() {
        ViewPager pager = findViewById(R.id.viewPager);
        final CustomViewPagerAdapter pagerAdapter = new CustomViewPagerAdapter(this);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float v, int i1) {
                Log.e("jj", "onPageScrolled position : " + position);
            }

            @Override
            public void onPageSelected(int position) {
                currentSelectedPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int position) {
                if (currentSelectedPosition == pagerAdapter.MAX_PAGE_COUNT - 1 &&
                        position == pagerAdapter.MAX_PAGE_COUNT - 1) {
                    finish();
                }
            }
        });
    }
}

class CustomViewPagerAdapter extends PagerAdapter {

    final int MAX_PAGE_COUNT = 2;

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

            switch (position) {
                case 0 :
                    view.setBackgroundResource(R.drawable.bg_swipe1);
                    break;
                case 1 :
                    view.setBackgroundResource(R.drawable.bg_swipe2);
                    break;
            }

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
        return MAX_PAGE_COUNT;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (view == (View)object);
    }
}