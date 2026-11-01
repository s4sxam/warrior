package com.tanay.warrior.nnn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledDomainRulesTest {
    private val rules = BundledDomainRules.rules

    @Test fun hasPositiveVersionAndRules() {
        assertTrue(BundledDomainRules.VERSION > 0)
        assertTrue(rules.isNotEmpty())
    }

    @Test fun everyDomainIsAlreadyNormalizedAndUnique() {
        rules.forEach { assertEquals(it.domain, HostNormalizer.normalize(it.domain)) }
        assertEquals(rules.size, rules.map { it.domain }.toSet().size)
    }

    @Test fun everyRuleCarriesProvenanceAndVersion() {
        rules.forEach {
            assertEquals(BundledDomainRules.SOURCE, it.source)
            assertEquals(BundledDomainRules.VERSION, it.ruleVersion)
            assertTrue(it.confidence in 1..100)
            assertTrue(it.enabled)
        }
    }

    @Test fun engineAcceptsAllBundledRules() {
        assertEquals(0, DomainRuleEngine(rules).droppedRuleCount)
    }

    @Test fun noRuleIsASingleLabelOrTld() = rules.forEach { assertTrue(it.domain.contains('.')) }
}
