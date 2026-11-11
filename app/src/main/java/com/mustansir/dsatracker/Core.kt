package com.mustansir.dsatracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Curriculum {
    // Edit these lists to change the plan. The widget shows exactly 5 items.
    val weeks = listOf(
        "Arrays + hashing",
        "Two pointers + sliding window",
        "Stack + queue",
        "Binary search + sorting",
        "Linked lists",
        "Recursion + backtracking",
        "Trees I (traversals, BST)",
        "Trees II (harder problems)",
        "Heaps / priority queues",
        "Graphs I (BFS, DFS)",
        "Graphs II (topo sort, union-find, shortest paths)",
        "Greedy + intervals",
        "DP I (1D)",
        "DP II (2D)",
        "DP III (harder patterns)",
        "Tries, bit manipulation, math"
    )
    val items = listOf(
        "Watch concept video(s)",
        "Solve problem 1",
        "Solve problem 2",
        "Solve problem 3",
        "Write one-line note"
    )
}

object Store {
    private fun p(c: Context) = c.getSharedPreferences("dsa", Context.MODE_PRIVATE)

    fun week(c: Context): Int = p(c).getInt("week", 0)

    fun setWeek(c: Context, w: Int) {
        p(c).edit().putInt("week", w.coerceIn(0, Curriculum.weeks.lastIndex)).apply()
    }

    fun checked(c: Context, i: Int): Boolean = p(c).getBoolean("w${week(c)}_$i", false)

    fun toggle(c: Context, i: Int) {
        val k = "w${week(c)}_$i"
        p(c).edit().putBoolean(k, !p(c).getBoolean(k, false)).apply()
    }

    fun doneCount(c: Context): Int = Curriculum.items.indices.count { checked(c, it) }

    fun reminder(c: Context): Pair<Int, Int> = p(c).getInt("rh", 21) to p(c).getInt("rm", 0)

    fun setReminder(c: Context, h: Int, m: Int) {
        p(c).edit().putInt("rh", h).putInt("rm", m).apply()
    }
}

object Scheduler {
    fun schedule(c: Context) {
        val (h, m) = Store.reminder(c)
        val now = Calendar.getInstance()
        val next = Calendar.getInstance()
        next.set(Calendar.HOUR_OF_DAY, h)
        next.set(Calendar.MINUTE, m)
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        if (next.before(now)) next.add(Calendar.DAY_OF_YEAR, 1)
        val delay = next.timeInMillis - now.timeInMillis
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(c)
            .enqueueUniquePeriodicWork("daily_reminder", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val c = applicationContext
        val done = Store.doneCount(c)
        val total = Curriculum.items.size
        if (done < total) {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel("daily", "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val pi = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            val n = Notification.Builder(c, "daily")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("DSA: ${Curriculum.weeks[Store.week(c)]}")
                .setContentText("$done/$total done this week. Do today's bit.")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(1, n)
        }
        return Result.success()
    }
}
