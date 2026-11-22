@rem SPDX-License-Identifier: Apache-2.0
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.

@if "%DEBUG%"=="" @echo off

if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set "NEF_JAVA17="

if defined JAVA_HOME (
    set "NEF_JAVA_HOME=%JAVA_HOME:"=%"
    if exist "%NEF_JAVA_HOME%\bin\java.exe" (
        "%NEF_JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /C:"17." >NUL
        if not errorlevel 1 set "NEF_JAVA17=%NEF_JAVA_HOME%\bin\java.exe"
    )
)

if not defined NEF_JAVA17 (
    for /f "delims=" %%J in ('where java 2^>NUL') do if not defined NEF_JAVA17 (
        "%%J" -version 2>&1 | findstr /C:"17." >NUL
        if not errorlevel 1 set "NEF_JAVA17=%%J"
    )
)

if not defined NEF_JAVA17 if defined LOCALAPPDATA (
    for /d %%D in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-17*") do if exist "%%~fD\bin\java.exe" set "NEF_JAVA17=%%~fD\bin\java.exe"
)

if not defined NEF_JAVA17 if defined ProgramFiles (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do if exist "%%~fD\bin\java.exe" set "NEF_JAVA17=%%~fD\bin\java.exe"
    for /d %%D in ("%ProgramFiles%\Java\jdk-17*") do if exist "%%~fD\bin\java.exe" set "NEF_JAVA17=%%~fD\bin\java.exe"
)

if defined NEF_JAVA17 (
    set "JAVA_EXE=%NEF_JAVA17%"
    goto execute
)

echo. 1>&2
echo ERROR: A Java 17 JDK was not found. 1>&2
echo This project needs Java 17 to run Gradle, while Minecraft is compiled and run with the Java 8 toolchain. 1>&2
echo. 1>&2
echo Install a Java 17 JDK or set JAVA_HOME to a Java 17 JDK, then run gradlew again. 1>&2
goto fail

:execute

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
