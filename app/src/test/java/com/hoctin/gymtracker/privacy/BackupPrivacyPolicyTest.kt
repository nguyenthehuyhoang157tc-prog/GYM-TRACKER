package com.hoctin.gymtracker.privacy

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class BackupPrivacyPolicyTest {

    private val excludedDomains = setOf(
        "root",
        "file",
        "database",
        "sharedpref",
        "external",
        "device_root",
        "device_file",
        "device_database",
        "device_sharedpref",
    )

    @Test
    fun manifestDisablesAutomaticBackupAndReferencesBothRuleFormats() {
        val document = parse(projectFile("src/main/AndroidManifest.xml"))
        val application = document.getElementsByTagName("application").item(0) as Element
        val androidNamespace = "http://schemas.android.com/apk/res/android"

        assertEquals("false", application.getAttributeNS(androidNamespace, "allowBackup"))
        assertEquals(
            "@xml/backup_rules",
            application.getAttributeNS(androidNamespace, "fullBackupContent"),
        )
        assertEquals(
            "@xml/data_extraction_rules",
            application.getAttributeNS(androidNamespace, "dataExtractionRules"),
        )
    }

    @Test
    fun sourceSetOverlaysCannotReenableOrRedirectBackupPolicy() {
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val sourceRoot = projectFile("src")
        val manifests = sourceRoot.walkTopDown()
            .filter { it.isFile && it.name == "AndroidManifest.xml" }
            .toList()

        assertTrue("Expected at least the main source manifest", manifests.isNotEmpty())
        manifests.forEach { manifest ->
            val application = parse(manifest).getElementsByTagName("application").item(0) as? Element
                ?: return@forEach
            val attributes = mapOf(
                "allowBackup" to "false",
                "fullBackupContent" to "@xml/backup_rules",
                "dataExtractionRules" to "@xml/data_extraction_rules",
            )

            attributes.forEach { (name, expected) ->
                if (application.hasAttributeNS(androidNamespace, name)) {
                    assertEquals(
                        "${manifest.invariantSeparatorsPath} must not diverge from the app backup policy",
                        expected,
                        application.getAttributeNS(androidNamespace, name),
                    )
                }
            }
        }
    }

    @Test
    fun legacyRulesExcludeEverySupportedStorageDomain() {
        val document = parse(projectFile("src/main/res/xml/backup_rules.xml"))

        assertEquals("full-backup-content", document.documentElement.tagName)
        assertEquals(excludedDomains, exclusions(document.documentElement))
        assertEquals(0, document.getElementsByTagName("include").length)
    }

    @Test
    fun modernRulesIndependentlyExcludeCloudAndDeviceTransfer() {
        val document = parse(projectFile("src/main/res/xml/data_extraction_rules.xml"))
        val root = document.documentElement

        assertEquals("data-extraction-rules", root.tagName)
        assertEquals(excludedDomains, exclusions(singleElement(root, "cloud-backup")))
        assertEquals(excludedDomains, exclusions(singleElement(root, "device-transfer")))
        assertEquals(0, document.getElementsByTagName("include").length)
        assertEquals(0, document.getElementsByTagName("cross-platform-transfer").length)
    }

    private fun exclusions(parent: Element): Set<String> {
        val nodes = parent.getElementsByTagName("exclude")
        val domains = mutableSetOf<String>()

        for (index in 0 until nodes.length) {
            val exclusion = nodes.item(index) as Element
            assertEquals("Every exclusion must cover its domain recursively", ".", exclusion.getAttribute("path"))
            assertTrue("Duplicate exclusion for ${exclusion.getAttribute("domain")}", domains.add(exclusion.getAttribute("domain")))
        }

        return domains
    }

    private fun singleElement(parent: Element, tagName: String): Element {
        val nodes = parent.getElementsByTagName(tagName)
        assertEquals("Expected exactly one <$tagName> section", 1, nodes.length)
        return nodes.item(0) as Element
    }

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(file)

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("app", path))
        return candidates.firstOrNull(File::exists)
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
