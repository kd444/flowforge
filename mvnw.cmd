@echo off
setlocal
set VERSION=3.9.9
set MVN_HOME=%~dp0.mvn\apache-maven-%VERSION%
if not exist "%MVN_HOME%\bin\mvn.cmd" (
  echo Install Maven %VERSION% into .mvn\apache-maven-%VERSION% or use the Unix mvnw script.
  exit /b 1
)
"%MVN_HOME%\bin\mvn.cmd" %*
