package com.arashrasoulzadeh.einkdash.util

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The app's "paper" look: pure black on white, hairline rules, flat outlined controls.
 * No grays, gradients or shadows, which ghost on e-ink.
 */
object Ui {
    const val INK = 0xFF000000.toInt()
    const val PAPER = 0xFFFFFFFF.toInt()

    val light: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    val bold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)

    fun dp(ctx: Context, v: Float) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun box(ctx: Context, fill: Int) = GradientDrawable().apply {
        setColor(fill)
        setStroke(dp(ctx, 1.5f), INK)
        cornerRadius = dp(ctx, 4f).toFloat()
    }

    /** White outlined button; inverts while pressed. */
    fun button(ctx: Context, text: CharSequence, onClick: () -> Unit) = Button(ctx).apply {
        this.text = text
        isAllCaps = false
        textSize = 16f
        typeface = bold
        minHeight = dp(ctx, 52f)
        minimumHeight = dp(ctx, 52f)
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), box(ctx, INK))
            addState(intArrayOf(), box(ctx, PAPER))
        }
        setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()), intArrayOf(PAPER, INK)))
        setPadding(dp(ctx, 12f), 0, dp(ctx, 12f), 0)
        setOnClickListener { onClick() }
    }

    fun button(ctx: Context, textRes: Int, onClick: () -> Unit) = button(ctx, ctx.getString(textRes), onClick)

    fun input(ctx: Context, hintRes: Int) = EditText(ctx).apply {
        setHint(hintRes)
        textSize = 18f
        setTextColor(INK)
        setHintTextColor(INK and 0x00FFFFFF or 0x88000000.toInt())
        background = box(ctx, PAPER)
        val p = dp(ctx, 12f)
        setPadding(p, p, p, p)
        minHeight = dp(ctx, 52f)
    }

    fun text(ctx: Context, sizeSp: Float, face: Typeface = Typeface.DEFAULT) = TextView(ctx).apply {
        textSize = sizeSp
        typeface = face
        setTextColor(INK)
    }

    /** Small bold uppercase label, like a newspaper section head. */
    fun label(ctx: Context, textRes: Int) = text(ctx, 12f, bold).apply {
        text = ctx.getString(textRes).uppercase(ctx.resources.configuration.locale)
    }

    fun rule(ctx: Context) = View(ctx).apply {
        setBackgroundColor(INK)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1f))
    }

    fun gap(ctx: Context, h: Float) = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(ctx, h))
    }

    /**
     * Standard secondary screen: back + title header, a rule, then content.
     * fitsSystemWindows sits on an outer frame so it doesn't eat the content padding.
     */
    fun screen(activity: Activity, titleRes: Int): LinearLayout {
        val pad = dp(activity, 20f)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(LinearLayout(activity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(text(activity, 30f, light).apply { setText(titleRes) },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(button(activity, com.arashrasoulzadeh.einkdash.R.string.back) { activity.finish() })
            })
            addView(gap(activity, 12f))
            addView(rule(activity))
            addView(gap(activity, 16f))
        }
        activity.setContentView(FrameLayout(activity).apply {
            fitsSystemWindows = true
            setBackgroundColor(PAPER)
            addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        })
        return content
    }

    fun row(ctx: Context, vararg views: View, gapDp: Float = 8f) = LinearLayout(ctx).apply {
        gravity = Gravity.CENTER_VERTICAL
        views.forEachIndexed { i, v ->
            addView(v, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i > 0) marginStart = dp(ctx, gapDp)
            })
        }
    }
}
