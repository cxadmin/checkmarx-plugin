param(
[Parameter(Mandatory=$true)]
[string]$Job
)

Invoke-WebRequest -Uri "http://cx-jenkins:8080/jenkins/job/$Job/buildWithParameters?token=PluginsToken&PARAMETER=true" -Method POST