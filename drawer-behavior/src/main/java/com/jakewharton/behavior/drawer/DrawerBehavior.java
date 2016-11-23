/*
 * Copyright 2016 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jakewharton.behavior.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.Keep;
import android.support.annotation.RestrictTo;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

public final class DrawerBehavior extends CoordinatorLayout.Behavior<View> {

  /** @hide */
  @SuppressWarnings("WeakerAccess")
  @RestrictTo(GROUP_ID)
  @IntDef({ViewDragHelper.STATE_IDLE, ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING})
  @Retention(RetentionPolicy.SOURCE)
  public @interface State {}

  /**
   * Listener for monitoring events about drawers.
   */
  public interface DrawerListener {
    /**
     * Called when a drawer's position changes.
     * @param drawerView The child view that was moved
     * @param slideOffset The new offset of this drawer within its range, from 0-1
     */
    void onDrawerSlide(View drawerView, float slideOffset);

    /**
     * Called when a drawer has settled in a completely open state.
     * The drawer is interactive at this point.
     *
     * @param drawerView Drawer view that is now open
     */
    void onDrawerOpened(View drawerView);

    /**
     * Called when a drawer has settled in a completely closed state.
     *
     * @param drawerView Drawer view that is now closed
     */
    void onDrawerClosed(View drawerView);

    /**
     * Called when the drawer motion state changes. The new state will
     *
     * be one of {@link ViewDragHelper#STATE_IDLE}, {@link ViewDragHelper#STATE_DRAGGING} or {@link ViewDragHelper#STATE_SETTLING}.
     * @param drawerView Drawer view which motion state is changed
     * @param newState The new drawer motion state
     */
    void onDrawerStateChanged(View drawerView,@State int newState);
  }

  private static void validateGravity(int gravity) {
    if (gravity != Gravity.LEFT
        && gravity != Gravity.RIGHT
        && gravity != GravityCompat.START
        && gravity != GravityCompat.END) {
      throw new IllegalArgumentException("Only START, END, LEFT, or RIGHT gravity is supported.");
    }
  }

  private final SimpleArrayMap<View, BehaviorDelegate> delegates = new SimpleArrayMap<>();

  private final int gravity;

  private DrawerListener listener;

  @SuppressWarnings("unused") // Public API for programmatic instantiation.
  public DrawerBehavior(int gravity) {
    validateGravity(gravity);
    this.gravity = gravity;
  }

  @Keep @SuppressWarnings("unused") // Instantiated reflectively from layout XML.
  public DrawerBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DrawerBehavior);
    int gravity =
        a.getInteger(R.styleable.DrawerBehavior_android_layout_gravity, GravityCompat.END);
    a.recycle();

    validateGravity(gravity);
    this.gravity = gravity;
  }

  public void setDrawerListener(DrawerListener listener) {
    this.listener = listener;
  }

  private BehaviorDelegate delegate(CoordinatorLayout parent, View child) {
    BehaviorDelegate delegate = delegates.get(child);
    if (delegate == null) {
      delegate = new BehaviorDelegate(parent, child, gravity);
      delegate.setDrawListener(listener);
      delegates.put(child, delegate);
    }
    return delegate;
  }

  @Override
  public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
    return child.getVisibility() == View.GONE //
        || delegate(parent, child).onLayoutChild();
  }

  @Override
  public boolean onInterceptTouchEvent(CoordinatorLayout parent, View child, MotionEvent ev) {
    return delegate(parent, child).onInterceptTouchEvent(ev);
  }

  @Override public boolean onTouchEvent(CoordinatorLayout parent, View child, MotionEvent ev) {
    return delegate(parent, child).onTouchEvent(ev);
  }

  @Override
  public Parcelable onSaveInstanceState(CoordinatorLayout parent, View child) {
    return new SavedState(super.onSaveInstanceState(parent, child),delegates.get(child).isDrawerOpen()) ;
  }

  @Override
  public void onRestoreInstanceState(CoordinatorLayout parent, View child, Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(parent, child, ss.getSuperState());
    delegates.get(child).setDrawerState(ss.openState?BehaviorDelegate.FLAG_IS_OPENED:BehaviorDelegate.FLAG_IS_CLOSED);
  }

  private static class SavedState extends AbsSavedState {

    boolean openState;

    private SavedState(Parcelable superState,boolean openState) {
      super(superState);
      this.openState = openState;
    }

    @SuppressWarnings("unused")
    protected SavedState(Parcel source) {
      this(source,null);
    }

    private SavedState(Parcel source, ClassLoader loader) {
      super(source, loader);
      openState = source.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeInt(openState?1:0);
    }

    public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
            new ParcelableCompatCreatorCallbacks<SavedState>() {
              @Override
              public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in,loader);
              }

              @Override
              public SavedState[] newArray(int size) {
                return new SavedState[size];
              }
            }
    );
  }


  public static DrawerBehavior from(View view) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
    if (!(layoutParams instanceof CoordinatorLayout.LayoutParams)) {
      throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
    }
    CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) layoutParams;
    CoordinatorLayout.Behavior behavior = lp.getBehavior();
    if (!(behavior instanceof DrawerBehavior)) {
      throw new IllegalArgumentException(
              "The view is not associated with DrawerBehavior");
    }
    return (DrawerBehavior) behavior;
  }

}
