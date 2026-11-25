package com.example

import com.example.data.VerifiedCatalogRepository
import com.example.model.CompatibilityStatus
import com.example.model.ProductCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedCatalogUnitTest {
    @Test
    fun `verified watch assembly contains 12 ordered components`() {
        val bundle = VerifiedCatalogRepository.createVerifiedWatchDemoBundle("test_uid")
        assertEquals(ProductCategory.WATCHES.firestoreValue, bundle.scan.category)
        assertEquals(12, bundle.components.size)
        assertTrue(bundle.components.any { it.categoryKey == "spring_bar" })
        assertTrue(bundle.components.any { it.categoryKey == "watch_crystal" })
        assertTrue(bundle.components.any { it.categoryKey == "movement" })
    }

    @Test
    fun `repair guide for unknown part returns honest fallback without fabricated manual`() {
        val guide = VerifiedCatalogRepository.getRepairGuide(
            ProductCategory.OTHER,
            "unknown_bracket",
            "Custom Hinge Bracket"
        )
        assertTrue(guide.sourceAttribution.contains("Unverified"))
        assertTrue(guide.safetyWarnings.isNotEmpty())
        assertTrue(guide.steps.isNotEmpty())
    }

    @Test
    fun `compatibility status parsing safely defaults on unknown raw value`() {
        assertEquals(
            CompatibilityStatus.COMPATIBILITY_UNKNOWN,
            CompatibilityStatus.fromRaw("UNEXPECTED_FUTURE_VALUE")
        )
    }
}
