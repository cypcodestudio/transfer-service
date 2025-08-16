# Use a base image with OpenJDK
FROM amazoncorretto:17

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot JAR file into the container
# Replace 'your-spring-boot-app.jar' with the actual name of your built JAR
COPY target/*.jar /app/application.jar

# Expose the port your application runs on (default for Spring Boot is 8080)
EXPOSE 8080

# Define the command to run your Spring Boot application
CMD ["java", "-jar", "/app/application.jar"]