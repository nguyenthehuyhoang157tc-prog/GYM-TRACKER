package com.hoctin.gymtracker.data.catalog

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room storage for the Exercise Catalog (table `catalog_exercises`, added in DB v2 via an additive
 * migration). All columns are primitive/TEXT so the catalog needs **no Room TypeConverters** and the
 * migration's CREATE TABLE is unambiguous. Enums are stored as their `name`; list fields are stored as
 * a simple CSV. Mapping to/from the [CatalogExercise] domain model lives here.
 *
 * This table is independent reference data — it is never read or written by the workout/session/log
 * tables, the Plan, Log Workout, History, Progression V3, or Import/Export.
 */
@Entity(tableName = "catalog_exercises")
data class CatalogExerciseEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val equipment: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val level: String,
    val force: String,
    val category: String,
    val progressionType: String,
    val defaultSets: Int,
    val defaultMinReps: Int,
    val defaultMaxReps: Int,
    val defaultIncrementKg: Double,
    val sessionsRequiredBeforeIncrease: Int,
    val isBodyweight: Boolean,
    val isUnilateral: Boolean,
    val aliases: String,
    // DB-1.5 detail fields. instructions/tips are newline-joined (one step/cue per line); imageRefs
    // uses the same "|" CSV as aliases. Added in DB v3 via an additive migration.
    val instructions: String,
    val tips: String,
    val imageRefs: String,
    // DB-6B: log-field tracking type (TrackingType name). Added in DB v5 via an additive migration
    // (catalog table DROP+CREATE, then re-seeded). Default-safe via TrackingType.fromName.
    val trackingType: String,
)

private const val LIST_SEPARATOR = "|"
private const val LINE_SEPARATOR = "\n"

private fun List<String>.joinCsv(): String = joinToString(LIST_SEPARATOR)

private fun String.splitCsv(): List<String> =
    if (isBlank()) emptyList() else split(LIST_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

private fun List<String>.joinLines(): String = joinToString(LINE_SEPARATOR)

private fun String.splitLines(): List<String> =
    if (isBlank()) emptyList() else split(LINE_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

fun CatalogExercise.toEntity(): CatalogExerciseEntity = CatalogExerciseEntity(
    slug = slug,
    name = name,
    equipment = equipment.name,
    primaryMuscles = primaryMuscles.map { it.name }.joinCsv(),
    secondaryMuscles = secondaryMuscles.map { it.name }.joinCsv(),
    level = level.name,
    force = force.name,
    category = category.name,
    progressionType = progressionType.name,
    defaultSets = defaultSets,
    defaultMinReps = defaultMinReps,
    defaultMaxReps = defaultMaxReps,
    defaultIncrementKg = defaultIncrementKg,
    sessionsRequiredBeforeIncrease = sessionsRequiredBeforeIncrease,
    isBodyweight = isBodyweight,
    isUnilateral = isUnilateral,
    aliases = aliases.joinCsv(),
    instructions = instructions.joinLines(),
    tips = tips.joinLines(),
    imageRefs = imageRefs.joinCsv(),
    trackingType = trackingType.name,
)

fun CatalogExerciseEntity.toDomain(): CatalogExercise = CatalogExercise(
    slug = slug,
    name = name,
    equipment = Equipment.fromName(equipment),
    primaryMuscles = primaryMuscles.splitCsv().mapNotNull { MuscleGroup.fromName(it) },
    secondaryMuscles = secondaryMuscles.splitCsv().mapNotNull { MuscleGroup.fromName(it) },
    level = Level.fromName(level),
    force = Force.fromName(force),
    category = Category.fromName(category),
    progressionType = ProgressionType.fromName(progressionType),
    defaultSets = defaultSets,
    defaultMinReps = defaultMinReps,
    defaultMaxReps = defaultMaxReps,
    defaultIncrementKg = defaultIncrementKg,
    sessionsRequiredBeforeIncrease = sessionsRequiredBeforeIncrease,
    isBodyweight = isBodyweight,
    isUnilateral = isUnilateral,
    aliases = aliases.splitCsv(),
    instructions = instructions.splitLines(),
    tips = tips.splitLines(),
    imageRefs = imageRefs.splitCsv(),
    trackingType = TrackingType.fromName(trackingType),
)
