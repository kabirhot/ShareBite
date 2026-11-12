#!/bin/sh
# Compiles and starts the ShareBite backend (macOS / Linux).
cd "$(dirname "$0")" || exit 1
[ -f db.properties ] || { echo "db.properties not found. Copy db.properties.example to db.properties and set your MySQL password."; exit 1; }
mkdir -p out
javac -cp "lib/*" -d out src/sharebite/*.java || { echo "Compile failed. JDK 17 or newer is needed: java -version"; exit 1; }
exec java -cp "out:lib/*" sharebite.ShareBiteServer
