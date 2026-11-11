package com.mustansir.dsatracker

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Scheduler.schedule(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun render() {
        val c = this
        val w = Store.week(c)
        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(24))
        }

        root.addView(TextView(c).apply {
            text = "Week ${w + 1} of ${Curriculum.weeks.size}"
            textSize = 14f
        })
        root.addView(TextView(c).apply {
            text = Curriculum.weeks[w]
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(c).apply {
            text = "${Store.doneCount(c)}/${Curriculum.items.size} done"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(16))
        })

        Curriculum.items.forEachIndexed { i, label ->
            root.addView(CheckBox(c).apply {
                text = label
                textSize = 18f
                isChecked = Store.checked(c, i)
                setPadding(dp(8), dp(12), 0, dp(12))
                setOnCheckedChangeListener { _, _ ->
                    Store.toggle(c, i)
                    ChecklistWidget.refresh(c)
                    render()
                }
            })
        }

        val nav = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(24), 0, 0)
        }
        nav.addView(Button(c).apply {
            text = "◀ Prev week"
            setOnClickListener { Store.setWeek(c, w - 1); ChecklistWidget.refresh(c); render() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(Button(c).apply {
            text = "Next week ▶"
            setOnClickListener { Store.setWeek(c, w + 1); ChecklistWidget.refresh(c); render() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(nav)

        val (h, m) = Store.reminder(c)
        root.addView(Button(c).apply {
            text = "Daily reminder: %02d:%02d (tap to change)".format(h, m)
            setOnClickListener {
                TimePickerDialog(c, { _, nh, nm ->
                    Store.setReminder(c, nh, nm)
                    Scheduler.schedule(c)
                    render()
                }, h, m, true).show()
            }
        })

        setContentView(ScrollView(c).apply { addView(root) })
    }
}
