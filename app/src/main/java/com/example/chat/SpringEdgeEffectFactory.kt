package com.example.chat

import android.content.Context
import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView

class SpringEdgeEffectFactory(private val context: Context) : RecyclerView.EdgeEffectFactory() {
    override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
        return object : EdgeEffect(context) {
            private var translationAnim: SpringAnimation? = null

            override fun onPull(deltaDistance: Float, displacement: Float) {
                super.onPull(deltaDistance, displacement)
                if (translationAnim == null) {
                    translationAnim = SpringAnimation(view, SpringAnimation.TRANSLATION_Y).apply {
                        spring = SpringForce().apply {
                            stiffness = SpringForce.STIFFNESS_LOW
                            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        }
                    }
                }
                view.translationY += deltaDistance * view.height
                translationAnim?.cancel()
            }

            override fun onRelease() {
                super.onRelease()
                translationAnim?.apply {
                    setStartValue(view.translationY)
                    setMinValue(0f)
                    setMaxValue(view.height.toFloat())
                    if (view.translationY > view.height) {
                        view.translationY = view.height.toFloat()
                    }
                    start()
                }
            }

            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)
                if (translationAnim == null) {
                    translationAnim = SpringAnimation(view, SpringAnimation.TRANSLATION_Y).apply {
                        spring = SpringForce().apply {
                            stiffness = SpringForce.STIFFNESS_LOW
                            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        }
                    }
                }
                translationAnim?.apply {
                    setStartValue(view.translationY)
                    setMinValue(0f)
                    setMaxValue(view.height.toFloat())
                    if (view.translationY > view.height) {
                        view.translationY = view.height.toFloat()
                    }
                    setStartVelocity(velocity.toFloat())
                    start()
                }
            }

            override fun draw(canvas: Canvas): Boolean {
                return super.draw(canvas)
            }
        }
    }
}