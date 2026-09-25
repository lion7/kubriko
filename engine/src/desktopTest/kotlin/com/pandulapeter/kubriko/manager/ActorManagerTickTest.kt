/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.helpers.TickSource
import kotlin.test.Test
import kotlin.test.assertEquals

class ActorManagerTickTest {

    private class CountingActor : Dynamic {
        var updateCount = 0

        override fun update(deltaTimeInMilliseconds: Int) {
            updateCount++
        }
    }

    private fun withKubriko(
        initialActors: List<CountingActor> = emptyList(),
        shouldPutFarAwayActorsToSleep: Boolean,
        block: (actorManager: ActorManager, tick: () -> Unit) -> Unit,
    ) {
        val tickSource = TickSource.manual()
        val actorManager = ActorManager.newInstance(
            initialActors = initialActors,
            shouldUpdateActorsWhileNotRunning = true,
            shouldPutFarAwayActorsToSleep = shouldPutFarAwayActorsToSleep,
        )
        val kubriko = Kubriko.newInstance(actorManager, tickSource = tickSource)
        try {
            tickSource.start()
            block(actorManager) { tickSource.tick(16) }
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun initialActorsAreUpdatedOnTheFirstTick() = repeat(ITERATIONS) {
        listOf(true, false).forEach { shouldPutFarAwayActorsToSleep ->
            val actor = CountingActor()
            withKubriko(listOf(actor), shouldPutFarAwayActorsToSleep) { _, tick ->
                tick()
                assertEquals(1, actor.updateCount)
            }
        }
    }

    @Test
    fun addedActorsAreUpdatedOnTheNextTick() = repeat(ITERATIONS) {
        listOf(true, false).forEach { shouldPutFarAwayActorsToSleep ->
            withKubriko(shouldPutFarAwayActorsToSleep = shouldPutFarAwayActorsToSleep) { actorManager, tick ->
                tick()
                val actor = CountingActor()
                actorManager.add(actor)
                tick()
                assertEquals(1, actor.updateCount)
                actorManager.remove(actor)
                tick()
                assertEquals(1, actor.updateCount)
            }
        }
    }

    private companion object {
        const val ITERATIONS = 200
    }
}
