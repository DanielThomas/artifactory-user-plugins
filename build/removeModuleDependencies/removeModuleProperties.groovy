import org.artifactory.build.DetailedBuildRun

build {
    beforeSave { DetailedBuildRun buildRun ->
        log.debug "removing dependencies for ${buildRun.name}"
        buildRun.build.modules.each { m ->
            m.dependencies = null
        }
    }
}
