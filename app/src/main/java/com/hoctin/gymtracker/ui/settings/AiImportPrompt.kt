package com.hoctin.gymtracker.ui.settings

const val AI_IMPORT_PROMPT = """
You are helping me create an AURON import JSON from workout screenshots or notes.

Convert the attached screenshots/notes into a valid JSON object for AURON's Import screen.

Return only raw JSON. Do not wrap it in markdown. Do not add explanations before or after the JSON.

The JSON must use this exact top-level shape:
{
  "workoutDays": [
    {
      "id": 1,
      "name": "Push",
      "sortOrder": 0
    }
  ],
  "exercises": [
    {
      "id": 1,
      "dayId": 1,
      "name": "Incline press",
      "targetSets": 3,
      "targetMinReps": 8,
      "targetMaxReps": 12,
      "sortOrder": 0
    }
  ],
  "workoutSessions": [],
  "loggedSets": []
}

Rules:
- Include all workout days and exercises you can infer from my screenshots/notes.
- Use stable positive integer IDs.
- Every exercise dayId must match an existing workoutDays id.
- sortOrder starts at 0 within each list/day and follows the order shown in the notes.
- targetSets, targetMinReps, and targetMaxReps must be numbers.
- If a field is unknown, make a conservative best guess and keep names clear.
- If no workout history is provided, leave workoutSessions and loggedSets as empty arrays.
- Omit savedWorkouts and savedWorkoutExercises unless the notes explicitly describe reusable one-off
  workout templates separate from the plan. When present, include both arrays together. Empty arrays
  intentionally replace the user's Saved Workouts with an empty collection.
- If workout history is provided, every loggedSet sessionId must match a workoutSessions id, and exerciseId should match the relevant exercise id.
- I will review the JSON before importing it into AURON.
"""
