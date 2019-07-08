def server = ""
def rtMaven = ""
def buildInfo = ""

pipeline {
    parameters {        
        booleanParam(name: 'IsReleaseBuild', description: 'Check the box if you want to create a release build') 
        string(name: 'BranchName', defaultValue: 'master', description: 'Branch used by the job')  
    }

    agent {
        node {
            label 'Plugins'
        }
    }

    tools {
        jdk 'JDK_WINDOWS_1.8.0_92'
    }

    stages {
        stage('Remove Snapshot') {
            steps {
                
                powershell '''#------------------------------------------------------------------------------------------------------------
                # REMOVE THE WORD SNAPSHOT (ONLY FOR RELEASE BUILDS)
                #------------------------------------------------------------------------------------------------------------


                [string]$IsReleaseBuild = $ENV:IsReleaseBuild
                [string]$RootPath = "C:\\CI-Slave\\workspace\\$ENV:JOB_NAME"


                If($IsReleaseBuild -eq "true")
                {
                    Write-Host " ----------------------------------------------------- "
                    Write-Host "|  SNAPSHOT DISABLED: Removing Snapshot before build  |"
                    Write-Host " ----------------------------------------------------- "

                    $XmlPath = $RootPath + "\\pom.xml"

                    If(Test-Path "$XmlPath")
                    {  
                        [xml]$XmlDocument = Get-Content -Path $XmlPath
                        $XmlDocument.project.version = $XmlDocument.project.version.Replace("-SNAPSHOT", "")
                        $XmlDocument.Save($XmlPath)
                    }
                }
                Else
                {
                    Write-Host " ----------------------------------------------------- "
                    Write-Host "|    SNAPSHOT ENABLED: Run Build without modifying    |"
                    Write-Host " ----------------------------------------------------- " 
                }
                '''
            }
        }
    
        stage('Pre Build') {
            steps {
                script {
                    env.JAVA_HOME="${tool 'JDK_WINDOWS_1.8.0_92'}"
                    env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
                    server = Artifactory.server('-484709638@1439224648400')
                    rtMaven = Artifactory.newMavenBuild()
                    rtMaven.tool = 'mvn_3.5.0_windows'
                    rtMaven.resolver server: server, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
                    rtMaven.deployer releaseRepo:'plugins-release-local', snapshotRepo:'plugins-snapshot-local', server: server
                    buildInfo = Artifactory.newBuildInfo()
                    rtMaven.opts = "-Dskip.tests=true,-Dbuild.number=${BUILD_NUMBER},-Dorg.apache.maven.user-settings=C:\\Jenkins\\workspace\\settings.xml"
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    buildInfo = rtMaven.run pom: 'pom.xml', goals: 'clean install'
                    server.publishBuildInfo buildInfo
                }
            }
        }
	  
        stage('Archive Artifacts') {
            steps {
                archiveArtifacts 'target/*.jar'
            }
        }	  
    }
}
