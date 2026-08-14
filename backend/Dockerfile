FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package
RUN <<'EOF' sh
cat > Healthcheck.java <<'JAVA'
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Healthcheck {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://localhost:8080/actuator/health";
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || !response.body().contains("\"status\":\"UP\"")) {
            System.err.println("Healthcheck failed: HTTP " + response.statusCode() + " " + response.body());
            System.exit(1);
        }
    }
}
JAVA
javac Healthcheck.java
EOF

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
COPY --from=build /workspace/Healthcheck.class /app/Healthcheck.class
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "for var in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD; do eval value=\\$$var; if [ -z \"$value\" ]; then echo \"Missing required environment variable: $var\" >&2; exit 1; fi; done; exec java -jar /app/app.jar"]
