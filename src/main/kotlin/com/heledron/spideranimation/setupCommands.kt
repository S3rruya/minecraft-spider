package com.heledron.spideranimation

import com.google.gson.Gson
import com.heledron.spideranimation.spider.components.Cloak
import com.heledron.spideranimation.spider.components.SoundsAndParticles
import com.heledron.spideranimation.spider.components.body.SpiderBody
import com.heledron.spideranimation.spider.components.splay
import com.heledron.spideranimation.spider.configuration.CloakOptions
import com.heledron.spideranimation.spider.configuration.SoundOptions
import com.heledron.spideranimation.spider.configuration.SoundPlayer
import com.heledron.spideranimation.spider.configuration.SpiderDebugOptions
import com.heledron.spideranimation.spider.configuration.SpiderOptions
import com.heledron.spideranimation.spider.presets.AnimatedPalettes
import com.heledron.spideranimation.spider.presets.SpiderTorsoModels
import com.heledron.spideranimation.spider.presets.applyEmptyLegModel
import com.heledron.spideranimation.spider.presets.applyLineLegModel
import com.heledron.spideranimation.spider.presets.applyMechanicalLegModel
import com.heledron.spideranimation.spider.presets.biped
import com.heledron.spideranimation.spider.presets.hexBot
import com.heledron.spideranimation.spider.presets.hexapod
import com.heledron.spideranimation.spider.presets.mech
import com.heledron.spideranimation.spider.presets.octoBot
import com.heledron.spideranimation.spider.presets.octopod
import com.heledron.spideranimation.spider.presets.quadBot
import com.heledron.spideranimation.spider.presets.quadruped
import com.heledron.spideranimation.utilities.BlockDisplayModelPiece
import com.heledron.spideranimation.utilities.Serializer
import com.heledron.spideranimation.utilities.custom_items.setupCustomItemCommand
import com.heledron.spideranimation.utilities.ecs.ECSEntity
import com.heledron.spideranimation.utilities.events.runLater
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.block.data.BlockData
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Display
import org.bukkit.entity.Player

private data class CommandOptionTarget(
    val current: () -> Any,
    val default: () -> Any,
)

fun setupCommands(plugin: SpiderAnimationPlugin) {
    fun getCommand(name: String) = plugin.getCommand(name) ?: throw Exception("Command $name not found")

    fun matching(options: Iterable<String>, args: Array<out String>): List<String> {
        val query = args.lastOrNull() ?: ""
        return options.distinct().filter { it.contains(query, ignoreCase = true) }.take(120)
    }

    fun requireSpiderForSender(sender: CommandSender): Pair<ECSEntity, SpiderBody>? {
        val senderLocation = locationFromSender(sender) ?: AppState.target ?: run {
            sender.sendMessage("No location found")
            return null
        }

        return findSpiderForSender(sender, senderLocation) ?: run {
            sender.sendMessage("No spider found")
            null
        }
    }

    fun findSpiderForCompleter(sender: CommandSender): Pair<ECSEntity, SpiderBody>? {
        val senderLocation = locationFromSender(sender) ?: AppState.target ?: return null
        return findSpiderForSender(sender, senderLocation)
    }

    fun matchMaterial(name: String): Material? {
        return Material.matchMaterial(name) ?: Material.matchMaterial(name.substringAfter("minecraft:"))
    }

    fun matchBlockData(name: String): BlockData? {
        val material = matchMaterial(name) ?: return null
        if (!material.isBlock) return null
        return material.createBlockData()
    }

    fun blockMaterialNames(): List<String> {
        return Material.entries.filter { it.isBlock }.map { it.key.toString() }
    }

    fun soundNames(): List<String> {
        return Registry.SOUNDS.map { it.key.toString() }
    }

    fun optionTargets(entity: ECSEntity, spider: SpiderBody): Map<String, CommandOptionTarget> {
        val scale = spider.bodyPlan.scale
        val targets = linkedMapOf(
            "walkGait" to CommandOptionTarget(
                current = { spider.walkGait },
                default = { SpiderOptions().walkGait.apply { scale(scale) } },
            ),
            "gallopGait" to CommandOptionTarget(
                current = { spider.gallopGait },
                default = { SpiderOptions().gallopGait.apply { scale(scale) } },
            ),
            "debug" to CommandOptionTarget(
                current = { spider.debug },
                default = { SpiderDebugOptions() },
            ),
            "misc" to CommandOptionTarget(
                current = { AppState.miscOptions },
                default = { MiscellaneousOptions() },
            ),
        )

        entity.query<Cloak>()?.let { cloak ->
            targets["cloak"] = CommandOptionTarget(
                current = { cloak.options },
                default = { CloakOptions() },
            )
        }

        entity.query<SoundsAndParticles>()?.let { sounds ->
            targets["sound"] = CommandOptionTarget(
                current = { sounds.options },
                default = { SoundOptions() },
            )
        }

        return targets
    }

    fun sampleOptionObject(name: String): Any? {
        return when (name) {
            "walkGait" -> SpiderOptions().walkGait
            "gallopGait" -> SpiderOptions().gallopGait
            "debug" -> SpiderDebugOptions()
            "cloak" -> CloakOptions()
            "sound" -> SoundOptions()
            "misc" -> MiscellaneousOptions()
            else -> null
        }
    }

    fun parseSound(soundString: String): Sound? {
        val soundID = NamespacedKey.fromString(soundString) ?: return null
        return Registry.SOUNDS.get(soundID)
    }

    fun setStepSound(
        options: SoundOptions,
        sound: Sound = options.step.sound,
        volume: Float = options.step.volume,
        pitch: Float = options.step.pitch,
        volumeVary: Float = options.step.volumeVary,
        pitchVary: Float = options.step.pitchVary,
    ) {
        options.step = SoundPlayer(
            sound = sound,
            volume = volume,
            pitch = pitch,
            volumeVary = volumeVary,
            pitchVary = pitchVary,
        )
    }

    fun getSoundOption(options: SoundOptions, option: String): Any? {
        return when (option) {
            "step.sound" -> options.step.sound.key.toString()
            "step.volume" -> options.step.volume
            "step.pitch" -> options.step.pitch
            "step.volumeVary" -> options.step.volumeVary
            "step.pitchVary" -> options.step.pitchVary
            else -> null
        }
    }

    fun setSoundOption(options: SoundOptions, option: String, value: String, defaults: SoundOptions = SoundOptions()): Boolean {
        if (value == "reset") {
            when (option) {
                "step.sound" -> setStepSound(options, sound = defaults.step.sound)
                "step.volume" -> setStepSound(options, volume = defaults.step.volume)
                "step.pitch" -> setStepSound(options, pitch = defaults.step.pitch)
                "step.volumeVary" -> setStepSound(options, volumeVary = defaults.step.volumeVary)
                "step.pitchVary" -> setStepSound(options, pitchVary = defaults.step.pitchVary)
                else -> return false
            }
            return true
        }

        when (option) {
            "step.sound" -> setStepSound(options, sound = parseSound(value) ?: return false)
            "step.volume" -> setStepSound(options, volume = value.toFloatOrNull() ?: return false)
            "step.pitch" -> setStepSound(options, pitch = value.toFloatOrNull() ?: return false)
            "step.volumeVary" -> setStepSound(options, volumeVary = value.toFloatOrNull() ?: return false)
            "step.pitchVary" -> setStepSound(options, pitchVary = value.toFloatOrNull() ?: return false)
            else -> return false
        }
        return true
    }

    fun collectOptionKeys(obj: Any?, output: MutableList<String>, prefix: String = "") {
        if (obj == null) return
        if (prefix.isNotEmpty()) output += prefix

        when (obj) {
            is Map<*, *> -> {
                for ((key, value) in obj) {
                    val path = if (prefix.isEmpty()) key.toString() else "$prefix.$key"
                    collectOptionKeys(value, output, path)
                }
            }
            is List<*> -> {
                for ((index, value) in obj.withIndex()) {
                    collectOptionKeys(value, output, "$prefix[$index]")
                }
            }
        }
    }

    fun modelPieces(spider: SpiderBody): List<BlockDisplayModelPiece> {
        return spider.bodyPlan.bodyModel.pieces + spider.bodyPlan.legs.flatMap { leg ->
            leg.segments.flatMap { segment -> segment.model.pieces }
        }
    }

    fun modelTags(spider: SpiderBody?): List<String> {
        val fallback = listOf("cloak", "torso", "leg", "base", "femur", "tibia", "tip", "eye", "blinking_lights", "humanoid_mech")
        return spider?.let { modelPieces(it).flatMap { piece -> piece.tags }.distinct() }?.ifEmpty { fallback } ?: fallback
    }

    getCommand("options").apply {
        val optionNames = listOf("walkGait", "gallopGait", "debug", "cloak", "sound", "misc")

        setExecutor { sender, _, _, args ->
            val (entity, spider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val targets = optionTargets(entity, spider)

            val targetName = args.getOrNull(0) ?: return@setExecutor false
            val target = targets[targetName] ?: run {
                sender.sendMessage("Invalid option target: $targetName")
                return@setExecutor true
            }

            val option = args.getOrNull(1) ?: return@setExecutor false
            val valueUnparsed = args.getOrNull(2)
            val obj = target.current()

            fun printable(value: Any?) = Gson().toJson(value)

            if (targetName == "sound" && obj is SoundOptions) {
                if (option == "reset") {
                    obj.step = SoundOptions().step
                    sender.sendMessage("Reset sound options")
                    return@setExecutor true
                }

                if (valueUnparsed == null) {
                    sender.sendMessage("Option sound.$option is ${printable(getSoundOption(obj, option))}")
                    return@setExecutor true
                }

                if (!setSoundOption(obj, option, valueUnparsed)) {
                    sender.sendMessage("Could not set sound.$option to $valueUnparsed")
                    return@setExecutor true
                }

                sender.sendMessage("Set sound.$option to ${printable(getSoundOption(obj, option))}")
                return@setExecutor true
            }

            if (option == "reset") {
                val map = Serializer.toMap(target.default()) as Map<*, *>
                Serializer.writeFromMap(obj, map)
                sender.sendMessage("Reset $targetName options")
                return@setExecutor true
            }

            if (valueUnparsed == null) {
                sender.sendMessage("Option $targetName.$option is ${printable(Serializer.get(obj, option))}")
                return@setExecutor true
            }

            if (valueUnparsed == "reset") {
                val value = Serializer.get(target.default(), option)
                Serializer.set(obj, option, value)
                sender.sendMessage("Reset $targetName.$option to ${printable(value)}")
                return@setExecutor true
            }

            val parsed = try {
                Gson().fromJson(valueUnparsed, Any::class.java)
            } catch (e: Exception) {
                sender.sendMessage("Could not parse: $valueUnparsed")
                return@setExecutor true
            }

            try {
                Serializer.setMap(obj, option, parsed)
            } catch (e: Exception) {
                sender.sendMessage("Could not set $targetName.$option: ${e.message ?: e.javaClass.simpleName}")
                return@setExecutor true
            }

            sender.sendMessage("Set $targetName.$option to ${printable(Serializer.get(obj, option))}")
            plugin.writeAndSaveConfig()
            true
        }

        setTabCompleter { sender, _, _, args ->
            if (args.size == 1) return@setTabCompleter matching(optionNames, args)

            if (args.getOrNull(0) == "sound") {
                if (args.size == 2) {
                    return@setTabCompleter matching(
                        listOf("reset", "step.sound", "step.volume", "step.pitch", "step.volumeVary", "step.pitchVary"),
                        args,
                    )
                }

                val suggestions = when (args.getOrNull(1)) {
                    "step.sound" -> soundNames()
                    "step.volume" -> listOf("0", "0.1", "0.3", "0.5", "1", "2", "reset")
                    "step.pitch" -> listOf("0.5", "0.75", "1", "1.25", "1.5", "2", "reset")
                    "step.volumeVary", "step.pitchVary" -> listOf("0", "0.05", "0.1", "0.2", "0.5", "1", "reset")
                    else -> listOf("reset")
                }
                return@setTabCompleter matching(suggestions, args)
            }

            val sample = if (args.size >= 2) {
                val targetName = args[0]
                val actual = findSpiderForCompleter(sender)
                    ?.let { (entity, spider) -> optionTargets(entity, spider)[targetName]?.current?.invoke() }
                actual ?: sampleOptionObject(targetName)
            } else null

            if (args.size == 2) {
                val keys = mutableListOf("reset")
                collectOptionKeys(Serializer.toNullableMap(sample), keys)
                return@setTabCompleter matching(keys, args)
            }

            val current = sample?.let { Serializer.get(it, args[1]) }
            val suggestions = when (current) {
                is Boolean -> listOf("true", "false", "reset")
                is Number -> listOf(current.toString(), "0", "1", "reset")
                else -> listOf("reset")
            }
            matching(suggestions, args)
        }
    }

    getCommand("modify_model").apply {
        val clauses = listOf("or", "set_block", "copy_block", "brightness", "scale")

        setExecutor { sender, _, _, args ->
            val (_, spider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val pieces = modelPieces(spider)
            val changes = mutableListOf<(BlockDisplayModelPiece) -> Unit>()
            val orGroups = mutableListOf<MutableList<(BlockDisplayModelPiece) -> Boolean>>(mutableListOf())

            var clause = "or"
            for ((index, arg) in args.withIndex()) {
                if (arg == "or") {
                    clause = "or"
                    orGroups.add(mutableListOf())
                    continue
                }

                if (arg == "set_block") {
                    clause = "set_block"
                    val palette = mutableListOf<BlockData>()
                    var cursor = index + 1
                    while (cursor < args.size) {
                        if (args[cursor] in clauses) break
                        val block = matchBlockData(args[cursor]) ?: break
                        palette += block
                        cursor++
                    }

                    if (palette.isEmpty()) {
                        sender.sendMessage("Usage: /spider:modify_model <selector> set_block <block> [block...]")
                        return@setExecutor true
                    }

                    changes += { piece -> piece.block = palette.random() }
                    continue
                }

                if (arg == "copy_block") {
                    clause = "copy_block"
                    val location = locationFromSender(sender) ?: run {
                        sender.sendMessage("Cannot copy block without location")
                        return@setExecutor true
                    }

                    fun resolveCoord(string: String, baseLocation: Double): Double {
                        if (string.startsWith("~")) {
                            val offset = string.substring(1).toDoubleOrNull() ?: 0.0
                            return baseLocation + offset
                        }

                        return string.toDoubleOrNull() ?: baseLocation
                    }

                    val x = resolveCoord(args.getOrNull(index + 1) ?: "~0", location.x)
                    val y = resolveCoord(args.getOrNull(index + 2) ?: "~0", location.y)
                    val z = resolveCoord(args.getOrNull(index + 3) ?: "~0", location.z)
                    val block = location.world!!.getBlockAt(x.toInt(), y.toInt(), z.toInt()).blockData

                    changes += { piece -> piece.block = block }
                    continue
                }

                if (arg == "brightness") {
                    clause = "brightness"
                    val blockLight = args.getOrNull(index + 1)?.toIntOrNull()?.coerceIn(0, 15) ?: 0
                    val skyLight = args.getOrNull(index + 2)?.toIntOrNull()?.coerceIn(0, 15) ?: 15
                    val brightness = Display.Brightness(blockLight, skyLight)
                    changes += { piece -> piece.brightness = brightness }
                    continue
                }

                if (arg == "scale") {
                    clause = "scale"
                    val x = args.getOrNull(index + 1)?.toFloatOrNull() ?: 1.0f
                    val y = args.getOrNull(index + 2)?.toFloatOrNull() ?: 1.0f
                    val z = args.getOrNull(index + 3)?.toFloatOrNull() ?: 1.0f
                    changes += { piece -> piece.scale(x, y, z) }
                    continue
                }

                if (clause == "or") {
                    val material = matchMaterial(arg)
                    if (material != null) {
                        orGroups.last() += { piece -> piece.block.material == material }
                        continue
                    }

                    if (arg in modelTags(spider)) {
                        orGroups.last() += { piece -> piece.tags.contains(arg) }
                        continue
                    }

                    if (arg == "random") {
                        val chance = args.getOrNull(index + 1)?.toDoubleOrNull() ?: run {
                            sender.sendMessage("Missing chance for random condition")
                            return@setExecutor true
                        }
                        orGroups.last() += { Math.random() < chance }
                    }
                }
            }

            val selectedPieces = pieces.filter { piece -> orGroups.any { group -> group.all { it(piece) } } }
            selectedPieces.forEach { piece -> changes.forEach { it(piece) } }
            sender.sendMessage("Modified ${selectedPieces.size} blocks with ${changes.size} changes")
            true
        }

        setTabCompleter { sender, _, _, args ->
            val spider = findSpiderForCompleter(sender)?.second
            val tags = modelTags(spider)
            val materials = blockMaterialNames()
            val currentClauseIndex = args.indexOfLast { it in clauses }
            val currentClauseLength = if (currentClauseIndex == -1) args.size else args.size - currentClauseIndex - 1
            val currentClause = if (currentClauseIndex == -1) "or" else args[currentClauseIndex]

            val options = mutableListOf<String>()
            when (currentClause) {
                "or" -> {
                    if (currentClauseLength > 1) options += clauses
                    options += tags + materials + "random"
                }
                "set_block" -> {
                    if (currentClauseLength > 1) options += clauses
                    options += materials
                }
                "copy_block" -> options += listOf("~", "~1", "~-1")
                "brightness" -> options += if (currentClauseLength > 2) clauses else List(16) { it.toString() }
                "scale" -> options += if (currentClauseLength > 3) clauses else listOf("0.5", "0.75", "1", "1.25", "1.5", "2")
            }

            matching(options, args)
        }
    }

    getCommand("animated_palette").apply {
        setExecutor { sender, _, _, args ->
            val (_, spider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val target = args.getOrNull(0) ?: return@setExecutor false
            val paletteName = args.getOrNull(1) ?: return@setExecutor false
            val palette = if (paletteName.equals("custom", ignoreCase = true)) {
                mutableListOf()
            } else {
                AnimatedPalettes.entries
                    .find { it.name.equals(paletteName, ignoreCase = true) }
                    ?.palette
                    ?.toMutableList()
                    ?: run {
                        sender.sendMessage("Invalid palette: $paletteName")
                        return@setExecutor true
                    }
            }

            for (i in 2 until args.size step 3) {
                val blockID = args[i]
                val block = matchBlockData(blockID) ?: run {
                    sender.sendMessage("Invalid material: $blockID")
                    return@setExecutor true
                }
                val blockLight = args.getOrNull(i + 1)?.toIntOrNull()?.coerceIn(0, 15) ?: 0
                val skyLight = args.getOrNull(i + 2)?.toIntOrNull()?.coerceIn(0, 15) ?: 15
                palette += block to Display.Brightness(blockLight, skyLight)
            }

            when (target) {
                "eye" -> spider.bodyPlan.eyePalette = palette
                "blinking_lights" -> spider.bodyPlan.blinkingPalette = palette
                else -> return@setExecutor false
            }

            sender.sendMessage("Set $target palette to ${palette.size} blocks")
            true
        }

        setTabCompleter { _, _, _, args ->
            val options = mutableListOf<String>()
            val presets = AnimatedPalettes.entries.map { it.name.lowercase() }

            if (args.size == 1) options += listOf("eye", "blinking_lights")
            if (args.size == 2) options += presets + "custom"
            if (args.size > 2 && args.getOrNull(1).equals("custom", ignoreCase = true)) {
                val size = args.size - 3
                if (size % 3 == 0) options += blockMaterialNames()
                if (size % 3 == 1) options += List(16) { it.toString() }
                if (size % 3 == 2) options += List(16) { it.toString() }
            }

            matching(options, args)
        }
    }

    getCommand("torso_model").apply {
        setExecutor { sender, _, _, args ->
            val (_, spider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val option = args.getOrNull(0) ?: return@setExecutor false
            val model = SpiderTorsoModels.entries
                .find { it.name.equals(option, ignoreCase = true) }
                ?.model
                ?.clone()
                ?: run {
                    sender.sendMessage("Invalid torso model: $option")
                    return@setExecutor true
                }

            spider.bodyPlan.bodyModel = model.scale(spider.bodyPlan.scale.toFloat())
            sender.sendMessage("Set torso model to ${option.lowercase()}")
            true
        }

        setTabCompleter { _, _, _, args ->
            matching(SpiderTorsoModels.entries.map { it.name.lowercase() }, args)
        }
    }

    getCommand("leg_model").apply {
        setExecutor { sender, _, _, args ->
            val (_, spider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val option = args.getOrNull(0) ?: return@setExecutor false

            when (option) {
                "empty" -> applyEmptyLegModel(spider.bodyPlan)
                "mechanical" -> applyMechanicalLegModel(spider.bodyPlan)
                "line" -> {
                    val materialName = args.getOrNull(1) ?: "minecraft:netherite_block"
                    val material = matchBlockData(materialName) ?: run {
                        sender.sendMessage("Invalid material: $materialName")
                        return@setExecutor true
                    }
                    applyLineLegModel(spider.bodyPlan, material)
                }
                else -> {
                    sender.sendMessage("Invalid leg model: $option")
                    return@setExecutor true
                }
            }

            sender.sendMessage("Set leg model to $option")
            true
        }

        setTabCompleter { _, _, _, args ->
            val options = when {
                args.size == 1 -> listOf("empty", "mechanical", "line")
                args.getOrNull(0) == "line" -> blockMaterialNames()
                else -> emptyList()
            }
            matching(options, args)
        }
    }

    getCommand("fall").apply {
        setExecutor { sender, _, _, args ->
            val (entity, spider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val height = args.getOrNull(0)?.toDoubleOrNull()
            if (height == null) {
                sender.sendMessage("Usage: /spider:fall <height>")
                return@setExecutor true
            }

            spider.teleport(entity, spider.position.clone().add(org.bukkit.util.Vector(0.0, height, 0.0)))
            sender.sendMessage("Moved spider by $height blocks")
            true
        }

        setTabCompleter { _, _, _, args -> matching(listOf("1", "5", "10", "25", "50"), args) }
    }

    getCommand("preset").apply {
        val presets = mapOf(
            "biped" to ::biped,
            "quadruped" to ::quadruped,
            "hexapod" to ::hexapod,
            "octopod" to ::octopod,
            "quadbot" to ::quadBot,
            "hexbot" to ::hexBot,
            "octobot" to ::octoBot,
            "mech" to ::mech,
            "mecha" to ::mech,
            "cyborg" to ::mech,
            "humanoid" to ::mech,
        )

        setExecutor { sender, _, _, args ->
            val name = args.getOrNull(0)?.lowercase() ?: return@setExecutor false
            val mechanicalPresets = setOf("mech", "mecha", "cyborg", "humanoid")
            val segmentCount = args.getOrNull(1)?.toIntOrNull() ?: if (name.contains("bot") || name in mechanicalPresets) 4 else 3
            val segmentLength = args.getOrNull(2)?.toDoubleOrNull() ?: 1.0
            val createPreset = presets[name]

            if (createPreset == null) {
                sender.sendMessage("Invalid preset: $name")
                return@setExecutor true
            }

            val (entity, oldSpider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val newOptions = createPreset(segmentCount, segmentLength)
            oldSpider.bodyPlan = newOptions.bodyPlan
            oldSpider.walkGait = newOptions.walkGait
            oldSpider.gallopGait = newOptions.gallopGait
            entity.query<Cloak>()?.options = newOptions.cloak
            entity.query<SoundsAndParticles>()?.options = newOptions.sound
            sender.sendMessage("Applied preset $name")
            true
        }

        setTabCompleter { _, _, _, args ->
            when (args.size) {
                1 -> matching(presets.keys, args)
                2 -> matching(listOf("2", "3", "4", "5", "6"), args)
                3 -> matching(listOf("0.5", "0.75", "1.0", "1.25", "1.5", "2.0"), args)
                else -> emptyList()
            }
        }
    }

    getCommand("scale").apply {
        setExecutor { sender, _, _, args ->
            val scale = args.getOrNull(0)?.toDoubleOrNull()

            if (scale == null || scale <= 0.0) {
                sender.sendMessage("Usage: /spider:scale <scale>")
                return@setExecutor true
            }

            val (_, spider) = requireSpiderForSender(sender) ?: return@setExecutor true
            val scaleFactor = scale / spider.bodyPlan.scale
            spider.walkGait.scale(scaleFactor)
            spider.gallopGait.scale(scaleFactor)
            spider.bodyPlan.scale(scaleFactor)
            spider.legs = emptyList()

            sender.sendMessage("Set spider scale to $scale")
            true
        }

        setTabCompleter { _, _, _, args -> matching(listOf("0.5", "0.75", "1", "1.25", "1.5", "2"), args) }
    }

    setupCustomItemCommand()

    getCommand("set_sound").apply {
        setExecutor { sender, _, _, args ->
            val (entity, _) = requireSpiderForSender(sender) ?: return@setExecutor true
            val kind = args.getOrNull(0) ?: return@setExecutor false
            val soundString = args.getOrNull(1) ?: return@setExecutor false
            val sound = parseSound(soundString) ?: run {
                sender.sendMessage("Invalid sound: $soundString")
                return@setExecutor true
            }

            val soundPlayer = SoundPlayer(
                sound = sound,
                volume = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f,
                pitch = args.getOrNull(3)?.toFloatOrNull() ?: 1.0f,
                volumeVary = args.getOrNull(4)?.toFloatOrNull() ?: 0.1f,
                pitchVary = args.getOrNull(5)?.toFloatOrNull() ?: 0.1f,
            )

            val sounds = entity.query<SoundsAndParticles>() ?: run {
                sender.sendMessage("No sound component found")
                return@setExecutor true
            }

            when (kind) {
                "step" -> sounds.options.step = soundPlayer
                else -> return@setExecutor false
            }

            sender.sendMessage("Set $kind sound to $soundString")
            true
        }

        setTabCompleter { _, _, _, args ->
            val options = when (args.size) {
                1 -> listOf("step")
                2 -> soundNames()
                3 -> listOf("0", "0.1", "0.3", "0.5", "1", "2")
                4 -> listOf("0.5", "0.75", "1", "1.25", "1.5", "2")
                5, 6 -> listOf("0", "0.05", "0.1", "0.2", "0.5", "1")
                else -> emptyList()
            }
            matching(options, args)
        }
    }

    getCommand("splay").apply {
        setExecutor { sender, _, _, args ->
            val delay = args.getOrNull(0)?.toLongOrNull() ?: 0
            val (entity, _) = requireSpiderForSender(sender) ?: return@setExecutor true

            runLater(delay) {
                splay(entity)
            }
            true
        }

        setTabCompleter { _, _, _, args -> matching(listOf("0", "5", "10", "20", "40"), args) }
    }
}

fun locationFromSender(sender: CommandSender): org.bukkit.Location? {
    return when (sender) {
        is Player -> sender.location
        is BlockCommandSender -> sender.block.location
        else -> null
    }
}

fun findSpiderForSender(
    sender: CommandSender,
    location: org.bukkit.Location,
): Pair<ECSEntity, SpiderBody>? {
    return if (sender is Player) {
        AppState.findSpiderForPlayer(sender)
    } else {
        AppState.findNearestSpider(location)
    }
}
