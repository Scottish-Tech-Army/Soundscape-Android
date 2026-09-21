package org.scottishtecharmy.soundscape.platform

import okio.FileSystem

/**
 * The platform's real filesystem.
 *
 * `okio.FileSystem.SYSTEM` is declared in okio's intermediate
 * `systemFileSystemMain` source set, which is not visible from our
 * `commonMain`. We forward to it from each platform's actual instead so
 * commonMain code can read/write files without per-call platform branching.
 */
internal expect val systemFileSystem: FileSystem
