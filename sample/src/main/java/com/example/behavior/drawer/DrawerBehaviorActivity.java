package com.example.behavior.drawer;

import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.jakewharton.behavior.drawer.DrawerBehavior;

public final class DrawerBehaviorActivity extends AppCompatActivity {
  private static final String TAG = DrawerBehaviorActivity.class.getCanonicalName();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.drawer_behavior);
    View view = findViewById(R.id.view);
    DrawerBehavior behavior = DrawerBehavior.from(view);
    behavior.setDrawerListener(new DrawerBehavior.DrawerListener() {
      @Override
      public void onDrawerSlide(View drawerView, float slideOffset) {

      }

      @Override
      public void onDrawerOpened(View drawerView) {
        Log.d(TAG,"opened:"+ drawerView.toString());
      }

      @Override
      public void onDrawerClosed(View drawerView) {
        Log.d(TAG,"closed:"+ drawerView.toString());
      }

      @Override
      public void onDrawerStateChanged(int newState) {
          Log.d(TAG,"onDrawerStateChanged:"+ newState);
      }
    });
  }
}
