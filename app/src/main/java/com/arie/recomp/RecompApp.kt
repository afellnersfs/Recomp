package com.arie.recomp

import android.app.Application
import com.arie.recomp.data.Graph
import com.arie.recomp.notifications.Channels
import com.arie.recomp.notifications.ReminderScheduler
import com.arie.recomp.widgets.WidgetUpdateWorker

class RecompApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Channels.ensure(this)
        ReminderScheduler.scheduleAllAsync(this)
        WidgetUpdateWorker.schedule(this)
    }
}
