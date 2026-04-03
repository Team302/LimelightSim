# Build and Run Script for LimelightSim
# This script builds a fat JAR and executes it

param(
    [switch]$BuildOnly,
    [switch]$RunOnly,
    [string]$JarPath = "app/build/libs/LimelightSim-1.0-all.jar"
)

# Color output functions
function Write-Success {
    Write-Host $args -ForegroundColor Green
}

function Write-Error-Message {
    Write-Host $args -ForegroundColor Red
}

function Write-Info {
    Write-Host $args -ForegroundColor Cyan
}

# Get the script directory
$scriptDir = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition

# Main build function
function Build-Project {
    Write-Info "========================================="
    Write-Info "Building LimelightSim JAR..."
    Write-Info "========================================="
    
    # Change to project root
    Push-Location $scriptDir
    
    try {
        # Run gradle build and fatJar task
        & .\gradlew.bat clean build fatJar
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Build successful!"
            return $true
        } else {
            Write-Error-Message "Build failed!"
            return $false
        }
    } finally {
        Pop-Location
    }
}

# Main run function
function Run-Project {
    Write-Info "========================================="
    Write-Info "Running LimelightSim..."
    Write-Info "========================================="
    
    Push-Location $scriptDir
    
    try {
        $fullJarPath = Join-Path $scriptDir $JarPath
        
        if (-not (Test-Path $fullJarPath)) {
            Write-Error-Message "JAR file not found at: $fullJarPath"
            Write-Info "Please build the project first using: .\build-and-run.ps1 -BuildOnly"
            return $false
        }
        
        Write-Info "Executing JAR: $fullJarPath"
        Write-Info ""
        
        # Run the JAR
        & java -jar $fullJarPath
        
        return $LASTEXITCODE -eq 0
    } finally {
        Pop-Location
    }
}

# Main logic
if ($RunOnly) {
    Run-Project
} elseif ($BuildOnly) {
    Build-Project
} else {
    # Default: Build and run
    if (Build-Project) {
        Write-Info ""
        Run-Project
    } else {
        Write-Error-Message "Build failed. Exiting."
        exit 1
    }
}
