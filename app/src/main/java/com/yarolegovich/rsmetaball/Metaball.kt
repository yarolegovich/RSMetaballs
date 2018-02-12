package com.yarolegovich.rsmetaball

import kotlin.reflect.KProperty

/**
 * Created by yarolegovich on 2/11/18.
 */
interface MetaballScene {
    fun updateScene()
}

typealias DimensionTransform = (Float) -> Float

val IDENTITY: DimensionTransform = { it }

class Metaball(private var realX: Float,
               private var realY: Float,
               private val realRadius: Float) {

    val x by Transform { realX }
    val y by Transform { realY }
    val radius by Transform { realRadius }

    var transform: DimensionTransform = IDENTITY

    private var velocityX = 0f
    private var velocityY = 0f

    fun isInHorizontalBounds(start: Int, end: Int) = isInBounds(realX, start, end)

    fun isInVerticalBounds(start: Int, end: Int) = isInBounds(realY, start, end)

    private fun isInBounds(coordinate: Float, start: Int, end: Int): Boolean {
        return (coordinate - realRadius >= start) && (coordinate + realRadius <= end)
    }

    fun move() {
        realX += velocityX
        realY += velocityY
    }

    fun scaleVelocity(xScale: Int, yScale: Int) {
        velocityX *= xScale
        velocityY *= yScale
    }

    fun setVelocity(velocityX: Float, velocityY: Float) {
        this.velocityX = velocityX
        this.velocityY = velocityY
    }

    private inner class Transform(private var provider: () -> Float) {
        operator fun getValue(obj: Metaball, prop: KProperty<*>) = transform(provider())
    }
}