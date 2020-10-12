package com.dengdai.countdownview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keep1.countdownListener = object : com.dengdai.countdownview.KeepCountdownView.CountdownListener{
            override fun onStart() {

            }

            override fun onEnd() {

            }
        }

        b1.setOnClickListener(this)
        b2.setOnClickListener(this)
        b3.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.b1->{
                keep1.plus(5)
            }
            R.id.b2->{
                keep1.post(Runnable { kotlin.run { keep1.startCountDown() } })
            }
            R.id.b3->{
                keep1.reset()
            }
        }
    }
}