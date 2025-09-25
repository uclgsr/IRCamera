#!/bin/bash
./gradlew :app:compileDebugKotlin --continue 2>&1 | grep -A 3 -B 1 "SessionOrchestrationDemo.kt" | head -30
