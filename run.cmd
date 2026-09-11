@echo off
cd /d "%~dp0"

if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" set "%%A=%%~B"
  )
)

if exist ".tools\apache-maven-3.9.16\bin\mvn.cmd" (
  set "MVN=.tools\apache-maven-3.9.16\bin\mvn.cmd"
) else (
  set "MVN=mvn"
)

"%MVN%" -q test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.selenium.test.SMSShareTest %*
