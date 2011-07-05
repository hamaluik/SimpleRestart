@ECHO OFF
SET BINDIR=%~dp0
CD /D "%BINDIR%"
:start
"%ProgramFiles%\Java\jre6\bin\java.exe" -Xincgc -Xmx1G -jar craftbukkit-0.0.1-SNAPSHOT.jar
goto start