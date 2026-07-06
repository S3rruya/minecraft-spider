package com.heledron.spideranimation.spider.presets

import com.heledron.spideranimation.spider.configuration.BodyPlan
import com.heledron.spideranimation.spider.configuration.SegmentPlan
import com.heledron.spideranimation.utilities.BlockDisplayModelPiece
import com.heledron.spideranimation.utilities.DisplayModel
import com.heledron.spideranimation.utilities.maths.FORWARD_VECTOR
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

object HumanoidMechModels {
    private val lit = Display.Brightness(15, 15)
    private val dim = Display.Brightness(0, 15)

    fun body(): DisplayModel {
        val pieces = mutableListOf<BlockDisplayModelPiece>()

        pieces += box(Material.NETHERITE_BLOCK, 0f, -0.58f, 0.02f, 0.95f, 0.34f, 0.50f, "pelvis", "torso")
        pieces += box(Material.BLACK_CONCRETE, 0f, -0.12f, 0.00f, 0.76f, 0.72f, 0.46f, "abdomen", "torso", "cloak")
        pieces += box(Material.BLACK_CONCRETE, 0f, 0.52f, 0.02f, 1.10f, 0.84f, 0.56f, "chest", "torso", "cloak")
        pieces += box(Material.NETHERITE_BLOCK, 0f, 0.90f, 0.00f, 1.58f, 0.26f, 0.40f, "shoulders", "torso")

        pieces += box(Material.RED_CONCRETE, -0.28f, 0.58f, 0.35f, 0.34f, 0.54f, 0.08f, "chest_plate", "torso", "cloak")
        pieces += box(Material.RED_CONCRETE, 0.28f, 0.58f, 0.35f, 0.34f, 0.54f, 0.08f, "chest_plate", "torso", "cloak")
        pieces += box(Material.PURPLE_CONCRETE, 0f, 0.53f, 0.41f, 0.18f, 0.26f, 0.08f, "reactor", "blinking_lights", brightness = lit)
        pieces += box(Material.ORANGE_CONCRETE, -0.48f, 0.80f, 0.31f, 0.12f, 0.12f, 0.08f, "blinking_lights", brightness = lit)
        pieces += box(Material.PURPLE_CONCRETE, 0.48f, 0.80f, 0.31f, 0.12f, 0.12f, 0.08f, "blinking_lights", brightness = lit)

        pieces += box(Material.NETHERITE_BLOCK, 0f, 1.08f, 0.02f, 0.26f, 0.22f, 0.26f, "neck", "torso")
        pieces += box(Material.BLACK_CONCRETE, 0f, 1.36f, 0.08f, 0.58f, 0.46f, 0.54f, "humanoid_head", "head", "cloak")
        pieces += box(Material.RED_CONCRETE, 0f, 1.20f, 0.32f, 0.46f, 0.12f, 0.16f, "humanoid_head", "jaw", "cloak")
        pieces += box(Material.PURPLE_CONCRETE, -0.14f, 1.42f, 0.39f, 0.12f, 0.10f, 0.06f, "humanoid_head", "eye", brightness = lit)
        pieces += box(Material.PURPLE_CONCRETE, 0.14f, 1.42f, 0.39f, 0.12f, 0.10f, 0.06f, "humanoid_head", "eye", brightness = lit)
        pieces += box(Material.ORANGE_CONCRETE, 0f, 1.55f, 0.34f, 0.16f, 0.06f, 0.06f, "humanoid_head", "blinking_lights", brightness = lit)

        pieces += box(Material.NETHERITE_BLOCK, 0f, 0.42f, -0.34f, 0.68f, 0.88f, 0.20f, "backpack", "torso")
        pieces += box(Material.REDSTONE_BLOCK, -0.22f, 0.42f, -0.47f, 0.12f, 0.48f, 0.08f, "back_light", "blinking_lights", brightness = lit)
        pieces += box(Material.ORANGE_CONCRETE, 0.22f, 0.42f, -0.47f, 0.12f, 0.48f, 0.08f, "back_light", "blinking_lights", brightness = lit)

        addArm(pieces, side = 1f)
        addArm(pieces, side = -1f)

        return DisplayModel(pieces)
    }

    fun legSegments(lengthScale: Double, side: Double): List<SegmentPlan> {
        val outward = side.coerceIn(-1.0, 1.0)
        return listOf(
            SegmentPlan(0.15 * lengthScale, direction(outward * 0.08, -0.45, 0.10)),
            SegmentPlan(0.63 * lengthScale, direction(outward * 0.18, -0.98, 0.12)),
            SegmentPlan(0.68 * lengthScale, direction(outward * -0.10, -0.99, -0.05)),
            SegmentPlan(0.30 * lengthScale, direction(0.0, -0.08, 0.98)),
        )
    }

    fun applyLegModels(bodyPlan: BodyPlan) {
        bodyPlan.legs.forEach { leg ->
            leg.segments.forEachIndexed { index, segment ->
                segment.model = when (index) {
                    0 -> hipModel(segment.length.toFloat())
                    1 -> thighModel(segment.length.toFloat())
                    leg.segments.size - 2 -> shinModel(segment.length.toFloat())
                    leg.segments.size - 1 -> footModel(segment.length.toFloat())
                    else -> thighModel(segment.length.toFloat())
                }
            }
        }
    }

    private fun addArm(pieces: MutableList<BlockDisplayModelPiece>, side: Float) {
        val sideTag = if (side > 0) "left" else "right"
        val shoulder = Vector3f(0.82f * side, 0.78f, 0.02f)
        val elbow = Vector3f(0.93f * side, 0.18f, 0.10f)
        val hand = Vector3f(0.90f * side, -0.48f, 0.23f)

        pieces += box(Material.NETHERITE_BLOCK, shoulder.x, shoulder.y, shoulder.z, 0.32f, 0.34f, 0.36f, "humanoid_arm", "shoulder", sideTag)
        pieces += limb(Material.BLACK_CONCRETE, shoulder, Vector3f(0.18f * side, -1f, 0.12f), 0.70f, 0.30f, 0.34f, "humanoid_arm", "upper_arm", sideTag, "cloak")
        pieces += limb(Material.RED_CONCRETE, Vector3f(shoulder.x, shoulder.y - 0.04f, shoulder.z + 0.12f), Vector3f(0.18f * side, -1f, 0.12f), 0.52f, 0.12f, 0.38f, "humanoid_arm", "upper_arm", sideTag, "cloak")
        pieces += box(Material.PURPLE_CONCRETE, elbow.x, elbow.y, elbow.z, 0.30f, 0.24f, 0.30f, "humanoid_arm", "elbow", "blinking_lights", sideTag, brightness = lit)
        pieces += limb(Material.BLACK_CONCRETE, elbow, Vector3f(-0.05f * side, -1f, 0.20f), 0.66f, 0.26f, 0.30f, "humanoid_arm", "lower_arm", sideTag, "cloak")
        pieces += limb(Material.RED_CONCRETE, Vector3f(elbow.x, elbow.y - 0.04f, elbow.z + 0.10f), Vector3f(-0.05f * side, -1f, 0.20f), 0.48f, 0.10f, 0.34f, "humanoid_arm", "lower_arm", sideTag, "cloak")
        pieces += box(Material.NETHERITE_BLOCK, hand.x, hand.y, hand.z, 0.34f, 0.28f, 0.34f, "humanoid_arm", "hand", sideTag)
    }

    private fun hipModel(length: Float): DisplayModel {
        return DisplayModel(listOf(
            limb(Material.NETHERITE_BLOCK, Vector3f(0f, 0f, 0f), FORWARD, length, 0.36f, 0.36f, "leg", "hip"),
            box(Material.PURPLE_CONCRETE, 0f, 0f, length * 0.55f, 0.22f, 0.22f, 0.12f, "leg", "hip", "blinking_lights", brightness = lit),
        ))
    }

    private fun thighModel(length: Float): DisplayModel {
        return DisplayModel(listOf(
            limb(Material.BLACK_CONCRETE, Vector3f(0f, 0f, 0f), FORWARD, length, 0.40f, 0.46f, "leg", "thigh", "cloak"),
            limb(Material.RED_CONCRETE, Vector3f(0.16f, 0f, length * 0.08f), FORWARD, length * 0.72f, 0.10f, 0.50f, "leg", "thigh", "cloak"),
            limb(Material.RED_CONCRETE, Vector3f(-0.16f, 0f, length * 0.08f), FORWARD, length * 0.72f, 0.10f, 0.50f, "leg", "thigh", "cloak"),
            box(Material.ORANGE_CONCRETE, 0f, 0f, length, 0.42f, 0.42f, 0.12f, "leg", "knee", "blinking_lights", brightness = lit),
        ))
    }

    private fun shinModel(length: Float): DisplayModel {
        return DisplayModel(listOf(
            limb(Material.BLACK_CONCRETE, Vector3f(0f, 0f, 0f), FORWARD, length, 0.34f, 0.42f, "leg", "shin", "cloak"),
            limb(Material.RED_CONCRETE, Vector3f(0f, 0.16f, length * 0.08f), FORWARD, length * 0.72f, 0.38f, 0.10f, "leg", "shin", "cloak"),
            limb(Material.NETHERITE_BLOCK, Vector3f(0f, 0f, length * 0.78f), FORWARD, length * 0.20f, 0.40f, 0.40f, "leg", "ankle"),
            box(Material.PURPLE_CONCRETE, 0f, 0f, length * 0.48f, 0.18f, 0.18f, 0.10f, "leg", "blinking_lights", brightness = lit),
        ))
    }

    private fun footModel(length: Float): DisplayModel {
        return DisplayModel(listOf(
            box(Material.BLACK_CONCRETE, 0f, -0.08f, length * 0.75f, 0.58f, 0.26f, 0.72f, "leg", "foot", "cloak"),
            box(Material.RED_CONCRETE, 0f, 0.04f, length + 0.22f, 0.52f, 0.12f, 0.28f, "leg", "toe", "cloak"),
            box(Material.NETHERITE_BLOCK, 0f, -0.20f, length * 0.55f, 0.48f, 0.10f, 0.44f, "leg", "sole"),
        ))
    }

    private fun box(
        material: Material,
        x: Float,
        y: Float,
        z: Float,
        width: Float,
        height: Float,
        depth: Float,
        vararg tags: String,
        brightness: Display.Brightness? = dim,
    ): BlockDisplayModelPiece {
        return BlockDisplayModelPiece(
            block = material.createBlockData(),
            transform = Matrix4f()
                .translate(x, y, z)
                .scale(width, height, depth)
                .translate(-0.5f, -0.5f, -0.5f),
            brightness = brightness,
            tags = tags.toList() + "humanoid_mech",
        )
    }

    private fun limb(
        material: Material,
        start: Vector3f,
        direction: Vector3f,
        length: Float,
        width: Float,
        height: Float,
        vararg tags: String,
        brightness: Display.Brightness? = dim,
    ): BlockDisplayModelPiece {
        val rotation = Quaternionf().rotationTo(FORWARD, Vector3f(direction).normalize())
        return BlockDisplayModelPiece(
            block = material.createBlockData(),
            transform = Matrix4f()
                .translate(start.x, start.y, start.z)
                .rotate(rotation)
                .scale(width, height, length)
                .translate(-0.5f, -0.5f, 0f),
            brightness = brightness,
            tags = tags.toList() + "humanoid_mech",
        )
    }

    private fun direction(x: Double, y: Double, z: Double): Vector {
        return Vector(x, y, z).normalize()
    }

    private val FORWARD = FORWARD_VECTOR.toVector3f()
}
