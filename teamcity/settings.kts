import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import java.net.URL

version = "2020.1"

project {

    vcsRoot(PluginRepo)

    buildType(BuildIt)
    buildType(TestIt)
    if ("true".equals(DslContext.getParameter("deploy")))
        buildType(DeployIt)
}

object PluginRepo : GitVcsRoot({
    name = "${DslContext.getParameter("repoName")} Repo"
    url = DslContext.getParameter("fetchUrl")
    branchSpec = "+:refs/heads/*"
    authMethod = uploadedKey {
        uploadedKey = "id_rsa"
    }
})

open class MavenBuildConfiguration(configName: String, mavenGoals: String, mavenArgs: String = "", init: BuildType.() -> Unit = {}) : BuildType({
    name = configName

    vcs {
        root(PluginRepo)
    }

    steps {
        script {
            scriptContent = "echo ${DslContext.getParameter("deploy")}"
        }
        maven {
            name = "Maven ${mavenGoals}"
            goals = mavenGoals
            runnerArgs = mavenArgs
        }
    }
    init()
})


object BuildIt : MavenBuildConfiguration("Build It", "package", "-DskipTests")

object TestIt : MavenBuildConfiguration("Test It", "test", "", {
    triggers {
        vcs {
            branchFilter = "+:<default>"
        }
    }

    dependencies {
        snapshot(BuildIt) {
            reuseBuilds = ReuseBuilds.NO
        }
    }
})

object DeployIt : BuildType ({
    name = "Deploy It"
    type = Type.DEPLOYMENT

    steps {
        script {
            scriptContent = "echo Deployed!"
        }

    }
    dependencies {
        dependency(TestIt) {
            snapshot {}
            artifacts {
                artifactRules = "deployment"
            }
        }
    }
})