# LimelightSim Build and Run Guide

This project includes scripts to build and run the LimelightSim application as an executable JAR file.

## Prerequisites

- Java 21 or later
- Gradle (included via gradlew)

## Building and Running

### Option 1: Batch File (Windows)

```bash
# Build and run (default)
.\build-and-run.bat

# Build only
.\build-and-run.bat -build

# Run only (JAR must be built first)
.\build-and-run.bat -run
```

### Option 2: PowerShell Script

```powershell
# Build and run (default)
.\build-and-run.ps1

# Build only
.\build-and-run.ps1 -BuildOnly

# Run only (JAR must be built first)
.\build-and-run.ps1 -RunOnly
```

### Option 3: Manual Gradle Commands

```bash
# Build the fat JAR with all dependencies
.\gradlew.bat clean build fatJar

# Run the JAR directly
java -jar app\build\libs\LimelightSim-1.0-all.jar
```

## Output

After building, the executable JAR will be located at:
```
app/build/libs/LimelightSim-1.0-all.jar
```

This is a "fat JAR" (uber JAR) that includes all dependencies, so it can be run standalone on any machine with Java 21+.

## Running the Application

Once running, the LimelightSim simulator will:
1. Start the simulation engine
2. Bind to an available port (tries 5801, 5805, 5809, 8080, 8000 in order)
3. Start the HTTP server for the web interface

Look for output like:
```
========================================
Limelight 4 Simulator Started
========================================
Web interface: http://localhost:5801
API endpoint: http://localhost:5801/api/latest
```

## Troubleshooting

- **Port already in use**: The script tries multiple ports. Close other applications using these ports or modify `LimelightSimulator.java`.
- **Build fails**: Ensure Java 21+ is installed and available in your PATH.
- **JAR not found**: Make sure to build first using `-build` flag or default build-and-run.
