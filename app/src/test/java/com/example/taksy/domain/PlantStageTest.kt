package com.example.taksy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlantStageTest {

    @Test
    fun `stage follows the thresholds`() {
        assertEquals(PlantStage.SEED, PlantStage.of(0))
        assertEquals(PlantStage.SEED, PlantStage.of(4))
        assertEquals(PlantStage.SPROUT, PlantStage.of(5))
        assertEquals(PlantStage.SEEDLING, PlantStage.of(20))
        assertEquals(PlantStage.PLANT, PlantStage.of(60))
        assertEquals(PlantStage.SEEDLING, PlantStage.of(59))
        assertEquals(PlantStage.TREE, PlantStage.of(150))
        assertEquals(PlantStage.TREE, PlantStage.of(10_000))
    }

    @Test
    fun `remaining counts the tasks to the next stage`() {
        assertEquals(5, PlantStage.SEED.remaining(0))
        assertEquals(1, PlantStage.SEED.remaining(4))
        assertEquals(15, PlantStage.SPROUT.remaining(5))
        assertEquals(0, PlantStage.TREE.remaining(500))
    }

    @Test
    fun `progress runs from 0 to 1 inside a stage`() {
        assertEquals(0f, PlantStage.SPROUT.progress(5), 0.0001f)
        assertEquals(0.5f, PlantStage.SEEDLING.progress(40), 0.0001f)
        assertEquals(1f, PlantStage.TREE.progress(150), 0.0001f)
    }

    @Test
    fun `the tree has no next stage`() {
        assertNull(PlantStage.TREE.next)
        assertEquals(PlantStage.SPROUT, PlantStage.SEED.next)
    }
}
