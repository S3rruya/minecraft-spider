package com.heledron.spideranimation.spider.presets

import com.heledron.spideranimation.spider.configuration.BodyPlan
import com.heledron.spideranimation.spider.configuration.Gait
import com.heledron.spideranimation.spider.configuration.LegPlan
import com.heledron.spideranimation.spider.configuration.SegmentPlan
import com.heledron.spideranimation.spider.configuration.SpiderOptions
import com.heledron.spideranimation.utilities.maths.FORWARD_VECTOR
import org.bukkit.Material
import org.bukkit.util.Vector


private fun equalLength(segmentCount: Int, length: Double): List<SegmentPlan> {
    return List(segmentCount) { SegmentPlan(length, FORWARD_VECTOR) }
}

private fun BodyPlan.addLegPair(root: Vector, rest: Vector, segments: List<SegmentPlan>) {
    legs += LegPlan(Vector( root.x, root.y, root.z), Vector( rest.x, rest.y, rest.z), segments)
    legs += LegPlan(Vector(-root.x, root.y, root.z), Vector(-rest.x, rest.y, rest.z), segments.map { it.clone() })
}

fun biped(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vector(.0, .0, .0), Vector(1.0, .0, .0), equalLength(segmentCount, 1.0 * segmentLength))
    applyLineLegModel(options.bodyPlan, Material.NETHERITE_BLOCK.createBlockData())
    return options
}

fun quadruped(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vector(.0, .0, .0), Vector(0.9,.0, 0.9), equalLength(segmentCount, 0.9 * segmentLength))
    options.bodyPlan.addLegPair(Vector(.0, .0, .0), Vector(1.0, .0, -1.1), equalLength(segmentCount, 1.2 * segmentLength))
    applyLineLegModel(options.bodyPlan, Material.NETHERITE_BLOCK.createBlockData())
    return options
}

fun hexapod(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vector(.0,.0,0.1), Vector(1.0,.0, 1.1), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vector(.0,.0,0.0), Vector(1.3,.0,-0.3), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vector(.0,.0,-.1), Vector(1.2,.0,-2.0), equalLength(segmentCount, 1.6 * segmentLength))
    applyLineLegModel(options.bodyPlan, Material.NETHERITE_BLOCK.createBlockData())
    return options
}

fun octopod(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.addLegPair(Vector(.0,.0,  .1), Vector(1.0, .0,  1.6), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vector(.0,.0,  .0), Vector(1.3, .0,  0.4), equalLength(segmentCount, 1.0 * segmentLength))
    options.bodyPlan.addLegPair(Vector(.0,.0, -.1), Vector(1.3, .0, -0.9), equalLength(segmentCount, 1.1 * segmentLength))
    options.bodyPlan.addLegPair(Vector(.0,.0, -.2), Vector(1.1, .0, -2.5), equalLength(segmentCount, 1.6 * segmentLength))
    applyLineLegModel(options.bodyPlan, Material.NETHERITE_BLOCK.createBlockData())
    return options
}


private fun createRobotSegments(segmentCount: Int, lengthScale: Double) = List(segmentCount) { index ->
    var length = lengthScale.toFloat()
    var initDirection = FORWARD_VECTOR

    if (index == 0) {
        length *= .5f
        initDirection = initDirection.rotateAroundX(Math.PI / 3)
    }

    if (index == 1) length *= .8f

    SegmentPlan(length.toDouble(), initDirection)
}


fun quadBot(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.bodyModel = SpiderTorsoModels.FLAT.model.clone()
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15, .2), rest = Vector(1.3 * 1.0,.0, 1.0), createRobotSegments(segmentCount, .9 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15,-.2), rest = Vector(1.3 * 1.1,.0,-1.2), createRobotSegments(segmentCount, 1.2 * .7 * segmentLength))
    applyMechanicalLegModel(options.bodyPlan)
    return options
}

fun hexBot(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.bodyModel = SpiderTorsoModels.FLAT.model.clone()
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15, .2), rest = Vector(1.3 * 1.0,.0, 1.3), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15, .0), rest = Vector(1.3 * 1.2,.0,-0.1), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15,-.2), rest = Vector(1.3 * 1.1,.0,-1.6), createRobotSegments(segmentCount, 1.3 * .7 * segmentLength))
    applyMechanicalLegModel(options.bodyPlan)
    return options
}

fun octoBot(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    options.bodyPlan.bodyModel = SpiderTorsoModels.FLAT.model.clone()
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15, .3), rest = Vector(1.3 * 1.0,.0, 1.3), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15, .1), rest = Vector(1.3 * 1.2,.0, 0.5), createRobotSegments(segmentCount, 1.0 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15, .1), rest = Vector(1.3 * 1.2,.0,-0.7), createRobotSegments(segmentCount, 1.1 * .7 * segmentLength))
    options.bodyPlan.addLegPair(root = Vector(.2,-.2 - .15,-.3), rest = Vector(1.3 * 1.1,.0,-1.6), createRobotSegments(segmentCount, 1.3 * .7 * segmentLength))
    applyMechanicalLegModel(options.bodyPlan)
    return options
}

fun mech(segmentCount: Int, segmentLength: Double): SpiderOptions {
    val options = SpiderOptions()
    val legLengthScale = segmentLength * (segmentCount.coerceAtLeast(4).toDouble() / 4.0)

    options.bodyPlan.bodyModel = HumanoidMechModels.body()
    options.bodyPlan.addLegPair(
        root = Vector(.30, -.42, .02),
        rest = Vector(.34, .0, .12),
        segments = HumanoidMechModels.legSegments(legLengthScale)
    )

    HumanoidMechModels.applyLegModels(options.bodyPlan)

    options.bodyPlan.eyePalette = AnimatedPalettes.MECH_EYES.palette
    options.bodyPlan.blinkingPalette = AnimatedPalettes.MECH_BLINKING_LIGHTS.palette

    options.walkGait = Gait.defaultBipedWalk()
    options.gallopGait = Gait.defaultBipedRun()

    options.bodyPlan.scale(1.05)

    return options
}

private fun applyHeavyMechStyle(bodyPlan: BodyPlan) {
    val blackArmor = Material.BLACK_CONCRETE.createBlockData()
    val redArmor = Material.RED_CONCRETE.createBlockData()
    val darkMetal = Material.NETHERITE_BLOCK.createBlockData()
    val purpleLight = Material.PURPLE_CONCRETE.createBlockData()
    val orangeLight = Material.ORANGE_CONCRETE.createBlockData()

    bodyPlan.bodyModel.pieces.forEachIndexed { index, piece ->
        piece.block = when {
            piece.tags.contains("eye") -> purpleLight
            piece.tags.contains("blinking_lights") -> if (index % 2 == 0) purpleLight else orangeLight
            piece.tags.contains("cloak") -> if (index % 3 == 0) redArmor else blackArmor
            piece.tags.contains("torso") && index % 5 == 0 -> redArmor
            else -> piece.block
        }
    }

    bodyPlan.legs.forEachIndexed { legIndex, leg ->
        leg.segments.forEachIndexed { segmentIndex, segment ->
            segment.model.scale(1.45f, 1.45f, 1.0f)
            segment.model.pieces.forEachIndexed { pieceIndex, piece ->
                piece.block = when {
                    piece.tags.contains("cloak") -> if ((legIndex + segmentIndex + pieceIndex) % 2 == 0) redArmor else blackArmor
                    piece.tags.contains("tip") -> darkMetal
                    piece.tags.contains("base") -> darkMetal
                    piece.tags.contains("leg") && (segmentIndex + pieceIndex) % 4 == 0 -> redArmor
                    else -> piece.block
                }
            }
        }
    }
}
