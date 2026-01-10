package com.anonysoul.weaver.session.infrastructure.workspace

import com.anonysoul.weaver.session.application.WorkspaceProperties
import org.springframework.stereotype.Component
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

@Component
class WorkspaceManager(
    private val workspaceProperties: WorkspaceProperties,
) {
    fun resolvePath(sessionId: Long): Path = Paths.get(workspaceProperties.basePath, sessionId.toString())

    fun prepareWorkspace(sessionId: Long): Path {
        Files.createDirectories(Paths.get(workspaceProperties.basePath))
        return resolvePath(sessionId)
    }

    fun deleteWorkspace(path: String) {
        if (path.isBlank()) {
            return
        }
        val workspacePath = Paths.get(path)
        if (Files.notExists(workspacePath)) {
            return
        }
        Files.walkFileTree(
            workspacePath,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.deleteIfExists(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    dir: Path,
                    exc: java.io.IOException?,
                ): FileVisitResult {
                    Files.deleteIfExists(dir)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }
}
