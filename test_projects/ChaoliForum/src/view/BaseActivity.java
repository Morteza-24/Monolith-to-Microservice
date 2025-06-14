package com.daquexian.chaoli.forum.view;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;

/**
 * Created by jianhao on 16-5-31.
 * base class of all activity classes
 */
public abstract class BaseActivity extends AppCompatActivity implements IView {
    public void configToolbar(String title){
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.tl_custom);
    }
    public void configToolbar(int resId){
        configToolbar(getString(resId));
    }

    public void showToast(int strId){
        Toast.makeText(this, strId, Toast.LENGTH_SHORT).show();
    }

    public void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

}
