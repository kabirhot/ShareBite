# ShareBite container for online hosting (used by Render).
# Builds the Java backend, downloads the MySQL JDBC driver and serves the frontend.
FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY frontend ./frontend
COPY backend ./backend

WORKDIR /app/backend
# MySQL JDBC driver (checked against its published SHA-1)
RUN mkdir -p lib out \
 && curl -fsSL -o lib/mysql-connector-j-8.4.0.jar \
    https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar \
 && echo "b1bc0f47bcad26ad5f9bceefb63fcb920d868fca  lib/mysql-connector-j-8.4.0.jar" | sha1sum -c - \
 && javac -cp "lib/*" -d out src/sharebite/*.java

# Settings come from environment variables set on the host:
#   DB_URL, DB_USER, DB_PASSWORD, MAIL_USER, MAIL_PASSWORD, OTP_DEMO_MODE, TRUST_PROXY, PORT
ENV FRONTEND_DIR=/app/frontend
EXPOSE 8080
CMD ["java", "-cp", "out:lib/*", "sharebite.ShareBiteServer"]
