package com.arie.recomp.data

import android.net.Uri

enum class Equipment(val label: String) {
    SMITH("Smith machine"),
    BARBELL("Bench press barbell"),
    DUMBBELL("Dumbbells"),
    FREE_BARBELL("Free barbell"),
    BODYWEIGHT("Bodyweight")
}

data class Exercise(
    val id: String,
    val name: String,
    val equipment: Equipment,
    val videoUrl: String,            // curated demo video for this exact movement
    val repRangeLow: Int,
    val repRangeHigh: Int,
    val incrementLbs: Double,
    val startingWeightLbs: Double,
    val startNote: String,
    val cues: List<String>,          // max 4, one line each — glance between sets
    val mistakes: List<String>,      // 2-3 quick bullets
    val isTimed: Boolean = false     // plank: the "reps" field is seconds held
) {
    // Fallback in case the curated video is ever taken down.
    val searchUrl: String
        get() = "https://www.youtube.com/results?search_query=" +
            Uri.encode("$name proper form tutorial")
}

object ExerciseCatalog {

    val all: List<Exercise> = listOf(
        Exercise(
            id = "smith_squat",
            name = "Smith Machine Squat",
            equipment = Equipment.SMITH,
            videoUrl = "https://www.youtube.com/watch?v=AHnX-aimA4E",
            repRangeLow = 8, repRangeHigh = 12,
            incrementLbs = 10.0, startingWeightLbs = 20.0,
            startNote = "Weight = plates you add (don't count the bar). Start light, own the form.",
            cues = listOf(
                "Feet shoulder-width, slightly in front of the bar",
                "Chest up, core braced, back flat",
                "Lower until thighs hit parallel",
                "Drive through the whole foot to stand"
            ),
            mistakes = listOf(
                "Feet directly under the bar",
                "Cutting depth short",
                "Bouncing out of the bottom"
            )
        ),
        Exercise(
            id = "bench_press",
            name = "Barbell Bench Press",
            equipment = Equipment.BARBELL,
            videoUrl = "https://www.youtube.com/watch?v=vcBig73ojpE",
            repRangeLow = 6, repRangeHigh = 10,
            incrementLbs = 5.0, startingWeightLbs = 45.0,
            startNote = "Start with the empty bar (45 lb) and add from there.",
            cues = listOf(
                "Shoulder blades pinched back and down",
                "Grip about 1.5× shoulder width",
                "Lower under control to mid-chest",
                "Feet planted — press up and slightly back"
            ),
            mistakes = listOf(
                "Bouncing the bar off the chest",
                "Elbows flared to 90°",
                "Butt lifting off the bench"
            )
        ),
        Exercise(
            id = "smith_row",
            name = "Smith Machine Bent-Over Row",
            equipment = Equipment.SMITH,
            videoUrl = "https://www.youtube.com/watch?v=tGGq2VZIW_M",
            repRangeLow = 8, repRangeHigh = 12,
            incrementLbs = 10.0, startingWeightLbs = 20.0,
            startNote = "Weight = plates added. Hinge deep enough that the bar hangs below the knees.",
            cues = listOf(
                "Hinge at the hips, torso ~45° or lower",
                "Pull the bar to your lower ribs",
                "Squeeze shoulder blades at the top",
                "Neck neutral, back flat"
            ),
            mistakes = listOf(
                "Standing too upright",
                "Yanking with arms and momentum",
                "Rounding the lower back"
            )
        ),
        Exercise(
            id = "smith_rdl",
            name = "Smith Machine Romanian Deadlift",
            equipment = Equipment.SMITH,
            videoUrl = "https://www.youtube.com/watch?v=pwGLw2xJCGw",
            repRangeLow = 8, repRangeHigh = 12,
            incrementLbs = 10.0, startingWeightLbs = 30.0,
            startNote = "Weight = plates added. You should feel the hamstrings stretch, not the back.",
            cues = listOf(
                "Bar close — over your shoelaces",
                "Push hips back, slight knee bend",
                "Flat back, lower until hamstrings stretch",
                "Drive hips forward, squeeze glutes to stand"
            ),
            mistakes = listOf(
                "Rounding the back",
                "Squatting instead of hinging",
                "Going too deep too soon"
            )
        ),
        Exercise(
            id = "smith_ohp",
            name = "Smith Machine Overhead Press",
            equipment = Equipment.SMITH,
            videoUrl = "https://www.youtube.com/watch?v=kYZ0aUEzgEQ",
            repRangeLow = 6, repRangeHigh = 10,
            incrementLbs = 5.0, startingWeightLbs = 20.0,
            startNote = "Weight = plates added. Shoulders warm up slowly — no ego here.",
            cues = listOf(
                "Bar starts at upper chest, elbows under the bar",
                "Core braced, glutes tight",
                "Press to full lockout overhead",
                "Wrists stacked over elbows"
            ),
            mistakes = listOf(
                "Overarching the lower back",
                "Stopping short of lockout",
                "Pressing in front of the face"
            )
        ),
        Exercise(
            id = "smith_split_squat",
            name = "Smith Machine Split Squat",
            equipment = Equipment.SMITH,
            videoUrl = "https://www.youtube.com/watch?v=MXrSCU4P9L4",
            repRangeLow = 8, repRangeHigh = 12,
            incrementLbs = 10.0, startingWeightLbs = 0.0,
            startNote = "Reps are per leg. Start with just the bar — this one burns.",
            cues = listOf(
                "One foot about two feet in front of the other",
                "Torso upright, core tight",
                "Lower until the back knee nearly touches",
                "Push through the front foot"
            ),
            mistakes = listOf(
                "Stance too short",
                "Leaning forward",
                "Front knee caving in"
            )
        ),
        Exercise(
            id = "db_lateral_raise",
            name = "Dumbbell Lateral Raise",
            equipment = Equipment.DUMBBELL,
            videoUrl = "https://www.youtube.com/watch?v=3VcKaXpzqRo",
            repRangeLow = 12, repRangeHigh = 20,
            incrementLbs = 2.5, startingWeightLbs = 5.0,
            startNote = "Weight is per dumbbell. Lighter than you think — strict form.",
            cues = listOf(
                "Slight bend in the elbows",
                "Raise to shoulder height, no higher",
                "Lead with the elbows",
                "Lower slowly — no dropping"
            ),
            mistakes = listOf(
                "Swinging or shrugging the weight up",
                "Going too heavy",
                "Raising above the shoulders"
            )
        ),
        Exercise(
            id = "db_curl",
            name = "Dumbbell Biceps Curl",
            equipment = Equipment.DUMBBELL,
            videoUrl = "https://www.youtube.com/watch?v=ykJmrZ5v0Oo",
            repRangeLow = 10, repRangeHigh = 15,
            incrementLbs = 2.5, startingWeightLbs = 10.0,
            startNote = "Weight is per dumbbell.",
            cues = listOf(
                "Elbows pinned to your sides",
                "Curl up, squeeze at the top",
                "Lower slowly to a full stretch"
            ),
            mistakes = listOf(
                "Swinging the torso",
                "Half reps",
                "Elbows drifting forward"
            )
        ),
        Exercise(
            id = "db_hammer_curl",
            name = "Dumbbell Hammer Curl",
            equipment = Equipment.DUMBBELL,
            videoUrl = "https://www.youtube.com/watch?v=zC3nLlEvin4",
            repRangeLow = 10, repRangeHigh = 15,
            incrementLbs = 2.5, startingWeightLbs = 10.0,
            startNote = "Weight is per dumbbell.",
            cues = listOf(
                "Palms facing each other",
                "Elbows steady at your sides",
                "Squeeze at the top, lower slow"
            ),
            mistakes = listOf(
                "Using momentum",
                "Wrists flopping around"
            )
        ),
        Exercise(
            id = "db_oh_triceps",
            name = "Dumbbell Overhead Triceps Extension",
            equipment = Equipment.DUMBBELL,
            videoUrl = "https://www.youtube.com/watch?v=v1qDINLCb8Q",
            repRangeLow = 10, repRangeHigh = 15,
            incrementLbs = 2.5, startingWeightLbs = 10.0,
            startNote = "One dumbbell, both hands cupped under the top plate.",
            cues = listOf(
                "Hold one dumbbell with both hands overhead",
                "Elbows point forward, stay narrow",
                "Lower behind the head, extend fully"
            ),
            mistakes = listOf(
                "Flaring the elbows wide",
                "Arching the lower back"
            )
        ),
        Exercise(
            id = "plank",
            name = "Plank",
            equipment = Equipment.BODYWEIGHT,
            videoUrl = "https://www.youtube.com/watch?v=mwlp75MS6Rg",
            repRangeLow = 20, repRangeHigh = 60,
            incrementLbs = 0.0, startingWeightLbs = 0.0,
            startNote = "Log seconds held in the reps field.",
            cues = listOf(
                "Elbows under shoulders",
                "Straight line, head to heels",
                "Squeeze glutes, brace abs, keep breathing"
            ),
            mistakes = listOf(
                "Hips sagging",
                "Butt in the air",
                "Holding your breath"
            ),
            isTimed = true
        ),
        Exercise(
            id = "smith_calf_raise",
            name = "Smith Machine Calf Raise",
            equipment = Equipment.SMITH,
            videoUrl = "https://www.youtube.com/watch?v=avO_qtvHJAg",
            repRangeLow = 12, repRangeHigh = 20,
            incrementLbs = 10.0, startingWeightLbs = 20.0,
            startNote = "Balls of feet on a plate for extra range.",
            cues = listOf(
                "Balls of feet on a plate or block",
                "Rise as high as possible, pause",
                "Lower to a full stretch"
            ),
            mistakes = listOf(
                "Bouncing",
                "Short range of motion"
            )
        )
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Exercise = byId.getValue(id)
}
