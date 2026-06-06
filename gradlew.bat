@rem Gradle startup script for Windows
@echo off
setlocal

set APP_HOME=%~dp0
set APP_HOME=%APP_HOME:~0,-1%

if defined JAVA_HOME (
  if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JAVA_HOME points to a missing java.exe: %JAVA_HOME%\bin\java.exe
    exit /b 1
  )
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
  where /q java.exe
  if errorlevel 1 (
    echo ERROR: JAVA_HOME is not set and java.exe is not on PATH.
    exit /b 1
  )
)

"%JAVA_EXE%" -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
if errorlevel 1 (
  endlocal
  exit /b 1
)
endlocal
exit /b 0
