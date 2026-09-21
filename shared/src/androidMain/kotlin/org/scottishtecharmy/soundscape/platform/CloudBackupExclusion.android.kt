package org.scottishtecharmy.soundscape.platform

/**
 * Nothing to do on Android. Auto Backup is configured by the rules in res/xml, and both
 * backup_rules.xml and data_extraction_rules.xml exclude the `file` and `external` domains,
 * which is where extracts are downloaded to. There is no per-file equivalent of the iOS flag.
 */
internal actual fun excludeFromCloudBackup(path: String) {}
