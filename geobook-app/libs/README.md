This folder stores Oracle JDBC and Oracle Multimedia JARs required at runtime.

Do NOT commit JAR files into the repository. Place required jars (e.g., ojdbc8.jar, ordim.jar, oramedia.jar) here and start the app with:

  mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dloader.path=$(pwd)/libs"

Or for packaged jar:

  java -Dloader.path=$(pwd)/libs -jar target/<artifact>.jar

Keep this folder out of version control.
