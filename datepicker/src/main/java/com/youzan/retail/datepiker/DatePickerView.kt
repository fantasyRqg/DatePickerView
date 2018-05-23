package com.youzan.retail.datepiker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import java.text.SimpleDateFormat
import java.util.*

/**
 * * Created by rqg on 2018/5/23.
 */


class DatePickerView : View {
    private val mHPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mNPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mDPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mMPaint = Paint(Paint.ANTI_ALIAS_FLAG)


    private val mWeekLabels by lazy {

        val dateFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            "EEEEE"
        } else {
            "E"
        }
        val dft = SimpleDateFormat(dateFormat, Locale.getDefault())
        val calendar = Calendar.getInstance(Locale.getDefault())

        val labels = mutableListOf<String>()

        for (i in 0 until 7) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + i)
            labels.add(dft.format(calendar.time))
        }
        labels.toList()
    }

    private var mHighlightColor = Color.parseColor("#0983F6")
    private var mRangeColor = Color.parseColor("#3C0983F6")
    private var mNormalColor = Color.BLACK
    private var mInverseColor = Color.WHITE
    private var mDividerColor = Color.GRAY

    private var mTextSize = 30f
    private var mLineMargin = 10
    private var mDividerHeight = 1f

    private var mMonthTitleFormat = "%d年%d月"


    private var mSideWidth = -1.0f
    private var mContentWidth = -1


    private var mYear = 0
    private var mMonth = 0
    private var mCalendar = Calendar.getInstance(Locale.getDefault())

    private var mScrolled = 0f
    var mTextOffset = 0f


    private val mScroller = Scroller(context)
    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            mScroller.forceFinished(true)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {

            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            Log.d(TAG, "onFling() called with: e1 = [  ], e2 = [  ], velocityX = [ ${velocityX} ], velocityY = [ ${velocityY} ]")
            mScroller.fling(0,
                    mScrolled.toInt(),
                    0,
                    -velocityY.toInt(),
                    0, 0,
                    mScrolled.toInt() - measuredHeight * 4,
                    mScrolled.toInt() + measuredHeight * 4
            )
            postInvalidate()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            mScrolled += distanceY
            postInvalidate()
            return true
        }
    }
    private val mGestureDetector = GestureDetector(context, mGestureListener)

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {


        mHPaint.color = mHighlightColor
        mRPaint.color = mRangeColor
        mNPaint.color = mNormalColor
        mWPaint.color = mInverseColor
        mDPaint.color = mDividerColor
        mMPaint.color = mNormalColor

        mHPaint.textSize = mTextSize
        mRPaint.textSize = mTextSize
        mNPaint.textSize = mTextSize
        mWPaint.textSize = mTextSize
        mMPaint.textSize = mTextSize


        mHPaint.textAlign = Paint.Align.CENTER
        mRPaint.textAlign = Paint.Align.CENTER
        mNPaint.textAlign = Paint.Align.CENTER
        mWPaint.textAlign = Paint.Align.CENTER
        mMPaint.textAlign = Paint.Align.CENTER

        mMPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        mDPaint.strokeWidth = mDividerHeight


        mTextOffset = (mNPaint.ascent() + mNPaint.descent()) / 2f


        mYear = mCalendar.get(Calendar.YEAR)
        mMonth = mCalendar.get(Calendar.MONTH)
    }

    fun setLineMargin(margin: Int) {
        mLineMargin = margin
    }


    fun setDate(year: Int, month: Int) {
        mYear = year
        mMonth = month

        mScrolled = 0f

        postInvalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        mContentWidth = measuredWidth - paddingLeft - paddingRight
        mSideWidth = mContentWidth / 7.0f
    }

    override fun onDraw(canvas: Canvas?) {
        var startY = 0f
        startY += drawWeek(canvas!!)
        startY += drawDivider(startY, canvas)

        drawContent(startY, canvas)
    }


    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mScrolled = mScroller.currY.toFloat()
            postInvalidate()
        }
    }

    private fun calcMonthHeight(year: Int, month: Int): Triple<Int, Int, Float> {
        val cal = mCalendar
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val daysOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

        val dd = firstDayWeek + daysOfMonth

        val linesOfDay = if (dd % 7 == 0) {
            dd / 7
        } else {
            dd / 7 + 1
        }

        val height = linesOfDay * mSideWidth +                //day line
                (linesOfDay - 1) * mLineMargin +                    // margin between line
                mDividerHeight +                                    //  divider height
                mSideWidth +                                        // month title height
                mDividerHeight                                      //end divider height

        return Triple(firstDayWeek, daysOfMonth, height)
    }

    private fun drawContent(startY: Float, canvas: Canvas) {
        val restorePoint = canvas.save()
        canvas.clipRect(0, startY.toInt(), measuredWidth, measuredHeight)
        val offset = -mScrolled

        drawAllMonth(startY, offset, canvas)

        canvas.restoreToCount(restorePoint)
    }


    val TAG = "DatePickerView"
    private fun drawAllMonth(startY: Float, offset: Float, canvas: Canvas) {
        var cy = mYear
        var cm = mMonth

        var startOffset = 0f

        //find first show month
        if (offset >= 0) {
            // original month below startY
            var pos = offset


            while (pos > 0) {
                cm--
                if (cm < 0) {
                    cy--
                    cm = 11
                }
                val (_, _, mh) = calcMonthHeight(cy, cm)
                pos -= mh
            }
            startOffset = pos

        } else {
            // original month above or on startY
            var pos = offset

            while (pos < 0) {
                startOffset = pos
                val (_, _, mh) = calcMonthHeight(cy, cm)
                pos += mh

                if (pos >= 0) break

                cm++
                if (cm > 11) {
                    cy++
                    cm = 0
                }
            }
        }

        var ssy = startY + startOffset

        while (ssy < measuredHeight) {

            ssy += drawMonthTitle(ssy, canvas, cy, cm)
            ssy += drawDivider(ssy, canvas)
            val (firstDayWeek, daysOfMonth, mh) = calcMonthHeight(cy, cm)
            drawDays(ssy, canvas, firstDayWeek, daysOfMonth)

            ssy += mh - mSideWidth - mDividerHeight * 2

            ssy += drawDivider(ssy, canvas)

            cm++
            if (cm > 11) {
                cy++
                cm = 0
            }
        }

    }

    private fun drawMonthTitle(startY: Float, canvas: Canvas, year: Int, month: Int): Float {
        canvas.drawText(String.format(mMonthTitleFormat, year, month + 1), mContentWidth / 2f, startY + mSideWidth / 2f - mTextOffset, mMPaint)
        return mSideWidth
    }


    private fun drawDays(startY: Float, canvas: Canvas,
                         firstDayWeek: Int,
                         daysOfMonth: Int) {
        var drawX = firstDayWeek * mSideWidth + mSideWidth / 2f
        var drawY = startY + mSideWidth / 2f - mTextOffset

        var paint: Paint

        for (i in 0 until daysOfMonth) {
            if ((i + firstDayWeek) % 7 == 0 && i != 0) {
                drawX = mSideWidth / 2f
                drawY += mSideWidth + mDividerHeight
            }

            if ((i + firstDayWeek) % 7 == 0 || (i + firstDayWeek) % 7 == 6) {
                paint = mHPaint
            } else {
                paint = mNPaint
            }

            canvas.drawText((i + 1).toString(), drawX, drawY, paint)

            drawX += mSideWidth
        }

    }

    private fun drawDivider(startY: Float, canvas: Canvas): Float {
        val drawY = mDividerHeight / 2f + startY
        canvas.drawLine(paddingLeft.toFloat(), drawY, measuredWidth.toFloat(), drawY, mDPaint)
        return mDividerHeight
    }

    private fun drawWeek(canvas: Canvas): Float {
        var drawX = paddingLeft + mSideWidth / 2f
        val drawY = paddingTop + mSideWidth / 2f - mTextOffset


        for (i in 0..6) {
            val paint = if (i == 0 || i == 6) {
                mHPaint
            } else {
                mNPaint
            }
            canvas.drawText(mWeekLabels[i], drawX, drawY, paint)
            drawX += mSideWidth
        }

        return mSideWidth
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mGestureDetector.onTouchEvent(event)
    }

}
