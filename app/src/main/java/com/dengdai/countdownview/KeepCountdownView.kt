package com.dengdai.countdownview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.ceil


class KeepCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    //属性相关
    /**
     * 圆弧颜色
     */
    private val arcColor: Int

    /**
     * 数字颜色
     */
    private val numColor: Int

    /**
     * 圆弧厚度 px
     */
    private val arcStrokeWidth: Float

    /**
     * 数字大小 px
     */
    private val numTextSize: Float

    /**
     * 圆弧半径
     */
    private val radius: Float

    /**
     * 倒计时开始角度
     */
    private var initDegree: Int

    /**
     * 是否顺时针倒计时
     */
    private val isCW: Boolean

    /**
     * 倒计时最大数值（时长）
     */
    private var maxNum: Float

    /**
     * 见[.plus]
     */
    private var maxNumForText : Float//用于文字


    /**
     * 动态增加、减少倒计时的圆弧动画时长(s)
     */
    private val plusNumAnimDuration: Float

    //绘图相关
    private var arcPaint: Paint? = null
    private var numPaint: Paint? = null
    private var arcFraction = 1.0f
    private var numFraction = 1.0f
    private var arcRectF: RectF? = null
    var countdownListener: CountdownListener? = null
    private var countdownAnim: AnimatorSet? = null
    private var numCountdownAnim: ValueAnimator? = null
    private var arcCountdownAnim: ValueAnimator? = null
    private var plusArcAnim: ValueAnimator? = null
    private var canceledByOut = false
    private fun initPaint() {
        arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        arcPaint!!.color = arcColor
        arcPaint!!.strokeWidth = arcStrokeWidth
        arcPaint!!.style = Paint.Style.STROKE
        //        arcPaint.setStrokeCap(Paint.Cap.ROUND); //圆角
        numPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        numPaint!!.color = numColor
        numPaint!!.textSize = numTextSize
        numPaint!!.textAlign = Paint.Align.CENTER
    }

    private fun initData() {
        arcRectF = RectF(-radius, -radius, radius, radius)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var wSize = MeasureSpec.getSize(widthMeasureSpec)
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        var hSize = MeasureSpec.getSize(heightMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)

        //WRAP_CONTENT
        if (wMode != MeasureSpec.EXACTLY) {
            wSize = calculateMinWidth()
        }
        if (hMode != MeasureSpec.EXACTLY) {
            hSize = calculateMinWidth()
        }
        setMeasuredDimension(wSize, hSize)
    }

    /**
     * 计算控件最小边长
     *
     * @return
     */
    private fun calculateMinWidth(): Int {
        val minWidth = (arcStrokeWidth / 2.0f + radius) * 2
        return (minWidth + dp2px(4f)).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(measuredWidth / 2.0f, measuredHeight / 2.0f)
        drawArc(canvas)
        drawNum(canvas)
    }

    private fun drawNum(canvas: Canvas) {
        canvas.save()
        val metrics = numPaint!!.fontMetrics
        val currentNum = "" + ceil(
            getCurrentNumByFraction(
                numFraction,
                maxNumForText
            ).toDouble()
        ).toInt()
        canvas.drawText(
            currentNum
            , 0f, 0 - (metrics.ascent + metrics.descent) / 2 //真正居中);
            , numPaint!!
        )
        canvas.restore()
    }

    private fun drawArc(canvas: Canvas) {
        canvas.save()
        val currentSweepDegree = 360.getCurrentSweepDegree(arcFraction)
        val startAngle: Float
        val sweepAngle: Float
        if (isCW) {
            startAngle = initDegree - currentSweepDegree
            sweepAngle = currentSweepDegree
        } else {
            startAngle = initDegree.toFloat()
            sweepAngle = currentSweepDegree
        }
        canvas.drawArc(
            arcRectF!!
            , startAngle
            , sweepAngle
            , false
            , arcPaint!!
        )
        canvas.restore()
    }

    fun startCountDown() {
        if (countdownAnim != null && countdownAnim!!.isRunning) {
            countdownAnim!!.cancel()
            countdownAnim = null
        }
        countdownAnim = AnimatorSet()
        countdownAnim!!.playTogether(numAnim, arcAnim)
        countdownAnim!!.interpolator = LinearInterpolator()
        countdownAnim!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                canceledByOut = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (canceledByOut) {
                    canceledByOut = false
                    return
                }
                if (countdownListener != null) {
                    countdownListener!!.onEnd()
                }
            }

            override fun onAnimationStart(animation: Animator) {
                if (countdownListener != null) {
                    countdownListener!!.onStart()
                }
            }
        })
        countdownAnim!!.start()
    }

    private val numAnim: ValueAnimator?
        get() {
            if (numCountdownAnim != null) {
                numCountdownAnim!!.cancel()
                numCountdownAnim = null
            }
            numCountdownAnim = ValueAnimator.ofFloat(numFraction, 0.0f)
            numCountdownAnim!!.interpolator = LinearInterpolator()
            numCountdownAnim!!.duration = (getCurrentNumByFraction(
                numFraction,
                maxNumForText
            ) * 1000).toLong()
            numCountdownAnim!!.addUpdateListener { animation ->
                numFraction = animation.animatedValue as Float
                postInvalidate()
            }
            return numCountdownAnim
        }

    private val arcAnim: ValueAnimator?
        get() {
            if (arcCountdownAnim != null) {
                arcCountdownAnim!!.cancel()
                arcCountdownAnim = null
            }
            arcCountdownAnim = ValueAnimator.ofFloat(arcFraction, 0.0f)
            arcCountdownAnim!!.interpolator = LinearInterpolator()
            arcCountdownAnim!!.duration = (getCurrentNumByFraction(
                arcFraction,
                maxNum
            ) * 1000).toLong()
            arcCountdownAnim!!.addUpdateListener { animation ->
                arcFraction = animation.animatedValue as Float
                postInvalidate()
            }
            return arcCountdownAnim
        }

    /**
     * 重置
     */
    fun reset() {
        if (countdownAnim != null) {
            countdownAnim!!.cancel()
            countdownAnim = null
        }
        if (plusArcAnim != null) {
            plusArcAnim!!.cancel()
            plusArcAnim = null
        }
        if (numCountdownAnim != null) {
            numCountdownAnim!!.cancel()
            numCountdownAnim = null
        }
        if (arcCountdownAnim != null) {
            arcCountdownAnim!!.cancel()
            arcCountdownAnim = null
        }
        arcFraction = 1.0f
        numFraction = 1.0f
        invalidate()
    }

    /**
     * 增加、减少倒计时时间
     * 1、当plusNum为正数，若plusNum + currentNum > maxNum，那么圆弧会增长到超过360度，所以一旦到360度后，应当重新根据新maxNum重置动画速度
     * 2、当plusNum为负数，若plusNum + currentNum < 0，即倒计时直接减到结束了
     * 3、计算时要考虑到圆弧增长动画时长plusNumDuration的影响。
     *
     * @param plusNum
     */
    operator fun plus(plusNum: Int) {
        if (countdownAnim != null) {
            countdownAnim!!.cancel()
        }
        if (numCountdownAnim != null) {
            numCountdownAnim!!.cancel()
        }
        if (arcCountdownAnim != null) {
            arcCountdownAnim!!.cancel()
        }
        if (plusArcAnim != null && plusArcAnim!!.isRunning) {
            //正在增长动画变化中，不允许重叠调用
            return
        }
        val gotoNum = plusNum + getCurrentNumByFraction(numFraction, maxNum)
        var gotoFraction = getCurrentFractionByNum(gotoNum, maxNum)

        //如果增加、减少的时间比圆弧增长动画还短，那么直接变过去
        if (abs(plusNum) <= plusNumAnimDuration) {
            //情况1：直接减到了0，那么动画结束
            if (gotoNum <= 0) {
                arcFraction = 0.0f
                numFraction = arcFraction
                invalidate()
                if (countdownListener != null) {
                    countdownListener!!.onEnd()
                }
                return
            }
            //情况2：加到了比原来maxNum还大，那么直接变回360度然后重新倒计时
            if (gotoNum > maxNum) {
                maxNumForText = gotoNum
                maxNum = maxNumForText
                arcFraction = 1.0f
                numFraction = arcFraction
            } else {
                //情况3：正常变到对应位置
                arcFraction = gotoFraction
                numFraction = arcFraction
            }
            startCountDown()
            return
        }

        //增加、减少的时间比圆弧增长动画长，可以做动画
        if (plusNum > 0) {
            //情况1：减掉圆弧增长动画时长后仍比原来maxNum还大，那么就是圆弧变回360度然后重新根据剩余时间进行倒计时
            if (gotoNum - plusNumAnimDuration > maxNum) {
                //数字直接更新到最新值并开始倒计时，圆弧则要动画到最新值
                maxNumForText = gotoNum
                maxNum = maxNumForText
                gotoFraction = 1.0f
                numFraction = 1.0f
                numAnim!!.start()
                plusArcAnim = ValueAnimator.ofFloat(arcFraction, gotoFraction)
                plusArcAnim!!.interpolator = LinearInterpolator()
                plusArcAnim!!.duration = (plusNumAnimDuration * 1000).toLong()
                plusArcAnim!!.addUpdateListener { animation ->
                    arcFraction = animation.animatedValue as Float
                    postInvalidate()
                }
                plusArcAnim!!.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        canceledByOut = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (canceledByOut) {
                            canceledByOut = false
                            return
                        }
                        //文字是直接变到新的值然后动画倒计时的，但是圆弧需要花费plusNumAnimDuration的时间去做
                        //增长动画，等增长动画结束后（也就是增回到360度后），此时圆弧开始重新倒计时，其倒计时时间
                        //是减去plusNumAnimDuration所剩下的时间，这是区别于文字的，这是额外定义maxNumForText的原因
                        maxNum -= plusNumAnimDuration
                        arcAnim!!.start()
                    }
                })
                plusArcAnim!!.start()
            } else {
                //情况2：减掉增长动画时长后比原来maxNum小

                //数字直接更新到最新值并开始倒计时动画，圆弧则要动画到最新值
                numFraction = gotoFraction
                numAnim!!.start()
                gotoFraction = getCurrentFractionByNum(gotoNum - plusNumAnimDuration, maxNum)
                plusArcAnim = ValueAnimator.ofFloat(arcFraction, gotoFraction)
                plusArcAnim!!.interpolator = LinearInterpolator()
                plusArcAnim!!.duration = (plusNumAnimDuration * 1000).toLong()
                plusArcAnim!!.addUpdateListener(AnimatorUpdateListener { animation ->
                    arcFraction = animation.animatedValue as Float
                    postInvalidate()
                })
                plusArcAnim!!.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        canceledByOut = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (canceledByOut) {
                            canceledByOut = false
                            return
                        }
                        arcAnim!!.start()
                    }
                })
                plusArcAnim!!.start()
            }
        } else if (plusNum < 0) {
            //todo 思路同上。略
        }
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    /**
     * 根据当前倒计时进度比例和倒计时时长换算出当前倒计时值
     *
     * @param numFraction 当前倒计时进度比例
     * @param maxNum      倒计时最大值（倒计时时长）
     * @return 当前倒计时值(s ）
     */
    private fun getCurrentNumByFraction(numFraction: Float, maxNum: Float): Float {
        return numFraction * maxNum
    }

    /**
     * 根据当前倒计时值和倒计时时长换算出进度比例
     *
     * @param currentNum 当前倒计时值(s)
     * @param maxNum     倒计时最大值（倒计时时长）
     * @return 进度比例
     */
    private fun getCurrentFractionByNum(currentNum: Float, maxNum: Float): Float {
        return currentNum / maxNum
    }

    /**
     * 圆弧当前弧度计算
     *
     * @param arcFraction
     * @param this@getCurrentSweepDegree
     * @return
     */
    private fun Int.getCurrentSweepDegree(arcFraction: Float): Float {
        return this * arcFraction
    }

    // todo 注意setter逻辑要根据具体情况调用invalidate、requestLayout、重置动画等操作。
    private fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            resources.displayMetrics
        )
    }

    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp,
            resources.displayMetrics
        )
    }

    /**
     * 监听接口
     */
    interface CountdownListener {
        fun onStart()
        fun onEnd()
    }

    companion object {
        //默认值
        const val ARC_COLOR = -0xcc339a//圆弧颜色
        const val NUM_COLOR = -0xcc339a//数字颜色
        const val ARC_STROKE_WIDTH_IN_DP = 5//圆弧厚度
        const val NUM_TEXT_SIZE_IN_SP = 70//数字大小sp
        const val RADIUS_IN_DP = 90f//圆半径
        const val INIT_DEGREE = 270//倒计时开始角度
        const val MAX_NUM = 20//倒计时时间
        const val IS_CW = false//是否顺时针倒计时
        const val PLUS_NUM_ANIM_DURATION = 0.8f//动态增加/减少时间时圆弧增长动画时长
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.KeepCountdownView)
        arcColor = ta.getColor(
            R.styleable.KeepCountdownView_arcColor,
            ARC_COLOR
        )
        numColor = ta.getColor(
            R.styleable.KeepCountdownView_numColor,
            NUM_COLOR
        )
        arcStrokeWidth = ta.getDimension(
            R.styleable.KeepCountdownView_arcStrokeWidth,
            dp2px(ARC_STROKE_WIDTH_IN_DP.toFloat())
        )
        numTextSize = ta.getDimension(
            R.styleable.KeepCountdownView_numTextSize,
            sp2px(NUM_TEXT_SIZE_IN_SP.toFloat())
        )
        radius = ta.getDimension(
            R.styleable.KeepCountdownView_radius,
            dp2px(RADIUS_IN_DP)
        )
        initDegree = ta.getInt(
            R.styleable.KeepCountdownView_initDegree,
            INIT_DEGREE
        )
        maxNumForText =
            ta.getInt(
                R.styleable.KeepCountdownView_maxNum,
                MAX_NUM
            )
                .toFloat()
        maxNum = maxNumForText
        isCW = ta.getBoolean(
            R.styleable.KeepCountdownView_isCW,
            IS_CW
        )
        plusNumAnimDuration = ta.getFloat(
            R.styleable.KeepCountdownView_plusNumAnimDuration,
            PLUS_NUM_ANIM_DURATION
        )
        ta.recycle()
        initDegree %= 360
        initPaint()
        initData()
    }
}