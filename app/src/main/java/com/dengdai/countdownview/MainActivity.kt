package com.dengdai.countdownview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keep1.countdownListener =
            object : com.dengdai.countdownview.KeepCountdownView.CountdownListener {
                override fun onStart() {

                }

                override fun onEnd() {

                }
            }

        b1.setOnClickListener(this)
        b2.setOnClickListener(this)
        b3.setOnClickListener(this)

        tvCountDown
            .setNormalText("获取验证码")
            .setCountDownText("(", "s)")
            .setCloseKeepCountDown(false)//关闭页面保持倒计时开关
            .setCountDownClickable(false)//倒计时期间点击事件是否生效开关
            .setShowFormatTime(false)//是否格式化时间
            .setIntervalUnit(TimeUnit.SECONDS)
            .setOnCountDownStartListener { Toast.makeText(this@MainActivity, "开始计时", Toast.LENGTH_SHORT).show() }
            .setOnCountDownTickListener { Log.e("------", "onTick: $it") }
            .setOnCountDownFinishListener { Toast.makeText(this@MainActivity, "倒计时完毕", Toast.LENGTH_SHORT).show() }
            .setOnClickListener { Toast.makeText(this@MainActivity, "短信已发送", Toast.LENGTH_SHORT).show()
                    tvCountDown.startCountDown(5)

            }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.b1 -> {
                keep1.plus(5)
            }
            R.id.b2 -> {
                keep1.post(Runnable { kotlin.run { keep1.startCountDown() } })
            }
            R.id.b3 -> {
                keep1.reset()
            }
        }
    }
}