package com.ironsidemedical.connect.data

import com.ironsidemedical.connect.data.repository.AuditRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Requirement trace: SRS-SEC-005 (Audit chain integrity)
 * Verifies that the hash chain algorithm is deterministic and tamper-evident.
 */
class AuditRepositoryTest {

    @Test
    fun `computeChainHash is deterministic for identical inputs`() {
        val h1 = AuditRepository.computeChainHash(
            prevHash = AuditRepository.GENESIS_HASH,
            eventType = "APP_LAUNCH",
            detail = "version=1.0.0",
            timestampEpochSec = 1_700_000_000L,
        )
        val h2 = AuditRepository.computeChainHash(
            prevHash = AuditRepository.GENESIS_HASH,
            eventType = "APP_LAUNCH",
            detail = "version=1.0.0",
            timestampEpochSec = 1_700_000_000L,
        )
        assertEquals(h1, h2)
    }

    @Test
    fun `computeChainHash produces different hashes for different prevHash`() {
        val h1 = AuditRepository.computeChainHash("aabbcc", "APP_LAUNCH", "", 0L)
        val h2 = AuditRepository.computeChainHash("ddeeff", "APP_LAUNCH", "", 0L)
        assertNotEquals(h1, h2)
    }

    @Test
    fun `computeChainHash produces different hashes for different eventType`() {
        val h1 = AuditRepository.computeChainHash("abc", "APP_LAUNCH", "", 0L)
        val h2 = AuditRepository.computeChainHash("abc", "USER_LOGOUT", "", 0L)
        assertNotEquals(h1, h2)
    }

    @Test
    fun `computeChainHash produces different hashes for different timestamp`() {
        val h1 = AuditRepository.computeChainHash("abc", "APP_LAUNCH", "", 1000L)
        val h2 = AuditRepository.computeChainHash("abc", "APP_LAUNCH", "", 2000L)
        assertNotEquals(h1, h2)
    }

    @Test
    fun `resulting hash is a valid 64-character hex string`() {
        val hash = AuditRepository.computeChainHash(AuditRepository.GENESIS_HASH, "TEST", "detail", 0L)
        assertEquals(64, hash.length)
        assert(hash.all { it.isDigit() || it in 'a'..'f' }) {
            "Hash contains non-hex characters: $hash"
        }
    }
}
