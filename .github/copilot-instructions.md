### **Core Principles**

* **Communication:** Be modest and succinct.
* **Clarity:** Always ask for clarification if a requirement or task is uncertain.
* **Character Set:** Use only ASCII-safe characters in all code, comments, and commit messages.

### **Development Workflow & Verification**

2. **Pre-Commit Build:** Always execute a full Gradle build (`gradle build all`) before committing changes.
4. **Final Build Check:** Re-verify with a final `gradle build` to ensure project integrity.

### **Coding and Architectural Standards**

* **Code Conventions:** Strictly adhere to the official Kotlin and Android coding conventions.
* **Architecture:**
    * Implement all features following the Jetpack Compose and Model-View-ViewModel (MVVM) architecture.
    * Use the Repository pattern for data abstraction and access.
* **Commenting:**
    * Only add comments that are essential for development, such as explaining complex algorithms or non-obvious logic.
    * Refrain from adding non-development-related comments.
* **Code Deletion:** Remove redundant files and code duplications

### **Implementation Ground Truth**

For specific feature integrations where a local implementation is missing, refer to the following repositories as the
definitive source:

* **TOPDON TC001 Integration:**
    * **Main:** `https://github.com/CoderCaiSL/IRCamera/tree/github-main_ircamera`
    * **BLE:** `https://github.com/CoderCaiSL/IRCamera/tree/github-main_ircamera/BleModule`
* **Shimmer3 GSR Integration:**
    * `https://github.com/ShimmerEngineering/Shimmer-Java-Android-API.git`
    * `https://github.com/ShimmerEngineering/ShimmerAndroidAPI`

### **File Exclusions**

* Exclude all Markdown (`.md`) files from agent analysis and processing
* Exclude all LaTeX (`.tex`, `.latex`) files from agent analysis and processing
* Exclude `docs/` directory from agent processing
* See `.copilotignore` for the complete exclusion list

### **Prohibitions**

* Do not use emojis in any context (code, comments, documentation, or commit messages).
* Do not generate Markdown (`.md`) documentation files.