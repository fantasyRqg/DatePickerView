package com.rqg.github.datepiker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
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
    companion object {
        const val SELECT_MODE_SINGLE = 0
        const val SELECT_MODE_RANGLE = 1
    }

    val TAG = "DatePickerView"

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
    private var mLineMargin = 10f
    private var mDividerHeight = 1f
    private var mCornerRadius = 5f

    private var mMonthTitleFormat = "%d年%d月"


    private var mSideWidth = -1.0f
    private var mContentWidth = -1


    private var mYear = 0
    private var mMonth = 0
    private var mCalendar = Calendar.getInstance(Locale.getDefault())

    private var mScrolled = 0f
    var mTextOffset = 0f

    private val mSelectedStart = DateDate()
    private val mSelectedEnd = DateDate()

    private var mSelectMode = SELECT_MODE_RANGLE

    private val mRoundRectPath = Path()

    private var mCallback: DatePickCallback? = null

    private val mScroller = Scroller(context)
    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            mScroller.forceFinished(true)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            val x = e?.x ?: return false
            val y = e.y

            //move down start line , find current click month start y
            var offset = -mScrolled - y +        //down move should reduce scrolled
                    (mSideWidth + mDividerHeight)       //week header height

            var (cy, cm, startY) = findStartYearMonthOffset(mYear, mMonth, offset, y)

            val (firstDayWeek, daysOfMonth, mh) = calcMonthHeight(cy, cm)
            startY += mSideWidth + mDividerHeight // remove month header height
            val toRowTop = y - startY
            val lineHeight = mSideWidth + mLineMargin
            val clickOnLine = (toRowTop / lineHeight).toInt()

            if (clickOnLine * lineHeight + mLineMargin > toRowTop) { // click on line margin region
                return false
            }
            val clickOnCol = (x / mSideWidth).toInt()

            val clickOnBox = clickOnLine * 7 + clickOnCol

            if (clickOnBox < firstDayWeek || clickOnBox >= daysOfMonth + firstDayWeek) { // click box has not date
                return false
            }

            val clickOnDayOfMonth = clickOnBox - firstDayWeek

            clickOnDate(cy, cm + 1, clickOnDayOfMonth + 1)

            return true
        }


        private fun clickOnDate(year: Int, month: Int, dayOfMonth: Int) {

            if (mSelectMode == SELECT_MODE_SINGLE) {
                if (mSelectedStart.equals(year, month, dayOfMonth)) {
                    mSelectedStart.set(null, null, null)
                } else {
                    mSelectedStart.set(year, month, dayOfMonth)
                }

                mCallback?.onDateSelect(year, month, dayOfMonth)

            } else {

                if (mSelectedStart.year == null) {
                    mSelectedStart.set(year, month, dayOfMonth)
                } else if (mSelectedStart.compare(year, month, dayOfMonth) >= 0 && mSelectedEnd.year == null) {
                    mSelectedStart.set(year, month, dayOfMonth)
                } else if (mSelectedEnd.year == null) {
                    mSelectedEnd.set(year, month, dayOfMonth)

                    mCallback?.onDateRangeSelect(mSelectedStart.year, mSelectedStart.month, mSelectedStart.dayOfMonth,
                            mSelectedEnd.year, mSelectedEnd.month, mSelectedEnd.dayOfMonth)
                } else {
                    mSelectedStart.set(null, null, null)
                    mSelectedEnd.set(null, null, null)
                }
            }

            postInvalidate()
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
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
        val typedArray = context!!.obtainStyledAttributes(attrs, R.styleable.DatePickerView)

        mHighlightColor = typedArray.getColor(R.styleable.DatePickerView_highlightColor, mHighlightColor)
        mRangeColor = typedArray.getColor(R.styleable.DatePickerView_rangeColor, mRangeColor)
        mNormalColor = typedArray.getColor(R.styleable.DatePickerView_normalColor, mNormalColor)
        mInverseColor = typedArray.getColor(R.styleable.DatePickerView_inverseColor, mInverseColor)
        mDividerColor = typedArray.getColor(R.styleable.DatePickerView_dividerColor, mDividerColor)

        mTextSize = typedArray.getDimensionPixelSize(R.styleable.DatePickerView_textSize, mTextSize.toInt()).toFloat()
        mLineMargin = typedArray.getDimensionPixelSize(R.styleable.DatePickerView_lineMargin, mLineMargin.toInt()).toFloat()
        mDividerHeight = typedArray.getDimensionPixelSize(R.styleable.DatePickerView_dividerHeight, mDividerHeight.toInt()).toFloat()
        mSelectMode = typedArray.getInt(R.styleable.DatePickerView_selectMode, SELECT_MODE_SINGLE)
        mCornerRadius = typedArray.getDimensionPixelSize(R.styleable.DatePickerView_cornerRadius, mCornerRadius.toInt()).toFloat()
        typedArray.recycle()

        initPaint()
    }

    private fun initPaint() {
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mRoundRectPath.reset()
            mRoundRectPath.moveTo(mCornerRadius, 0f)
            mRoundRectPath.lineTo(mSideWidth - mCornerRadius, 0f)
            mRoundRectPath.quadTo(mSideWidth, 0f, mSideWidth, mCornerRadius)
            mRoundRectPath.lineTo(mSideWidth, mSideWidth - mCornerRadius)
            mRoundRectPath.quadTo(mSideWidth, mSideWidth, mSideWidth - mCornerRadius, mSideWidth)
            mRoundRectPath.lineTo(mCornerRadius, mSideWidth)
            mRoundRectPath.quadTo(0f, mSideWidth, 0f, mSideWidth - mCornerRadius)
            mRoundRectPath.lineTo(0f, mCornerRadius)
            mRoundRectPath.quadTo(0f, 0f, mCornerRadius, 0f)
            mRoundRectPath.close()
        }
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
                (linesOfDay + 1) * mLineMargin +                    // margin between line
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


    private fun findStartYearMonthOffset(year: Int, month: Int, offset: Float, startY: Float): Triple<Int, Int, Float> {
        var cy = year
        var cm = month
        var startOffset = startY
        var pos = offset + startY

        //find first show month
        if (pos >= startY) {
            // original month below startY
            while (pos > startY) {
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
            while (pos < startY) {
                startOffset = pos
                val (_, _, mh) = calcMonthHeight(cy, cm)
                pos += mh

                if (pos >= startY) break

                cm++
                if (cm > 11) {
                    cy++
                    cm = 0
                }
            }
        }
        return Triple(cy, cm, startOffset)
    }

    private fun drawAllMonth(startY: Float, offset: Float, canvas: Canvas) {
        var (cy, cm, startOffset) = findStartYearMonthOffset(mYear, mMonth, offset, startY)

        var ssy = startOffset

        while (ssy < measuredHeight) {

            ssy += drawMonthTitle(ssy, canvas, cy, cm)
            ssy += drawDivider(ssy, canvas)
            ssy += drawDays(ssy, canvas, cy, cm)
            ssy += drawDivider(ssy, canvas)

            cm++
            if (cm > 11) {
                cy++
                cm = 0
            }
        }

    }

    private fun drawMonthTitle(startY: Float, canvas: Canvas, year: Int, month: Int): Float {
        canvas.drawText(String.format(mMonthTitleFormat, year, month + 1), mContentWidth / 2f + paddingLeft, startY + mSideWidth / 2f - mTextOffset, mMPaint)
        return mSideWidth
    }

    private fun drawDays(startY: Float, canvas: Canvas,
                         cy: Int,
                         cm: Int): Float {

        val (firstDayWeek, daysOfMonth, mh) = calcMonthHeight(cy, cm)

        val halfSizeWidth = mSideWidth / 2f
        var drawX = firstDayWeek * mSideWidth + halfSizeWidth + paddingLeft
        var drawY = startY + halfSizeWidth - mTextOffset + mLineMargin

        var paint: Paint

        for (i in 0 until daysOfMonth) {
            if ((i + firstDayWeek) % 7 == 0 && i != 0) {
                drawX = halfSizeWidth + paddingLeft
                drawY += mSideWidth + mLineMargin
            }

            if ((i + firstDayWeek) % 7 == 0 || (i + firstDayWeek) % 7 == 6) {
                paint = mHPaint
            } else {
                paint = mNPaint
            }

            //draw select state
            if (mSelectMode == SELECT_MODE_SINGLE) {
                if (mSelectedStart.equals(cy, cm + 1, i + 1)) {
                    drawRoundRect(canvas, drawX, drawY + mTextOffset, halfSizeWidth, mCornerRadius, mHPaint)
                    paint = mWPaint
                }
                canvas.drawText((i + 1).toString(), drawX, drawY, paint)
            } else {

                if (mSelectedStart.equals(cy, cm + 1, i + 1)) {
                    drawRoundRect(canvas, drawX, drawY + mTextOffset, halfSizeWidth, mCornerRadius, mHPaint)
                    canvas.drawText((i + 1).toString(), drawX, drawY, mWPaint)
                } else if (mSelectedEnd.equals(cy, cm + 1, i + 1)) {
                    drawRoundRect(canvas, drawX, drawY + mTextOffset, halfSizeWidth, mCornerRadius, mHPaint)
                    canvas.drawText((i + 1).toString(), drawX, drawY, mWPaint)
                } else if (inSelectRange(cy, cm + 1, i + 1)) {
                    canvas.drawRect(drawX - halfSizeWidth, drawY + mTextOffset - halfSizeWidth, drawX + halfSizeWidth, drawY + mTextOffset + halfSizeWidth, mRPaint)
                    canvas.drawText((i + 1).toString(), drawX, drawY, paint)
                } else {
                    canvas.drawText((i + 1).toString(), drawX, drawY, paint)
                }

            }

            drawX += mSideWidth
        }


        return mh - mSideWidth - mDividerHeight * 2;
    }


    private fun inSelectRange(year: Int, month: Int, daysOfMonth: Int): Boolean {
        if (mSelectedStart.year == null || mSelectedEnd.year == null)
            return false

        if (mSelectedStart.compare(year, month, daysOfMonth) < 0 && mSelectedEnd.compare(year, month, daysOfMonth) > 0) {
            return true
        }

        return false
    }


    private fun drawRoundRect(canvas: Canvas, drawX: Float, drawY: Float, halfSizeWidth: Float, cornerRadius: Float, paint: Paint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(drawX - halfSizeWidth, drawY - halfSizeWidth, drawX + halfSizeWidth, drawY + halfSizeWidth,
                    cornerRadius, cornerRadius, paint)
        } else {
            canvas.save()
            canvas.translate(drawX - halfSizeWidth, drawY - halfSizeWidth)
            canvas.drawPath(mRoundRectPath, paint)
            canvas.restore()
        }
    }

    private fun drawDivider(startY: Float, canvas: Canvas): Float {
        val drawY = mDividerHeight / 2f + startY
        canvas.drawLine(0f, drawY, measuredWidth.toFloat(), drawY, mDPaint)
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


    fun setDatePickCallback(callback: DatePickCallback?) {
        mCallback = callback
    }


//    fun setSelectDateRange(startYear: Int, startMonth: Int, startDayOfMonth: Int, endYear: Int, endMonth: Int, endDayOfMonth: Int) {
//        mSelectedStart.set(startYear, startMonth, startDayOfMonth)
//        mSelectedEnd.set(endYear, endMonth, endDayOfMonth)
//
//        if (mSelectedStart.compare(mSelectedEnd) >= 0) {
//            mSelectedStart.reset()
//            mSelectedEnd.reset()
//        }
//
//        postInvalidate()
//    }
}

private data class DateDate(
        var year: Int? = null,
        var month: Int? = null,
        var dayOfMonth: Int? = null
) {
    fun set(year: Int?, month: Int?, daysOfMonth: Int?) {
        this.year = year
        this.month = month
        this.dayOfMonth = daysOfMonth
    }

    fun equals(year: Int?, month: Int?, daysOfMonth: Int?): Boolean {
        return this.year == year &&
                this.month == month &&
                this.dayOfMonth == daysOfMonth
    }

    fun noNull(): Boolean {
        return year != null && month != null && dayOfMonth != null
    }

    fun compare(date: DateDate): Int {
        if (noNull())
            return compare(date.year!!, date.month!!, date.dayOfMonth!!)
        else {
            return -1
        }
    }

    fun compare(year: Int, month: Int, daysOfMonth: Int): Int {
        if (this.year == null || this.month == null || this.dayOfMonth == null)
            return -1

        if (this.year != year) {
            return this.year!! - year
        }

        if (this.month != month)
            return this.month!! - month

        if (this.dayOfMonth != daysOfMonth)
            return this.dayOfMonth!! - daysOfMonth


        return 0
    }


    fun reset() {
        year = null
        month = null
        dayOfMonth = null
    }

}


interface DatePickCallback {
    fun onDateSelect(year: Int?, month: Int?, dayOfMonth: Int?)

    fun onDateRangeSelect(startYear: Int?, startMonth: Int?, startDaysOfMonth: Int?, endYear: Int?, endMonth: Int?, endDaysOfMonth: Int?)
}
