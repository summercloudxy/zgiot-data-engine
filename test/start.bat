
set LOG_PATH=./logs
set LOG_FILE_NAME=dataengine.log

%JAVA_HOME%\bin\java -cp .\config\ -jar  ..\target\zgiot-data-engine-1.0.jar --spring.profiles.active=dev  --spring.config.name=dataengine 
pause