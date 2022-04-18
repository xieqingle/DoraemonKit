package com.didichuxing.doraemonkit.kit.test.event.monitor;

import android.os.Build
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import com.didichuxing.doraemonkit.extension.tagName
import com.didichuxing.doraemonkit.kit.core.DokitFrameLayout
import com.didichuxing.doraemonkit.kit.test.DoKitTestManager
import com.didichuxing.doraemonkit.kit.test.event.ControlEventManager
import com.didichuxing.doraemonkit.kit.test.util.XposedHookUtils
import com.didichuxing.doraemonkit.kit.test.event.ControlEvent
import com.didichuxing.doraemonkit.kit.test.event.AccessibilityEventNode
import com.didichuxing.doraemonkit.kit.test.event.DoKitViewNode
import com.didichuxing.doraemonkit.kit.test.event.ViewC12c
import com.didichuxing.doraemonkit.kit.test.event.EventType
import com.didichuxing.doraemonkit.kit.test.util.ViewPathUtil
import com.didichuxing.doraemonkit.kit.test.util.WindowPathUtil
import com.didichuxing.doraemonkit.util.ConvertUtils
import com.didichuxing.doraemonkit.util.LogHelper

/**
 * didi Create on 2022/2/22 .
 * <p>
 * Copyright (c) 2022/2/22 by didiglobal.com.
 *
 * @author <a href="realonlyone@126.com">zhangjun</a>
 * @version 1.0
 * @Date 2022/2/22 6:06 下午
 * @Description 用一句话说明文件功能
 */

object AccessibilityEventMonitor {

    const val TAG = "AccessibilityEventHandler"

    /**
     * 通用的ws信息处理
     */
    fun onAccessibilityEvent(view: View, event: AccessibilityEvent) {
        if (!DoKitTestManager.isHostMode()) {
            return
        }
        when (event.eventType) {
            //点击事件只响应给需要处理的控件
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (view.hasOnClickListeners() || view.parent is AdapterView<*> || view is Button) {
                    onViewHandleEvent(view, event)
                }
            }
            //针对dokit悬浮窗
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (view is DokitFrameLayout) {
                    onViewHandleEvent(view, event)
                }
            }
            /**
             * view 获取焦点
             */
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
                //针对 EditText 文字改变
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                // represents the event of scrolling a view
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
                // represents the event of long clicking on a View like Button, CompoundButton
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                // represents the event of changing the text selection of an EditText
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                onViewHandleEvent(view, event)
            }

            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                LogHelper.i(TAG, "TYPE_VIEW_SELECTED ,class=${view.javaClass},view=$view")
            }
            else -> {
                LogHelper.e(TAG, "type=${event.eventType},class=${view.javaClass},view=$view")
            }
        }
    }

    private fun onViewHandleEvent(view: View, accessibilityEvent: AccessibilityEvent) {

        val activity = ViewPathUtil.getActivity(view)
        val actionId = ControlEventManager.createNextEventId()
        val viewC12c: ViewC12c = createViewC12c(view, accessibilityEvent)
        val controlEvent = ControlEvent(
            actionId,
            EventType.WSE_COMMON_EVENT,
            mutableMapOf(
                "activityName" to activity.tagName
            ),
            viewC12c
        )
        ControlEventManager.onControlEventAction(activity, view, controlEvent)

    }


    private fun createViewC12c(view: View, acc: AccessibilityEvent): ViewC12c {
        var viewRootImplIndex: Int = -1
        var viewParents = WindowPathUtil.filterViewRoot(XposedHookUtils.ROOT_VIEWS);
        viewParents?.let {
            viewRootImplIndex = if (view.rootView.parent == null) {
                it.size - 1
            } else {
                it.indexOf(view.rootView.parent)
            }
        }
        return ViewC12c(
            accEventType = acc.eventType,
            windowIndex = viewRootImplIndex,
            viewPaths = ViewPathUtil.createViewPathOfWindow(view),
            accEventInfo = transformAccEventInfo(acc),
            text = if (view is TextView) {
                view.text.toString()
            } else {
                ""
            },
            doKitViewNode = createDoKitViewInfo(view)
        )
    }


    /**
     * 创建dokitview info
     */
    private fun createDoKitViewInfo(view: View): DoKitViewNode? {
        if (view !is DokitFrameLayout) {
            return null
        }

        if (view.layoutParams !is FrameLayout.LayoutParams) {
            return null
        }

        return DoKitViewNode(
            (view.layoutParams as FrameLayout.LayoutParams).leftMargin,
            (view.layoutParams as FrameLayout.LayoutParams).topMargin
        )

    }

    private fun transformAccEventInfo(acc: AccessibilityEvent): AccessibilityEventNode {
        return AccessibilityEventNode(
            acc.eventType,
            acc.className?.toString(),
            acc.packageName?.toString(),
            acc.eventTime,
            acc.beforeText?.toString(),
            acc.fromIndex,
            acc.addedCount,
            acc.removedCount,
            acc.movementGranularity,
            acc.toIndex,
            acc.action,
            ConvertUtils.px2dp(acc.maxScrollX.toFloat()),
            ConvertUtils.px2dp(acc.maxScrollY.toFloat()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ConvertUtils.px2dp(acc.scrollDeltaX.toFloat())
            } else {
                -1
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ConvertUtils.px2dp(acc.scrollDeltaY.toFloat())
            } else {
                -1
            },
            ConvertUtils.px2dp(acc.scrollX.toFloat()),
            ConvertUtils.px2dp(acc.scrollY.toFloat()),
            acc.isScrollable,
            acc.contentChangeTypes,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                acc.windowChanges
            } else {
                -1
            }
        )
    }
}
