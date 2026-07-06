package com.arie.recomp.data

import com.arie.recomp.data.db.SetLog
import kotlin.math.min

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val core: List<String>,        // always included
    val accessories: List<String>  // added as session length scales up
)

/**
 * Full-body recomp program, 2-3 days/week. Workouts simply alternate
 * A, B, A, B... so a 3-day week is A/B/A and the next is B/A/B.
 */
object Program {
    val A = WorkoutTemplate(
        id = "A", name = "Full Body A",
        core = listOf("smith_squat", "bench_press", "smith_row", "db_lateral_raise"),
        accessories = listOf("db_curl", "plank")
    )
    val B = WorkoutTemplate(
        id = "B", name = "Full Body B",
        core = listOf("smith_rdl", "smith_ohp", "smith_split_squat", "db_oh_triceps"),
        accessories = listOf("db_hammer_curl", "smith_calf_raise")
    )

    const val SETS_PER_EXERCISE = 3

    fun templateByCount(completedCount: Int): WorkoutTemplate =
        if (completedCount % 2 == 0) A else B

    fun byId(id: String): WorkoutTemplate = if (id == "A") A else B

    /** 30 min -> 4 exercises, 40 min -> 5, 50+ min -> 6. */
    fun exerciseIdsFor(template: WorkoutTemplate, sessionMinutes: Int): List<String> {
        val extra = when {
            sessionMinutes >= 50 -> 2
            sessionMinutes >= 40 -> 1
            else -> 0
        }
        return template.core + template.accessories.take(extra)
    }
}

data class Suggestion(val weightLbs: Double, val reps: Int, val note: String)

/**
 * Simple double progression: work within the rep range at a fixed weight,
 * and when every set hits the top of the range, add weight and drop back
 * to the bottom of the range.
 */
object Progression {

    fun epley(weightLbs: Double, reps: Int): Double =
        if (weightLbs <= 0) 0.0 else weightLbs * (1 + reps / 30.0)

    fun suggest(ex: Exercise, lastSets: List<SetLog>, dumbbellMaxLbs: Double): Suggestion {
        if (ex.isTimed) {
            val best = lastSets.maxOfOrNull { it.reps }
                ?: return Suggestion(0.0, ex.repRangeLow, ex.startNote)
            val next = min(best + 5, 120)
            return Suggestion(0.0, next, "Hold ${next}s — 5 more than last time")
        }
        if (lastSets.isEmpty()) {
            return Suggestion(ex.startingWeightLbs, ex.repRangeLow, ex.startNote)
        }

        val weight = lastSets.maxOf { it.weightLbs }
        val workSets = lastSets.filter { it.weightLbs == weight }
        val minReps = workSets.minOf { it.reps }
        val maxReps = workSets.maxOf { it.reps }

        return when {
            // Every set hit the top of the range -> move up
            minReps >= ex.repRangeHigh -> {
                val next = weight + ex.incrementLbs
                if (ex.equipment == Equipment.DUMBBELL && next > dumbbellMaxLbs) {
                    if (weight >= dumbbellMaxLbs) {
                        Suggestion(
                            dumbbellMaxLbs, min(maxReps + 2, 25),
                            "Maxed on your dumbbells — push reps (heavier DBs would help)"
                        )
                    } else {
                        Suggestion(dumbbellMaxLbs, ex.repRangeLow, "Move up! New weight")
                    }
                } else {
                    Suggestion(next, ex.repRangeLow, "Move up! +${trim(ex.incrementLbs)} lb")
                }
            }
            // Missed the bottom of the range -> consolidate
            minReps < ex.repRangeLow ->
                Suggestion(weight, ex.repRangeLow, "Same weight — build to ${ex.repRangeLow}+ clean reps")
            // In range -> chase one more rep
            else ->
                Suggestion(weight, min(maxReps + 1, ex.repRangeHigh), "Same weight — beat last time by a rep")
        }
    }

    private fun trim(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

data class RoutineStep(val name: String, val seconds: Int)

object Routines {
    const val WARMUP_VIDEO = "https://www.youtube.com/watch?v=LKSC_KujZ4g"
    const val COOLDOWN_VIDEO = "https://www.youtube.com/watch?v=Qy3U09CnELI"

    val warmup = listOf(
        RoutineStep("March in place", 45),
        RoutineStep("Arm circles — both directions", 30),
        RoutineStep("Bodyweight squats", 45),
        RoutineStep("Hip hinges — hands on hips", 30),
        RoutineStep("Leg swings — left", 20),
        RoutineStep("Leg swings — right", 20),
        RoutineStep("Wall slides / shoulder circles", 30),
        RoutineStep("Easy push-ups (knees OK)", 40)
    )

    val cooldown = listOf(
        RoutineStep("Quad stretch — left", 30),
        RoutineStep("Quad stretch — right", 30),
        RoutineStep("Hamstring stretch — left", 30),
        RoutineStep("Hamstring stretch — right", 30),
        RoutineStep("Doorway chest stretch", 30),
        RoutineStep("Cross-body shoulder — each side", 40),
        RoutineStep("Child's pose", 45)
    )
}
