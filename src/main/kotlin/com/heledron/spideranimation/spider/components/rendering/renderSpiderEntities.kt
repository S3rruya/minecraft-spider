package com.heledron.spideranimation.spider.components.rendering

import com.heledron.spideranimation.utilities.rendering.interpolateTransform
import com.heledron.spideranimation.utilities.rendering.renderBlock
import com.heledron.spideranimation.spider.components.body.SpiderBody
import com.heledron.spideranimation.spider.components.Cloak
import com.heledron.spideranimation.utilities.*
import com.heledron.spideranimation.utilities.rendering.RenderGroup
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sin


fun renderSpider(spider: SpiderBody, cloak: Cloak): RenderGroup {
    val group = RenderGroup()

    val transform = Matrix4f().rotate(spider.orientation)
    group["body"] = renderModel(spider, cloak, spider.position, spider.bodyPlan.bodyModel, transform)


    for ((legIndex, leg) in spider.legs.withIndex()) {
        val chain = leg.chain

        val pivot = spider.gait.legChainPivotMode.get(spider)
        for ((segmentIndex, rotation) in chain.getRotations(pivot).withIndex()) {
            val segmentPlan = spider.bodyPlan.legs.getOrNull(legIndex)?.segments?.getOrNull(segmentIndex) ?: continue

            val parent = chain.segments.getOrNull(segmentIndex - 1)?.position ?: chain.root

            val segmentTransform = Matrix4f().rotate(rotation)
            group[legIndex to segmentIndex] = renderModel(spider, cloak, parent, segmentPlan.model, segmentTransform)

        }
    }

    return group
}

private fun renderModel(
    spider: SpiderBody,
    cloak: Cloak,
    position: Vector,
    model: DisplayModel,
    transformation: Matrix4f
): RenderGroup {
    val group = RenderGroup()

    for ((index, piece) in model.pieces.withIndex()) {
        group[index] = renderModelPiece(spider, cloak, position, piece, transformation)
    }

    return group
}


private fun renderModelPiece(
    spider: SpiderBody,
    cloak: Cloak,
    position: Vector,
    piece: BlockDisplayModelPiece,
    transformation: Matrix4f,
//    cloakID: Any
) = renderBlock(
    location = position.toLocation(spider.world),
    init = {
        it.teleportDuration = 1
        it.interpolationDuration = 1
    },
    update = {
        val transform = Matrix4f(transformation).mul(animatedModelPieceTransform(spider, piece))
        it.interpolateTransform(transform)

        val cloak = if (piece.tags.contains("cloak")) {
            val relative = transform.transform(Vector4f(.5f, .5f, .5f, 1f))
            val piecePosition = position.clone()
            piecePosition.x += relative.x
            piecePosition.y += relative.y
            piecePosition.z += relative.z

            cloak.getPiece(piece, spider.world, piecePosition, piece.block, piece.brightness)
        } else null

        if (cloak != null) {
            it.block = cloak.first
            it.brightness = cloak.second
        } else {
            it.block = piece.block
            it.brightness = piece.brightness
        }
    }
)

private fun animatedModelPieceTransform(spider: SpiderBody, piece: BlockDisplayModelPiece): Matrix4f {
    val animation = humanoidMechAnimation(spider, piece) ?: return Matrix4f(piece.transform)
    return animation.mul(piece.transform)
}

private fun humanoidMechAnimation(spider: SpiderBody, piece: BlockDisplayModelPiece): Matrix4f? {
    if (!piece.tags.contains("humanoid_mech")) return null
    val modelScale = spider.bodyPlan.scale.toFloat()

    if (piece.tags.contains("humanoid_head")) {
        val idleYaw = sin(spider.world.fullTime.toFloat() * 0.07f) * 0.04f
        return pivotRotation(0f, 1.14f * modelScale, 0.06f * modelScale)
            .rotateY(idleYaw)
            .translate(0f, -1.14f * modelScale, -0.06f * modelScale)
    }

    if (!piece.tags.contains("humanoid_arm")) return null

    val side = when {
        piece.tags.contains("left") -> 1f
        piece.tags.contains("right") -> -1f
        else -> return null
    }

    val speedFraction = if (spider.gait.maxSpeed <= 0.0) {
        0f
    } else {
        (spider.velocity.horizontalLength() / spider.gait.maxSpeed).coerceIn(0.0, 1.0).toFloat()
    }

    val movement = if (spider.isWalking) speedFraction.coerceAtLeast(0.35f) else speedFraction
    val phaseSpeed = if (spider.gallop) 0.58f else 0.38f
    val phase = sin(spider.world.fullTime.toFloat() * phaseSpeed)
    val shoulderSwing = phase * side * 0.48f * movement

    val shoulderX = 0.82f * side * modelScale
    val shoulderY = 0.78f * modelScale
    val shoulderZ = 0.02f * modelScale
    val shoulder = pivotRotation(shoulderX, shoulderY, shoulderZ)
        .rotateX(shoulderSwing)
        .translate(-shoulderX, -shoulderY, -shoulderZ)

    if (piece.tags.contains("upper_arm") || piece.tags.contains("elbow")) {
        return shoulder
    }

    if (piece.tags.contains("lower_arm") || piece.tags.contains("hand")) {
        val elbowBend = (0.12f + abs(phase) * 0.22f) * movement
        val elbowX = 0.93f * side * modelScale
        val elbowY = 0.18f * modelScale
        val elbowZ = 0.10f * modelScale
        val elbow = pivotRotation(elbowX, elbowY, elbowZ)
            .rotateX(elbowBend)
            .translate(-elbowX, -elbowY, -elbowZ)
        return shoulder.mul(elbow)
    }

    return null
}

private fun pivotRotation(x: Float, y: Float, z: Float): Matrix4f {
    return Matrix4f().translate(x, y, z)
}
