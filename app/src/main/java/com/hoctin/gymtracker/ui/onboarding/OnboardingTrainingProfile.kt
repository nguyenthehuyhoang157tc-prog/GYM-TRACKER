package com.hoctin.gymtracker.ui.onboarding

import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.floor
import org.json.JSONArray
import org.json.JSONObject

internal const val TRAINING_PROFILE_SCHEMA_VERSION = 1

internal interface StableProfileValue {
    val storageValue: String
}

internal enum class GenderIdentity(
    override val storageValue: String,
    val label: String,
) : StableProfileValue {
    MAN("man", "Male"),
    WOMAN("woman", "Female"),
    NON_BINARY("non_binary", "Non-binary"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say"),
}

internal enum class AgeGroup(
    override val storageValue: String,
    val label: String,
    val supportsAdultBmi: Boolean,
) : StableProfileValue {
    TEEN("teen", "Teen · 13–19", false),
    ADULT("adult", "Adult · 20–39", true),
    MIDDLE_AGE("middle_age", "Middle age · 40–59", true),
    OLDER_ADULT("older_adult", "Older adult · 60+", true),
}

internal enum class PrimaryTrainingGoal(
    override val storageValue: String,
    val label: String,
    val description: String,
    val icon: String,
    val currentGoal: WorkoutGoalSelection,
) : StableProfileValue {
    MUSCLE_GAIN("muscle_gain", "Build muscle", "Prioritize muscle-building coaching.", "💪", WorkoutGoalSelection.HYPERTROPHY),
    FAT_LOSS("fat_loss", "Support fat loss", "Keep strength work central while you manage nutrition separately.", "🔥", WorkoutGoalSelection.FAT_LOSS_SUPPORT),
    STRENGTH("strength", "Get stronger", "Prioritize heavier, strength-focused coaching.", "🏋️", WorkoutGoalSelection.STRENGTH),
    BODYBUILDING("bodybuilding", "Bodybuilding", "Favor hypertrophy-focused coaching and execution.", "🦾", WorkoutGoalSelection.HYPERTROPHY),
    POWERBUILDING("powerbuilding", "Powerbuilding", "Blend a strength-first intent with physique goals.", "⚡", WorkoutGoalSelection.STRENGTH),
    SPORT_PERFORMANCE("sport_performance", "Sport performance", "Keep explosive intent visible in future coaching.", "🏃", WorkoutGoalSelection.POWER),
    MAINTAIN("maintain", "Maintain", "Stay capable and keep a consistent training rhythm.", "🛡️", WorkoutGoalSelection.GENERAL_FITNESS),
    RETURN_TO_TRAINING("return_to_training", "Return to training", "Rebuild consistency without pretending setup is rehabilitation.", "↗", WorkoutGoalSelection.AUTO),
}

internal enum class TrainingExperience(
    override val storageValue: String,
    val label: String,
    val description: String,
) : StableProfileValue {
    BEGINNER("beginner", "Beginner", "Less than 6 months of consistent training"),
    NOVICE("novice", "Novice", "About 6–18 months of consistent training"),
    INTERMEDIATE("intermediate", "Intermediate", "About 1.5–4 years of consistent training"),
    ADVANCED("advanced", "Advanced", "More than 4 years of structured training"),
    ATHLETE("athlete", "Athlete", "Training supports a competitive sport"),
}

internal enum class CompoundExperience(
    override val storageValue: String,
    val label: String,
    val description: String,
) : StableProfileValue {
    NONE("none", "New to compound lifts", "You are still learning the core movement patterns."),
    BASIC("basic", "Comfortable with the basics", "You can perform common compound lifts with a repeatable setup."),
    PROFICIENT("proficient", "Proficient", "You have stable technique and meaningful load history."),
}

internal enum class SessionDuration(
    val minutes: Int,
    override val storageValue: String,
    val label: String,
) : StableProfileValue {
    MIN_30(30, "30", "30 minutes"),
    MIN_45(45, "45", "45 minutes"),
    MIN_60(60, "60", "1 hour"),
    MIN_75(75, "75", "1 hour 15 minutes"),
    MIN_90_PLUS(90, "90_plus", "1 hour 30+ minutes"),
}

internal enum class TrainingEnvironment(
    override val storageValue: String,
    val label: String,
    val description: String,
    val icon: String,
) : StableProfileValue {
    LARGE_GYM("large_gym", "Large gym", "Full equipment and machine access", "▦"),
    SMALL_GYM("small_gym", "Small gym", "Limited equipment and machines", "▥"),
    HOME("home", "Home", "Minimal equipment and space", "⌂"),
    CUSTOM("custom", "Custom", "Choose your exact equipment next", "⚙"),
}

internal enum class AvailableEquipment(
    override val storageValue: String,
    val label: String,
    val icon: String,
) : StableProfileValue {
    FULL_GYM("full_gym", "Full gym", "▦"),
    BARBELL("barbell", "Barbell", "━"),
    DUMBBELLS("dumbbells", "Dumbbells", "✦"),
    CABLE_MACHINE("cable_machine", "Cable machine", "⌁"),
    MACHINES("machines", "Machines", "▥"),
    PULLUP_DIP_STATION("pullup_dip_station", "Pull-up / dip station", "⌜⌝"),
    KETTLEBELLS("kettlebells", "Kettlebells", "●"),
    RESISTANCE_BANDS("resistance_bands", "Resistance bands", "〰"),
    BENCH("bench", "Bench", "▬"),
    BODYWEIGHT_ONLY("bodyweight_only", "Bodyweight only", "◯"),
    OTHER("other", "Other", "+"),
}

internal enum class PriorityMuscle(
    override val storageValue: String,
    val label: String,
    val icon: String,
) : StableProfileValue {
    CHEST("chest", "Chest", "◒"),
    BACK("back", "Back", "⌁"),
    SHOULDERS("shoulders", "Shoulders", "◇"),
    ARMS("arms", "Arms", "⌇"),
    LEGS("legs", "Legs", "Λ"),
    GLUTES("glutes", "Glutes", "◡"),
    CORE("core", "Core", "◎"),
    WHOLE_BODY("whole_body", "Whole body", "✦"),
}

internal enum class PhysiqueDirection(
    override val storageValue: String,
    val label: String,
    val description: String,
) : StableProfileValue {
    BALANCED("balanced", "Balanced", "No single visual emphasis"),
    V_TAPER("v_taper", "V-taper", "Back and shoulder emphasis for future planning"),
    LEAN_DEFINED("lean_defined", "Lean and defined", "Aesthetic intent only; no body-composition promise"),
    MUSCLE_SIZE("muscle_size", "Muscle size", "Higher hypertrophy emphasis for future planning"),
    STRENGTH_PHYSIQUE("strength_physique", "Strength physique", "A dense, strength-led training direction"),
    GLUTE_LEG_EMPHASIS("glute_leg_emphasis", "Glute and leg emphasis", "Lower-body emphasis for future planning"),
}

internal enum class RecoveryRating(
    override val storageValue: String,
    val label: String,
    val description: String,
) : StableProfileValue {
    POOR("poor", "Often under-recovered", "Sleep, stress or soreness often limits training."),
    AVERAGE("average", "Usually okay", "Recovery varies, but most sessions are manageable."),
    GOOD("good", "Usually well recovered", "You normally arrive ready to train."),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say", "Keep this context private."),
}

internal enum class CardioAmount(
    override val storageValue: String,
    val label: String,
    val description: String,
) : StableProfileValue {
    NONE("none", "None", "No planned cardio preference"),
    LOW("low", "Low", "A small amount alongside lifting"),
    MODERATE("moderate", "Moderate", "A regular balance of lifting and cardio"),
    HIGH("high", "High", "Cardio is a major part of your week"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say", "Leave this for later."),
}

internal enum class PainArea(
    override val storageValue: String,
    val label: String,
) : StableProfileValue {
    SHOULDER("shoulder", "Shoulder"),
    BACK("back", "Back"),
    KNEE("knee", "Knee"),
    HIP("hip", "Hip"),
    WRIST("wrist", "Wrist"),
    ELBOW("elbow", "Elbow"),
    ANKLE("ankle", "Ankle"),
    NONE("none", "None right now"),
}

internal enum class HeightUnit(override val storageValue: String) : StableProfileValue {
    CM("cm"),
    FEET_INCHES("ft_in"),
}

internal enum class WeightUnit(override val storageValue: String) : StableProfileValue {
    KG("kg"),
    LB("lb"),
}

internal enum class OptionalProfileField(override val storageValue: String) : StableProfileValue {
    DISPLAY_NAME("display_name"),
    GENDER("gender"),
    MEASUREMENTS("measurements"),
    TARGET_WEIGHT("target_weight"),
    PRIORITY_MUSCLES("priority_muscles"),
    PHYSIQUE_DIRECTION("physique_direction"),
    CONSTRAINT_NOTE("constraint_note"),
    INJURIES("injuries"),
}

internal data class OnboardingTrainingProfileDraft(
    val displayName: String = "",
    val gender: GenderIdentity? = null,
    val ageGroup: AgeGroup? = null,
    val heightCm: Double? = null,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val weightKg: Double? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val targetWeightKg: Double? = null,
    val primaryGoal: PrimaryTrainingGoal? = null,
    val experience: TrainingExperience? = null,
    val compoundExperience: CompoundExperience? = null,
    val desiredDaysPerWeek: Int? = null,
    val preferredWeekdays: Set<Int> = emptySet(),
    val sessionDuration: SessionDuration? = null,
    val environment: TrainingEnvironment? = null,
    val equipment: Set<AvailableEquipment> = emptySet(),
    val priorityMuscles: Set<PriorityMuscle> = emptySet(),
    val physiqueDirection: PhysiqueDirection? = null,
    val recovery: RecoveryRating? = null,
    val cardio: CardioAmount? = null,
    val painAreas: Set<PainArea> = emptySet(),
    val injuryNotes: String = "",
    val futureAiNote: String = "",
    val skippedFields: Set<OptionalProfileField> = emptySet(),
) {
    val currentWorkoutGoal: WorkoutGoalSelection
        get() = primaryGoal?.currentGoal ?: WorkoutGoalSelection.AUTO

    fun withEnvironment(value: TrainingEnvironment): OnboardingTrainingProfileDraft {
        val defaults = when (value) {
            TrainingEnvironment.LARGE_GYM -> setOf(AvailableEquipment.FULL_GYM)
            TrainingEnvironment.SMALL_GYM -> setOf(AvailableEquipment.DUMBBELLS, AvailableEquipment.MACHINES)
            TrainingEnvironment.HOME -> setOf(AvailableEquipment.BODYWEIGHT_ONLY)
            TrainingEnvironment.CUSTOM -> emptySet()
        }
        return copy(environment = value, equipment = defaults)
    }

    fun togglePainArea(area: PainArea): OnboardingTrainingProfileDraft {
        val updated = when {
            area == PainArea.NONE -> setOf(PainArea.NONE)
            area in painAreas -> painAreas - area
            else -> (painAreas - PainArea.NONE) + area
        }
        return copy(painAreas = updated)
    }
}

internal object MeasurementConversion {
    const val MIN_HEIGHT_CM = 120.0
    const val MAX_HEIGHT_CM = 230.0
    const val MIN_WEIGHT_KG = 35.0
    const val MAX_WEIGHT_KG = 250.0

    fun inchesToCentimeters(inches: Double): Double = inches * 2.54
    fun centimetersToInches(centimeters: Double): Double = centimeters / 2.54
    fun poundsToKilograms(pounds: Double): Double = pounds / 2.2046226218
    fun kilogramsToPounds(kilograms: Double): Double = kilograms * 2.2046226218

    fun validHeightCm(value: Double?): Boolean = value != null && value.isFinite() && value in MIN_HEIGHT_CM..MAX_HEIGHT_CM
    fun validWeightKg(value: Double?): Boolean = value != null && value.isFinite() && value in MIN_WEIGHT_KG..MAX_WEIGHT_KG
    fun round(value: Double, decimals: Int = 2): Double =
        BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

internal enum class AdultBmiRange(override val storageValue: String, val label: String) : StableProfileValue {
    BELOW("below_adult_reference", "Below the adult reference range"),
    REFERENCE("adult_reference", "Within the adult reference range"),
    ABOVE("above_adult_reference", "Above the adult reference range"),
    HIGH("high_adult_reference", "Well above the adult reference range"),
}

internal enum class BmiStatus(override val storageValue: String) : StableProfileValue {
    UNAVAILABLE("unavailable"),
    ADULT_SCREENING_ONLY("adult_screening_only"),
    ADULT_CLASSIFICATION_UNAVAILABLE("adult_classification_unavailable"),
}

internal data class BmiResult(
    val value: Double? = null,
    val status: BmiStatus = BmiStatus.UNAVAILABLE,
    val adultRange: AdultBmiRange? = null,
)

internal data class TargetBmiScreening(
    val value: Double? = null,
    val adultThresholdApplied: Boolean = false,
    val blocksContinue: Boolean = false,
    val referenceRange: AdultWeightReferenceRange? = null,
    val isAboveReferenceRange: Boolean = false,
)

internal data class AdultWeightReferenceRange(
    val minimumKg: Int,
    val maximumKg: Int,
)

internal object BmiCalculator {
    fun calculate(profile: OnboardingTrainingProfileDraft): BmiResult {
        val rawBmi = calculateRawValue(profile.heightCm, profile.weightKg) ?: return BmiResult()
        val bmi = MeasurementConversion.round(rawBmi, 1)
        if (profile.ageGroup?.supportsAdultBmi != true) {
            return BmiResult(
                value = bmi,
                status = BmiStatus.ADULT_CLASSIFICATION_UNAVAILABLE,
            )
        }
        val range = when {
            rawBmi < 18.5 -> AdultBmiRange.BELOW
            rawBmi < 25.0 -> AdultBmiRange.REFERENCE
            rawBmi < 30.0 -> AdultBmiRange.ABOVE
            else -> AdultBmiRange.HIGH
        }
        return BmiResult(
            value = bmi,
            status = BmiStatus.ADULT_SCREENING_ONLY,
            adultRange = range,
        )
    }

    fun targetWeightScreening(profile: OnboardingTrainingProfileDraft): TargetBmiScreening {
        val rawBmi = calculateRawValue(profile.heightCm, profile.targetWeightKg)
            ?: return TargetBmiScreening()
        val displayBmi = MeasurementConversion.round(rawBmi, 1)
        val adultThresholdApplied = profile.ageGroup?.supportsAdultBmi == true
        return TargetBmiScreening(
            value = displayBmi,
            adultThresholdApplied = adultThresholdApplied,
            blocksContinue = adultThresholdApplied && rawBmi < 18.5,
            referenceRange = if (adultThresholdApplied) adultReferenceWeightRange(profile.heightCm) else null,
            isAboveReferenceRange = adultThresholdApplied && rawBmi >= 25.0,
        )
    }

    fun adultReferenceWeightRange(heightCm: Double?): AdultWeightReferenceRange? {
        if (!MeasurementConversion.validHeightCm(heightCm)) return null
        val heightMeters = heightCm!! / 100.0
        val squaredHeight = heightMeters * heightMeters
        return AdultWeightReferenceRange(
            minimumKg = ceil(18.5 * squaredHeight).toInt(),
            maximumKg = floor(24.9 * squaredHeight).toInt(),
        )
    }

    private fun calculateRawValue(heightCm: Double?, weightKg: Double?): Double? {
        if (!MeasurementConversion.validHeightCm(heightCm) || !MeasurementConversion.validWeightKg(weightKg)) {
            return null
        }
        val heightMeters = heightCm!! / 100.0
        if (heightMeters <= 0.0) return null
        return weightKg!! / (heightMeters * heightMeters)
    }
}

internal data class ProfileCompleteness(
    val percentage: Int,
    val requiredComplete: Int,
    val requiredTotal: Int,
    val optionalProvided: Int,
    val optionalTotal: Int,
) {
    val requiredReady: Boolean get() = requiredComplete == requiredTotal
}

internal object ProfileCompletenessCalculator {
    fun calculate(profile: OnboardingTrainingProfileDraft, requiredWeekdays: Int): ProfileCompleteness {
        val required = listOf(
            profile.ageGroup != null,
            profile.primaryGoal != null,
            profile.experience != null,
            profile.desiredDaysPerWeek in 1..7,
            profile.desiredDaysPerWeek in 1..7 && profile.preferredWeekdays.size == profile.desiredDaysPerWeek,
            profile.sessionDuration != null,
            profile.environment != null,
            profile.equipment.isNotEmpty(),
        )
        val optional = listOf(
            profile.displayName.isNotBlank(),
            profile.gender != null && profile.gender != GenderIdentity.PREFER_NOT_TO_SAY,
            MeasurementConversion.validHeightCm(profile.heightCm) && MeasurementConversion.validWeightKg(profile.weightKg),
            MeasurementConversion.validWeightKg(profile.targetWeightKg),
            profile.priorityMuscles.isNotEmpty(),
            profile.physiqueDirection != null,
            profile.injuryNotes.isNotBlank() || profile.futureAiNote.isNotBlank() ||
                profile.painAreas.any { it != PainArea.NONE },
        )
        val completedRequired = required.count { it }
        val providedOptional = optional.count { it }
        val percentage = ((completedRequired + providedOptional) * 100.0 / (required.size + optional.size))
            .toInt()
            .coerceIn(0, 100)
        return ProfileCompleteness(
            percentage = percentage,
            requiredComplete = completedRequired,
            requiredTotal = required.size,
            optionalProvided = providedOptional,
            optionalTotal = optional.size,
        )
    }
}

internal data class SavedTrainingProfile(
    val profile: OnboardingTrainingProfileDraft,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val currentWorkoutGoal: WorkoutGoalSelection,
    val currentWeekdays: Set<Int>,
    val currentPlanDayNames: List<String>,
    val completion: ProfileCompleteness,
)

internal object OnboardingProfileJson {
    fun encodeDraft(profile: OnboardingTrainingProfileDraft): String = JSONObject()
        .put("schemaVersion", TRAINING_PROFILE_SCHEMA_VERSION)
        .put("profile", profileObject(profile))
        .toString()

    fun decodeDraft(raw: String?): OnboardingTrainingProfileDraft? = runCatching {
        if (raw.isNullOrBlank()) return@runCatching null
        val root = JSONObject(raw)
        if (root.optInt("schemaVersion", 0) != TRAINING_PROFILE_SCHEMA_VERSION) return@runCatching null
        decodeProfile(root.getJSONObject("profile"))
    }.getOrNull()

    fun encodeSaved(value: SavedTrainingProfile): String {
        val bmi = BmiCalculator.calculate(value.profile)
        return JSONObject()
            .put("schemaVersion", TRAINING_PROFILE_SCHEMA_VERSION)
            .put("createdAtEpochMillis", value.createdAtEpochMillis)
            .put("updatedAtEpochMillis", value.updatedAtEpochMillis)
            .put("profile", profileObject(value.profile))
            .put(
                "derived",
                JSONObject()
                    .put(
                        "bmi",
                        JSONObject()
                            .putNullable("value", bmi.value)
                            .put("status", bmi.status.storageValue)
                            .putNullable("adultRange", bmi.adultRange?.storageValue)
                            .put("source", "derived_from_user_height_weight"),
                    )
                    .put(
                        "completeness",
                        JSONObject()
                            .put("percentage", value.completion.percentage)
                            .put("requiredComplete", value.completion.requiredComplete)
                            .put("requiredTotal", value.completion.requiredTotal)
                            .put("optionalProvided", value.completion.optionalProvided)
                            .put("optionalTotal", value.completion.optionalTotal),
                    ),
            )
            .put(
                "currentAppEffects",
                JSONObject()
                    .put("workoutGoal", value.currentWorkoutGoal.storageValue)
                    .put("trainingWeekdays", intArray(value.currentWeekdays))
                    .put("startingPlanDayNames", stringArray(value.currentPlanDayNames))
                    .put("startingPlanUnchanged", true)
                    .put("weeklyProgressWorkoutOnly", true)
                    .put("dailyBoostExcludedFromWeeklyProgress", true),
            )
            .put(
                "provenance",
                JSONObject()
                    .put("source", "user_onboarding")
                    .put("storage", "local_shared_preferences")
                    .put("planGenerator", "none"),
            )
            .put("completion", JSONObject().put("onboardingVersion", 3).put("completed", true))
            .toString()
    }

    fun savedCreatedAt(raw: String?): Long? = runCatching {
        raw?.let(::JSONObject)?.optLong("createdAtEpochMillis")?.takeIf { it > 0L }
    }.getOrNull()

    private fun profileObject(profile: OnboardingTrainingProfileDraft): JSONObject = JSONObject()
        .putNullable("displayName", profile.displayName.trim().takeIf { it.isNotEmpty() })
        .putNullable("gender", profile.gender?.storageValue)
        .putNullable("ageGroup", profile.ageGroup?.storageValue)
        .put(
            "measurements",
            JSONObject()
                .putNullable("heightCm", profile.heightCm)
                .put("heightUnit", profile.heightUnit.storageValue)
                .putNullable("weightKg", profile.weightKg)
                .put("weightUnit", profile.weightUnit.storageValue)
                .putNullable("targetWeightKg", profile.targetWeightKg),
        )
        .put(
            "trainingIntent",
            JSONObject()
                .putNullable("primaryGoal", profile.primaryGoal?.storageValue)
                .putNullable("experience", profile.experience?.storageValue)
                .putNullable("compoundExperience", profile.compoundExperience?.storageValue)
                .putNullable("desiredDaysPerWeek", profile.desiredDaysPerWeek)
                .put("preferredWeekdays", intArray(profile.preferredWeekdays))
                .putNullable("sessionDuration", profile.sessionDuration?.storageValue)
                .putNullable("environment", profile.environment?.storageValue)
                .put("equipment", enumArray(profile.equipment))
                .put("priorityMuscles", enumArray(profile.priorityMuscles))
                .putNullable("physiqueDirection", profile.physiqueDirection?.storageValue),
        )
        .put(
            "recoveryAndConstraints",
            JSONObject()
                .putNullable("recovery", profile.recovery?.storageValue)
                .putNullable("cardio", profile.cardio?.storageValue)
                .put("painAreas", enumArray(profile.painAreas))
                .putNullable("injuryNotes", profile.injuryNotes.trim().takeIf { it.isNotEmpty() }),
        )
        .putNullable("futureAiNote", profile.futureAiNote.trim().takeIf { it.isNotEmpty() })
        .put("skippedFields", enumArray(profile.skippedFields))

    private fun decodeProfile(root: JSONObject): OnboardingTrainingProfileDraft {
        val measurements = root.optJSONObject("measurements") ?: JSONObject()
        val training = root.optJSONObject("trainingIntent") ?: JSONObject()
        val recovery = root.optJSONObject("recoveryAndConstraints") ?: JSONObject()
        return OnboardingTrainingProfileDraft(
            displayName = root.nullableString("displayName").orEmpty(),
            gender = enumValue(root.nullableString("gender"), GenderIdentity.entries),
            ageGroup = enumValue(root.nullableString("ageGroup"), AgeGroup.entries),
            heightCm = measurements.nullableDouble("heightCm"),
            heightUnit = enumValue(measurements.nullableString("heightUnit"), HeightUnit.entries) ?: HeightUnit.CM,
            weightKg = measurements.nullableDouble("weightKg"),
            weightUnit = enumValue(measurements.nullableString("weightUnit"), WeightUnit.entries) ?: WeightUnit.KG,
            targetWeightKg = measurements.nullableDouble("targetWeightKg"),
            primaryGoal = enumValue(training.nullableString("primaryGoal"), PrimaryTrainingGoal.entries),
            experience = enumValue(training.nullableString("experience"), TrainingExperience.entries),
            compoundExperience = enumValue(training.nullableString("compoundExperience"), CompoundExperience.entries),
            desiredDaysPerWeek = training.nullableInt("desiredDaysPerWeek"),
            preferredWeekdays = decodeIntSet(training.optJSONArray("preferredWeekdays")).filter { it in 1..7 }.toSortedSet(),
            sessionDuration = enumValue(training.nullableString("sessionDuration"), SessionDuration.entries),
            environment = enumValue(training.nullableString("environment"), TrainingEnvironment.entries),
            equipment = decodeEnumSet(training.optJSONArray("equipment"), AvailableEquipment.entries),
            priorityMuscles = decodeEnumSet(training.optJSONArray("priorityMuscles"), PriorityMuscle.entries),
            physiqueDirection = enumValue(training.nullableString("physiqueDirection"), PhysiqueDirection.entries),
            recovery = enumValue(recovery.nullableString("recovery"), RecoveryRating.entries),
            cardio = enumValue(recovery.nullableString("cardio"), CardioAmount.entries),
            painAreas = decodeEnumSet(recovery.optJSONArray("painAreas"), PainArea.entries),
            injuryNotes = recovery.nullableString("injuryNotes").orEmpty(),
            futureAiNote = root.nullableString("futureAiNote").orEmpty(),
            skippedFields = decodeEnumSet(root.optJSONArray("skippedFields"), OptionalProfileField.entries),
        )
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf { it.isFinite() }

    private fun JSONObject.nullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun intArray(values: Set<Int>): JSONArray = JSONArray().apply {
        values.sorted().forEach(::put)
    }

    private fun stringArray(values: List<String>): JSONArray = JSONArray().apply {
        values.forEach(::put)
    }

    private fun enumArray(values: Set<out StableProfileValue>): JSONArray = JSONArray().apply {
        values.map { it.storageValue }.sorted().forEach(::put)
    }

    private fun decodeIntSet(array: JSONArray?): Set<Int> = buildSet {
        if (array == null) return@buildSet
        repeat(array.length()) { add(array.optInt(it)) }
    }

    private fun <T : StableProfileValue> enumValue(raw: String?, values: List<T>): T? =
        values.firstOrNull { it.storageValue == raw }

    private fun <T : StableProfileValue> decodeEnumSet(array: JSONArray?, values: List<T>): Set<T> = buildSet {
        if (array == null) return@buildSet
        repeat(array.length()) { index ->
            enumValue(array.optString(index), values)?.let(::add)
        }
    }
}
