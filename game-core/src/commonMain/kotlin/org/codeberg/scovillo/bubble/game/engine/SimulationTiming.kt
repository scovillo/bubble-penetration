package org.codeberg.scovillo.bubble.game.engine

/** Shared fixed simulation cadence for clients and deterministic replay. */
const val MATCH_SIMULATION_STEP_MS = 16L
const val MATCH_SIMULATION_STEP_SECONDS = MATCH_SIMULATION_STEP_MS / 1_000f
