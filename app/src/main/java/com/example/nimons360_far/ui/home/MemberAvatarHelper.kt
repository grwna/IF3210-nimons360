package com.example.nimons360_far.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.models.Member

object MemberAvatarHelper {

    private val colors = listOf(
        "#1E88E5", "#43A047", "#E53935", "#FB8C00", "#8E24AA", "#00ACC1"
    )

    fun populateAvatarStack(context: Context, container: LinearLayout, members: List<Member>?, maxVisible: Int = 3) {
        container.removeAllViews()
        if (members == null) return

        val visibleMembers = members.take(maxVisible)
        val remainingCount = members.size - maxVisible

        visibleMembers.forEachIndexed { index, member ->
            val avatarView = createAvatarView(context, getInitials(member.fullName), colors[index % colors.size])
            val params = LinearLayout.LayoutParams(
                dpToPx(context, 24),
                dpToPx(context, 24)
            )
            if (index > 0) {
                params.marginStart = dpToPx(context, -8)
            }
            container.addView(avatarView, params)
        }

        if (remainingCount > 0) {
            val moreView = createAvatarView(context, "+$remainingCount", "#E0E0E0", textColor = "#666666")
            val params = LinearLayout.LayoutParams(
                dpToPx(context, 24),
                dpToPx(context, 24)
            )
            params.marginStart = dpToPx(context, -8)
            container.addView(moreView, params)
        }
    }

    private fun getInitials(name: String): String {
        return name.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(1)
            .map { it[0].uppercaseChar() }
            .joinToString("")
    }

    private fun createAvatarView(context: Context, text: String, bgColor: String, textColor: String = "#FFFFFF"): TextView {
        return TextView(context).apply {
            this.text = text
            this.textSize = 10f
            this.setTextColor(Color.parseColor(textColor))
            this.setTypeface(null, Typeface.BOLD)
            this.gravity = Gravity.CENTER
            this.background = context.getDrawable(R.drawable.shape_rounded_corner_med)?.apply {
                setTint(Color.parseColor(bgColor))
            }
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
