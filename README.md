# IRCamera - Multi-Sensor Recording System

IRCamera is a comprehensive multi-sensor recording system for synchronized data capture from GSR (Galvanic Skin Response), thermal camera, and RGB camera sensors. The system consists of an Android application for mobile recording and a PC controller for remote orchestration.

## Key Features

- **Multi-Sensor Synchronization**: Coordinated recording from GSR, thermal, and RGB sensors
- **Time Synchronization**: Precise timestamp alignment across all sensors
- **PC-Android Communication**: Remote control via TCP/IP networking
- **Session Management**: Complete recording session lifecycle management
- **Data Export**: Structured CSV and JSON data output
- **Real-time Visualization**: Live sensor data plotting (PC controller)

## System Architecture

```
┌─────────────────┐         Network         ┌──────────────────┐
│   PC Controller │ ◄─────────────────────► │  Android Device  │
│                 │    (TCP/IP Protocol)     │                  │
│  - Orchestrator │                          │  - GSR Sensor    │
│  - Visualizer   │                          │  - Thermal Cam   │
│  - Data Manager │                          │  - RGB Camera    │
└─────────────────┘                          └──────────────────┘
```

## Quick Start

### Android Application
```bash
# Open in Android Studio
./gradlew build

# Or build from command line
./gradlew assembleDebug
```

### PC Controller
```bash
cd pc-controller
pip install -r requirements.txt
python run_unified_controller.py
```

## Documentation

All documentation is organized in the `docs/` directory:

### 📱 Android Application
- [Android Documentation](docs/android/) - PC-Android communication, time sync
- [Code Quality](docs/anti-patterns-readme.md) - Anti-patterns analysis and best practices
- [Performance](docs/anr-prevention-guide.md) - ANR prevention guidelines

### 💻 PC Controller
- [PC Controller Documentation](pc-controller/docs/) - Complete PC controller reference
- [Quick Start](pc-controller/docs/quick-start.md) - Getting started guide
- [Protocol Guide](pc-controller/docs/protocol-bridge-guide.md) - Communication protocol

### 📚 Thesis Content
- [Thesis Documentation](docs/thesis/) - Complete thesis content
  - System design (Chapter 3)
  - Experimental evaluation (Chapter 5)
  - Requirements evaluation (Chapter 6)
  - Diagrams and figures
  - Evaluation test suite

### 🔧 Maintenance & Summaries
- [Implementation Summaries](docs/summaries/) - Feature implementations
- [Maintenance Records](docs/maintenance/) - Migrations and fixes
- [Complete Documentation Index](docs/INDEX.md) - Full documentation index

## Project Structure

```
IRCamera/
├── app/                      # Android application
│   └── src/main/java/mpdc4gsr/
├── component/                # Sensor components
│   ├── gsr-recording/
│   └── thermalunified/
├── pc-controller/            # PC controller application
│   ├── docs/                 # PC controller documentation
│   ├── src/                  # Python source code
│   └── tests/                # Test suite
├── docs/                     # Complete documentation
│   ├── android/              # Android docs
│   ├── thesis/               # Thesis content
│   ├── summaries/            # Implementation summaries
│   └── maintenance/          # Maintenance records
└── libunified/               # Shared libraries
```

## Development

### Requirements
- Android Studio Arctic Fox or later
- Kotlin 1.9+
- Python 3.8+ (for PC controller)
- Gradle 8.0+

### Building
```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build release APK
./gradlew assembleRelease
```

### Code Quality
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Review [anti-patterns checklist](docs/anti-patterns-checklist.md) before commits
- Run lint: `./gradlew lint`
- Check [ANR prevention guide](docs/anr-prevention-guide.md) for performance

## Testing

### Android Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### PC Controller Tests
```bash
cd pc-controller
python -m pytest tests/
```

### Thesis Evaluation Tests
```bash
cd docs/thesis/evaluation
python run_tests.py
```

## Contributing

1. Review [documentation index](docs/INDEX.md)
2. Follow [coding standards](docs/anti-patterns-checklist.md)
3. Ensure tests pass
4. Update documentation as needed

## License

[Add license information here]

## Contact

- Project: UCL GSR (University College London)
- Repository: https://github.com/uclgsr/IRCamera

## Documentation Links

- [Complete Documentation](docs/INDEX.md)
- [Android App](docs/android/)
- [PC Controller](pc-controller/docs/)
- [Thesis Content](docs/thesis/)
- [Code Quality](docs/anti-patterns-readme.md)
