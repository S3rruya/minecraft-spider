package com.heledron.spideranimation.spider.configuration

import com.heledron.spideranimation.spider.components.body.GaitType
import com.heledron.spideranimation.spider.components.body.SpiderBody
import com.heledron.spideranimation.utilities.SplitDistance
import com.heledron.spideranimation.utilities.maths.lerp
import com.heledron.spideranimation.utilities.maths.toRadians
import org.joml.Quaternionf


class LerpGait(
    var bodyHeight: Double,
    var triggerZone: SplitDistance
) {
    fun scale(scale: Double): LerpGait {
        bodyHeight *= scale
        triggerZone = triggerZone.scale(scale)
        return this
    }

    fun clone() = LerpGait(
        bodyHeight = bodyHeight,
        triggerZone = triggerZone
    )

    fun lerp(target: LerpGait, factor: Double): LerpGait {
        this.bodyHeight = bodyHeight.lerp(target.bodyHeight, factor)
        this.triggerZone = triggerZone.lerp(target.triggerZone, factor)
        return this
    }
}


class Gait(
    walkSpeed: Double,
    val type: GaitType,
) {
    companion object {
        fun defaultWalk() = Gait(.15, GaitType.WALK)

        fun defaultGallop() = Gait(.4, GaitType.GALLOP).apply {
            moving.bodyHeight = 1.6
            legMoveSpeed = .5
            rotateAcceleration = .25f / 4
            uncomfortableSpeedMultiplier = .6
            samePairCooldown = 2
            crossPairCooldown = 4
            polygonLeeway = .5
        }

        fun defaultBipedWalk() = Gait(.11, GaitType.BIPED).apply {
            stationary.bodyHeight = 2.2
            stationary.triggerZone = SplitDistance(.32, 2.2)
            moving.bodyHeight = 2.12
            moving.triggerZone = SplitDistance(.72, 2.25)
            maxBodyDistanceFromGround = .55
            moveAcceleration = .15 / 6
            legMoveSpeed = .24
            legLiftHeight = .34
            legDropDistance = .20
            comfortZone = SplitDistance(1.05, 2.45)
            bodyHeightCorrectionAcceleration = gravityAcceleration * 7
            bodyHeightCorrectionFactor = .4
            legLookAheadFraction = .65
            samePairCooldown = 4
            crossPairCooldown = 6
            disableAdvancedRotation = true
            scanPivotMode = PivotMode.YAxis
            legChainPivotMode = PivotMode.YAxis
            legStraightenRotation = (-2f).toRadians()
            polygonLeeway = .65
            stabilizationFactor = .75
        }

        fun defaultBipedRun() = Gait(.28, GaitType.BIPED).apply {
            stationary.bodyHeight = 2.2
            stationary.triggerZone = SplitDistance(.32, 2.2)
            moving.bodyHeight = 2.25
            moving.triggerZone = SplitDistance(.92, 2.35)
            maxBodyDistanceFromGround = .6
            moveAcceleration = .15 / 5
            legMoveSpeed = .34
            legLiftHeight = .42
            legDropDistance = .24
            comfortZone = SplitDistance(1.25, 2.55)
            bodyHeightCorrectionAcceleration = gravityAcceleration * 8
            bodyHeightCorrectionFactor = .45
            legLookAheadFraction = .8
            samePairCooldown = 3
            crossPairCooldown = 5
            disableAdvancedRotation = true
            scanPivotMode = PivotMode.YAxis
            legChainPivotMode = PivotMode.YAxis
            legStraightenRotation = (-2f).toRadians()
            polygonLeeway = .75
            stabilizationFactor = .8
            uncomfortableSpeedMultiplier = .45
        }
    }

    fun scale(scale: Double) {
        stationary.scale(scale)
        moving.scale(scale)
        maxBodyDistanceFromGround *= scale
        maxSpeed *= scale
        moveAcceleration *= scale
        legMoveSpeed *= scale
        legLiftHeight *= scale
        legDropDistance *= scale
        comfortZone = comfortZone.scale(scale)
        legScanHeightBias *= scale
        tridentRotationalKnockBack /= scale
    }

    var stationary = LerpGait(
        bodyHeight = 1.1,
        triggerZone = SplitDistance(.25, 1.5)
    )

    var moving = LerpGait(
        bodyHeight = 1.1,
        triggerZone = SplitDistance(.8,1.5)
    )

    var maxBodyDistanceFromGround = .25

    var maxSpeed = walkSpeed
    var moveAcceleration = .15 / 4

    var rotateAcceleration = .15f / 4
    var rotationalDragCoefficient = .2f

    var legMoveSpeed = walkSpeed * 2.5

    var legLiftHeight = .35
    var legDropDistance = legLiftHeight

    var comfortZone = SplitDistance(1.2, 1.6)

    var gravityAcceleration = .08
    var airDragCoefficient = .02
    var bounceFactor = .5

    var bodyHeightCorrectionAcceleration = gravityAcceleration * 4
    var bodyHeightCorrectionFactor = .25

    var legScanAlternativeGround = true
    var legScanHeightBias = .5

    var tridentKnockBack = .3
    var tridentRotationalKnockBack = tridentKnockBack / 4
    var legLookAheadFraction = .6
    var groundDragCoefficient = .2

    var samePairCooldown = 1
    var crossPairCooldown = 1

    var useLegacyNormalForce = false
    var polygonLeeway = .0
    
    // TODO: Consider removing this
    var stabilizationFactor = .0 //0.7

    var uncomfortableSpeedMultiplier = 0.0

    var disableAdvancedRotation = false
    var preferredPitchLeeway = 10f.toRadians()

    var straightenLegs = true
    var legStraightenRotation = (-80f).toRadians()

    var scanPivotMode = PivotMode.YAxis
    var legChainPivotMode = PivotMode.SpiderOrientation

    var preferLevelBreakpoint = 45f.toRadians()
    var preferLevelBias = .0f //.2f
    var preferredRotationLerpFraction = .3f

    var rotationLerp = .3f
}


enum class PivotMode(val get: (spider: SpiderBody) -> Quaternionf) {
    YAxis({ spider -> Quaternionf().rotateY(spider.orientation.getEulerAnglesYXZ(org.joml.Vector3f()).y) }),
    SpiderOrientation({ spider -> spider.orientation }),
    GroundOrientation({ spider -> spider.preferredOrientation })
}
