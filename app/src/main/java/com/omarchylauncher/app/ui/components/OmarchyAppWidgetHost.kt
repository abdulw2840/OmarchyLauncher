package com.omarchylauncher.app.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.hypot

// [EXP-WIDGET-LONGPRESS] Custom AppWidgetHost + HostView so holding anywhere on a widget opens customization while normal taps pass through to the widget.
class OmarchyAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

    private var downX = 0f
    private var downY = 0f
    private var hasPerformedLongPress = false

    private val longPressRunnable = Runnable {
        hasPerformedLongPress = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                hasPerformedLongPress = false
                handler.removeCallbacks(longPressRunnable)
                handler.postDelayed(longPressRunnable, longPressTimeout)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (hypot(ev.x - downX, ev.y - downY) > touchSlop) {
                    handler.removeCallbacks(longPressRunnable)
                }
                return hasPerformedLongPress
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (hasPerformedLongPress) {
                    hasPerformedLongPress = false
                    return true
                }
                return false
            }
        }
        return false
    }
}

class OmarchyAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = OmarchyAppWidgetHostView(context)
}
