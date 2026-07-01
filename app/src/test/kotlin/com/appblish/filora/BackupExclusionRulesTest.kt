package com.appblish.filora

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pins the backup/transfer exclusion rules to REAL on-disk storage (security-impl-audit
 * F2). The effective protection today comes from excluding the whole Room DB and the
 * whole DataStore dir; this test asserts those real entries are present and that the
 * former phantom rules (a non-existent `saf_grants.xml` SharedPreferences file and an
 * `external thumbnails/` dir the app never writes) do not creep back — so a future
 * storage-location change can't silently start leaking.
 */
class BackupExclusionRulesTest {
    private val dataExtractionRules = readRes("data_extraction_rules.xml")
    private val backupRules = readRes("backup_rules.xml")

    @Test
    fun `api31plus rules exclude the real Room DB and DataStore paths`() {
        assertTrue(
            "expected database:filora.db exclusion",
            dataExtractionRules.contains(Regex("""domain="database"\s+path="filora\.db"""")),
        )
        assertTrue(
            "expected file:datastore/ exclusion",
            dataExtractionRules.contains(Regex("""domain="file"\s+path="datastore/"""")),
        )
    }

    @Test
    fun `legacy fullBackup rules exclude the real Room DB and DataStore paths`() {
        assertTrue(
            "expected database:filora.db exclusion",
            backupRules.contains(Regex("""domain="database"\s+path="filora\.db"""")),
        )
        assertTrue(
            "expected file:datastore/ exclusion",
            backupRules.contains(Regex("""domain="file"\s+path="datastore/"""")),
        )
    }

    @Test
    fun `phantom rules that match no real storage are absent`() {
        for (rules in listOf(dataExtractionRules, backupRules)) {
            assertTrue("saf_grants.xml is a phantom path — no such file exists", !rules.contains("saf_grants"))
            assertTrue("external thumbnails/ is a phantom path — cache is internal", !rules.contains("thumbnails"))
        }
    }

    private fun readRes(name: String): String {
        val candidates = listOf(
            File("src/main/res/xml/$name"), // :app module working dir
            File("app/src/main/res/xml/$name"), // repo root working dir
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from ${File(".").absolutePath}")
        return file.readText()
    }
}
