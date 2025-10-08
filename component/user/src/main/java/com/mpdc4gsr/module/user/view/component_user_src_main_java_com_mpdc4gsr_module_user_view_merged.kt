// Merged ALL .kt and .java files from the 'component\user\src\main\java\com\mpdc4gsr\module\user\view' directory and its subdirectories.
// Total files: 3 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\view\DragCustomerView.java =====

package com.mpdc4gsr.module.user.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mpdc4gsr.libunified.app.utils.ScreenUtils;

public class DragCustomerView extends androidx.appcompat.widget.AppCompatImageView {
    float mDownX;
    float mDownY;
    private int mWidth;
    private int mHeight;
    private int mScreenWidth;
    private int mScreenHeight;
    private Context mContext;
    private boolean isDrag = false;

    public DragCustomerView (Context context) {
        super(context);
        this.mContext = context;
    }

    public DragCustomerView (Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public DragCustomerView (Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!isInEditMode()) {
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();
            mScreenWidth = ScreenUtils.getScreenWidth(getContext());
            int statusBarHeight = getStatusBarHeight ();
            int navBarHeight = getNavigationBarHeight ();
            int offsetDp =(int)(62f * getContext().getResources().getDisplayMetrics().density);
            mScreenHeight = ScreenUtils.getScreenHeight(getContext()) - statusBarHeight - navBarHeight - offsetDp;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction() & MotionEvent . ACTION_MASK) {
        case MotionEvent . ACTION_DOWN :
        isDrag = false;
        mDownX = event.getX();
        mDownY = event.getY();
        setPressed(true);
        break;
        case MotionEvent . ACTION_MOVE :
        float mXDistance = event . getX () - mDownX;
        float mYDistance = event . getY () - mDownY;
        int left, right, top, bottom;
        if (Math.abs(mXDistance) > 10 || Math.abs(mYDistance) > 10 && !isDrag) {
            isDrag = true;
            left = (int)(getLeft() + mXDistance);
            right = left + mWidth;
            top = (int)(getTop() + mYDistance);
            bottom = top + mHeight;
            if (left < 0) {
                left = 0;
                right = left + mWidth;
            } else if (right > mScreenWidth) {
                right = mScreenWidth;
                left = right - mWidth;
            }
            if (top < 0) {
                top = 0;
                bottom = top + mHeight;
            } else if (bottom > mScreenHeight) {
                bottom = mScreenHeight;
                top = bottom - mHeight;
            }
            this.layout(left, top, right, bottom);
        }
        break;
        case MotionEvent . ACTION_UP :
        if (isDrag) {
            setPressed(false);
        }
        break;
    }
        return super.onTouchEvent(event);
    }

    private int getStatusBarHeight() {
        int resourceId = getContext ().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getNavigationBarHeight() {
        int resourceId = getContext ().getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\view\ListItemView.kt =====

package com.mpdc4gsr.module.user.view

import android.content.Context
import android.content.res.TypedArray
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mpdc4gsr.module.user.R

class ListItemView : LinearLayout {
    private lateinit var mIvLeftIcon: ImageView
    private lateinit var mIvLeftContent: TextView
    private lateinit var mIvRightContent: TextView
    private lateinit var mLineView: View
    private var lineShow: Boolean = false
    private var leftIconRes: Int = 0
    private var leftContent: String = ""
    private var rightContent: String = ""

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ListItemView)
        for (i in 0 until ta.indexCount) {
            when (ta.getIndex(i)) {
                R.styleable.ListItemView_list_item_left_icon ->
                    leftIconRes =
                        ta.getResourceId(R.styleable.ListItemView_list_item_left_icon, 0)

                R.styleable.ListItemView_list_item_left_text ->
                    leftContent =
                        ta.getString(R.styleable.ListItemView_list_item_left_text).toString()

                R.styleable.ListItemView_list_item_right_text ->
                    rightContent =
                        ta.getString(R.styleable.ListItemView_list_item_right_text).toString()

                R.styleable.ListItemView_list_item_line ->
                    lineShow =
                        ta.getBoolean(R.styleable.ListItemView_list_item_line, false)
            }
        }
        ta.recycle()
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private fun initView() {
        inflate(context, R.layout.ui_list_item_view, this)
        mIvLeftIcon = findViewById(R.id.iv_left_icon)
        mIvLeftContent = findViewById(R.id.iv_left_content)
        mIvRightContent = findViewById(R.id.iv_right_content)
        mLineView = findViewById(R.id.view_line)
        mIvLeftIcon.setImageResource(leftIconRes)
        mIvLeftContent.text = leftContent
        mIvRightContent.text = rightContent
        mLineView.visibility = if (lineShow) View.VISIBLE else View.GONE
    }

    fun setLeftText(text: CharSequence?) {
        if (TextUtils.isEmpty(text)) return
        mIvLeftContent.text = text
        mIvLeftContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun getLeftText(): String {
        return mIvLeftContent.text.toString()
    }

    fun setRightText(text: CharSequence?) {
        if (TextUtils.isEmpty(text)) return
        mIvRightContent.text = text
        mIvRightContent.movementMethod = LinkMovementMethod.getInstance()
    }

    fun getRightText(): String {
        return mIvRightContent.text.toString()
    }
}


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\view\ProgressBarView.java =====

package com.mpdc4gsr.module.user.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.mpdc4gsr.module.user.bean.ColorsBean;

import java.util.List;

public class ProgressBarView extends View {
    private Paint paint;
    private int totalParts = 100;
    private List < ColorsBean > colorsBeanList;

    public ProgressBarView (Context context) {
        super(context);
        init();
    }

    public ProgressBarView (Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressBarView (Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint ();
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth ();
        int height = getHeight ();
        float partWidth =(float) width / totalParts;
        RectF rect = new RectF(0, 0, width, height);
        paint.setColor(Color.parseColor("#00000000"));
        canvas.drawRoundRect(rect, 6f, 6f, paint);
        if (colorsBeanList != null) {
            for (int i = 0; i < colorsBeanList.size(); i++) {
                ColorsBean bean = colorsBeanList . get (i);
                paint.setColor(bean.getColor());
                RectF redRect = new RectF(
                    bean.getStart() * partWidth, 0,
                    bean.getEnd() * partWidth, height
                );
                canvas.drawRect(redRect, paint);
            }
        }
    }

    public void setSegmentPart(List<ColorsBean> colorsBeans) {
        this.colorsBeanList = colorsBeans;
        invalidate();
    }
}