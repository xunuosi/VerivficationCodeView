package com.lite.verificationcodeview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

public class VerificationCodeView extends AppCompatEditText implements TextWatcher {
    private static final int INPUT_TYPE_NONE = 0;
    private static final int INPUT_TYPE_NUM = 1;
    private static final int INPUT_TYPE_TEXT = 2;

    private final float DEFAULT_BOTTOM_SEGMENT_WIDTH = GaugeUtil.dpToPx(40);
    private final float DEFAULT_FONT_SIZE = GaugeUtil.dpToPx(40);
    private final String DEFAULT_LINE_COLOR = "#FFCFCFCF";
    private final String DEFAULT_FOCUS_LINE_COLOR = "#FF557ECB";
    private final String DEFAULT_FONT_COLOR = "#FF557ECB";
    private final float GAP_DISTANCE = GaugeUtil.dpToPx(7);
    private final float POINTER_GAP = GaugeUtil.dpToPx(9);
    private final float TEXT_BOTTOM_GAP = POINTER_GAP;
    private final float POINTER_WIDTH = GaugeUtil.dpToPx(1.8f);
    private float POINTER_HEIGHT;

    private Paint _paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float LINE_HEIGHT = GaugeUtil.dpToPx(0.9f);
    private float bottomRectLineWidth;
    private int inputLimit = 5;
    private TimerTask _cursorTimerTask;
    private Timer _cursorTimer;
    private boolean _isVisibleCursor = true;
    private boolean _needVisibleCursor = _isVisibleCursor;
    private int _singleFontWidth, _singleFontHeight;
    private InputListener _inputListener;
    private int _bottomLineNormalColor, _bottomLineFocusColor;
    private int _fontColor;
    private float _fontSize;
    private int _inputType;

    public VerificationCodeView(Context context) {
        this(context, null);
    }

    public VerificationCodeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerificationCodeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        initAttrs(context, attrs);
        refreshCursorFlickerState();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerificationCodeView);
        try {
            inputLimit = typedArray.getInt(R.styleable.VerificationCodeView_inputMax, 5);
            _bottomLineNormalColor = typedArray.getColor(R.styleable.VerificationCodeView_bottomColor, Color.parseColor(DEFAULT_LINE_COLOR));
            _bottomLineFocusColor = typedArray.getColor(R.styleable.VerificationCodeView_bottomFocusColor, Color.parseColor(DEFAULT_FOCUS_LINE_COLOR));
            _fontColor = typedArray.getColor(R.styleable.VerificationCodeView_textColor, Color.parseColor(DEFAULT_FONT_COLOR));
            _fontSize = typedArray.getDimension(R.styleable.VerificationCodeView_textSize, DEFAULT_FONT_SIZE);
            _inputType = typedArray.getInt(R.styleable.VerificationCodeView_inputType, INPUT_TYPE_NUM);
        } finally {
            typedArray.recycle();
        }

        switch (_inputType) {
            case INPUT_TYPE_NUM:
                setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case INPUT_TYPE_TEXT:
                setInputType(InputType.TYPE_CLASS_TEXT);
                break;
        }

        _paint.setColor(_bottomLineNormalColor);
        _paint.setStyle(Paint.Style.FILL_AND_STROKE);
        _paint.setStrokeCap(Paint.Cap.ROUND);
        _paint.setTextSize(_fontSize);

        Rect bounds = new Rect();
        _paint.getTextBounds("中", 0, 1, bounds);
        _singleFontWidth = bounds.right - bounds.left;
        _singleFontHeight = bounds.bottom - bounds.top;
        POINTER_HEIGHT = _singleFontHeight;

        setFocusableInTouchMode(true);
        super.addTextChangedListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            setLayoutDirection(LAYOUT_DIRECTION_LTR);
        }

    }

    private void refreshCursorFlickerState() {
        if (_cursorTimer == null) {
            _cursorTimer = new Timer();
        }
        if (_isVisibleCursor) {
            if (_cursorTimerTask == null) {
                _cursorTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        _needVisibleCursor = !_needVisibleCursor;
                        postInvalidate();
                    }
                };
            }
            _cursorTimerTask.run();
        } else {
            if (_cursorTimerTask != null) {
                _cursorTimerTask.cancel();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            requestFocus();
            setSelection(getText().length());
            showKeyBoard(getContext());
            return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthResult = 0;
        int heightResult = 0;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            widthResult = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            int segmentLineOptionalWidth = (int) Math.max(DEFAULT_BOTTOM_SEGMENT_WIDTH, _singleFontWidth);
            widthResult = (int) (getPaddingLeft() + getPaddingRight() + segmentLineOptionalWidth * inputLimit + (inputLimit > 0 ? GAP_DISTANCE * (inputLimit - 1) : 0));
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            heightResult = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            heightResult = (int) (getPaddingTop() + getPaddingBottom() + TEXT_BOTTOM_GAP + POINTER_HEIGHT);
        }

        setMeasuredDimension(widthResult, heightResult);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bottomRectLineWidth = (getWidth() - getPaddingLeft() - getPaddingRight() - (inputLimit - 1) * GAP_DISTANCE) / inputLimit;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        _cursorTimer.scheduleAtFixedRate(_cursorTimerTask, 0, 600);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        _cursorTimer.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw text
        _paint.setStyle(Paint.Style.FILL);
        _paint.setTextAlign(Paint.Align.CENTER);
        _paint.setColor(_fontColor);
        String value = getText().toString();
        for (int i = 0; i < value.length(); i++) {
            float startPosition = bottomRectLineWidth / 2 + (bottomRectLineWidth + GAP_DISTANCE) * i;
            canvas.drawText(String.valueOf(value.charAt(i)), startPosition, getBottom() - getTop() - TEXT_BOTTOM_GAP - LINE_HEIGHT, _paint);
        }

        // Draw bottom line
        for (int i = 0; i < inputLimit; i++) {
            float offset = (bottomRectLineWidth + GAP_DISTANCE) * i;
            if (i < value.length()) {
                _paint.setColor(_bottomLineFocusColor);
            } else {
                _paint.setColor(_bottomLineNormalColor);
            }
            canvas.drawRoundRect(offset, getBottom() - getTop() - LINE_HEIGHT,
                    offset + bottomRectLineWidth, getBottom() - getTop(),
                    GaugeUtil.dpToPx(2),
                    GaugeUtil.dpToPx(2),
                    _paint);
        }
        // Draw cursor
        if (value.length() >= inputLimit) {
            return;
        }
        if (_needVisibleCursor) {
            float cursorStartPosi = (bottomRectLineWidth + GAP_DISTANCE) * value.length() + bottomRectLineWidth / 2;
            _paint.setColor(_fontColor);
            _paint.setStrokeWidth(POINTER_WIDTH);
            canvas.drawLine(cursorStartPosi, getBottom() - getTop() - POINTER_GAP - POINTER_HEIGHT,
                    cursorStartPosi, getBottom() - getTop() - POINTER_GAP, _paint);
        }
    }

    private void showKeyBoard(Context context) {
        InputMethodManager inputService = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputService.showSoftInput(VerificationCodeView.this, InputMethodManager.SHOW_FORCED);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (getText().length() <= inputLimit) {
            if (_inputListener != null) {
                if (getText().length() == inputLimit) {
                    _inputListener.inputComplete(s.toString());
                } else {
                    _inputListener.afterTextChanged(s);
                }
            }
            postInvalidate();
        } else {
            // Delete invalid text
            getText().replace(inputLimit, getText().length(), "", 0, 0);
        }
    }

    @Override
    public void setCursorVisible(boolean visibleCursor) {
        super.setCursorVisible(visibleCursor);
        _isVisibleCursor = visibleCursor;
        refreshCursorFlickerState();
    }

    public void setBottomLineNormalColor(int bottomLineNormalColor) {
        _bottomLineNormalColor = bottomLineNormalColor;
    }

    public void setBottomLineFocusColor(int bottomLineFocusColor) {
        _bottomLineFocusColor = bottomLineFocusColor;
    }

    public void setFontColor(int fontColor) {
        _fontColor = fontColor;
    }

    public void setFontSize(float fontSize) {
        _fontSize = fontSize;
    }

    public void setInputLimit(int inputLimit) {
        this.inputLimit = inputLimit;
    }

    public void setInputListener(InputListener inputListener) {
        _inputListener = inputListener;
    }

    public interface InputListener {
        public void afterTextChanged(Editable s);

        public void inputComplete(String inputStr);
    }
}
