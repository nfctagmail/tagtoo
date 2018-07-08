package com.tagtoo.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.Checkable;

public class CheckableCardView extends CardView implements Checkable{

    private boolean isChecked = false;
    private boolean broadcasting;

    private OnCheckedChangeListener onCheckedChangeListener;

    private static final int[] CHECKED_STATE_SET = {
            R.attr.is_checked,
    };

    public CheckableCardView(Context context) {
        this(context, null);
    }

    public CheckableCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setClickable(true);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CheckableCardView);

        boolean checked = a.getBoolean(R.styleable.CheckableCardView_is_checked, false);
        setChecked(checked);

        a.recycle();
    }

    @Override
    public void toggle() {
        setChecked(!this.isChecked);
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if(isChecked != checked) {
            this.isChecked = checked;
            refreshDrawableState();

            if (broadcasting) {
                return;
            }

            broadcasting = true;
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, this.isChecked);
            }

            broadcasting = false;
        }
    }


    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked())
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    static class SavedState extends BaseSavedState {
        boolean checked;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            checked = (Boolean) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(checked);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.checked = isChecked();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        setChecked(ss.checked);
        requestLayout();
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes.
     *
     * @param listener
     *            the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        onCheckedChangeListener = listener;
    }

    public interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a button has changed.
         *
         * @param cardView
         *            The button view whose state has changed.
         * @param isChecked
         *            The new checked state of button.
         */
        void onCheckedChanged(CheckableCardView cardView, boolean isChecked);
    }

}
