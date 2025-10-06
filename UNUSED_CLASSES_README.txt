================================================================================
                    HOW TO USE THE UNUSED CLASSES REPORTS
================================================================================

OVERVIEW
--------
Three reports have been generated to help identify and remove unused classes
from the IRCamera project. This README explains how to use them effectively.

REPORT FILES
------------

1. UNUSED_CLASSES_SUMMARY.txt
   START HERE for an overview and key statistics

2. UNUSED_CLASSES_BY_CATEGORY.txt
   USE THIS for prioritized cleanup planning

3. UNUSED_CLASSES_REPORT.txt
   REFERENCE THIS for complete list with file paths

WORKFLOW FOR CLEANUP
--------------------

STEP 1: Read the Summary
  $ cat UNUSED_CLASSES_SUMMARY.txt
  
  This gives you the big picture:
  - How many unused classes were found
  - Module breakdown
  - Notable examples
  - Recommended approach

STEP 2: Review by Category
  $ cat UNUSED_CLASSES_BY_CATEGORY.txt
  
  This helps prioritize:
  - Legacy classes (high priority)
  - Test classes (review for removal)
  - UI components (medium priority)
  - Utility classes (high priority)
  - Data models (review carefully)

STEP 3: Verify Before Removal
  For each class you want to remove, verify it's truly unused:
  
  a) Search in code:
     $ grep -r "ClassName" --include="*.java" --include="*.kt" \
       --exclude-dir="build" --exclude-dir=".git" .
  
  b) Check Android Manifest:
     $ grep "ClassName" app/src/main/AndroidManifest.xml
  
  c) Check XML resources:
     $ find . -name "*.xml" -exec grep "ClassName" {} \;
  
  d) Use IDE's "Find Usages" (Shift+F12 or Alt+F7 in IntelliJ/Android Studio)

STEP 4: Remove in Phases
  Start with low-risk classes and work your way up:
  
  PHASE 1 (Low Risk):
  - Classes with "Old" suffix
  - Empty test files
  - Clear utility classes with no references
  
  PHASE 2 (Medium Risk):
  - Unused UI components
  - Unused data models
  - Unused callbacks
  
  PHASE 3 (Requires Review):
  - Service classes (may be planned features)
  - Base classes
  - Interfaces

STEP 5: Test After Each Phase
  After removing classes:
  
  $ ./gradlew clean
  $ ./gradlew build
  $ ./gradlew test
  
  If build fails, review what was removed and restore if necessary

EXAMPLE: REMOVING A LEGACY CLASS
---------------------------------

Let's remove "ImageThreadTCOld" as an example:

1. Verify it's unused:
   $ grep -r "ImageThreadTCOld" --include="*.java" --include="*.kt" .
   
2. Check the file location:
   From report: libunified/src/main/java/com/mpdc4gsr/libunified/ir/thread/ImageThreadTCOld.java
   
3. Create a backup branch:
   $ git checkout -b cleanup/remove-legacy-classes
   
4. Remove the file:
   $ rm libunified/src/main/java/com/mpdc4gsr/libunified/ir/thread/ImageThreadTCOld.java
   
5. Test the build:
   $ ./gradlew clean build
   
6. If successful, commit:
   $ git add .
   $ git commit -m "Remove unused legacy class ImageThreadTCOld"

QUICK REFERENCE COMMANDS
-------------------------

View top 20 unused classes:
  $ head -100 UNUSED_CLASSES_REPORT.txt

Count unused classes by module:
  $ grep "File:" UNUSED_CLASSES_REPORT.txt | cut -d'/' -f6 | sort | uniq -c

Find all legacy "Old" classes:
  $ grep "Old" UNUSED_CLASSES_REPORT.txt

Find all test classes:
  $ grep "Test" UNUSED_CLASSES_REPORT.txt

IMPORTANT WARNINGS
------------------

DO NOT remove classes that are:
1. Referenced in AndroidManifest.xml (Activities, Services, Receivers, etc.)
2. Used via reflection (common in frameworks)
3. Required by dependency injection (Dagger, Hilt, etc.)
4. Entry points (Application class, Content Providers, etc.)
5. Used in XML layouts or resources
6. Part of public API if this is a library

ALWAYS verify with multiple methods before removal!

FALSE POSITIVES
---------------

Some classes may appear unused but are actually used:
- Activities registered in manifest
- Services, BroadcastReceivers, ContentProviders
- Classes loaded via reflection
- JNI native methods
- Data binding classes
- Classes referenced in XML

When in doubt, keep the class or ask for review.

TOOLS TO HELP
-------------

1. Android Lint:
   $ ./gradlew lint
   
2. Android Studio:
   - Analyze > Inspect Code
   - Analyze > Run Inspection by Name > "Unused declaration"

3. ProGuard/R8:
   Check shrink reports after release build

AUTOMATION (ADVANCED)
---------------------

To automatically remove a list of files:

1. Create a file with paths (one per line):
   $ grep "File:" UNUSED_CLASSES_REPORT.txt | cut -d' ' -f2 > to_remove.txt

2. Review and edit to_remove.txt (remove files you want to keep)

3. Remove files:
   $ while read file; do rm "$file" 2>/dev/null && echo "Removed: $file"; done < to_remove.txt

4. Test thoroughly!

GETTING HELP
------------

If you're unsure about a class:
1. Ask the original author (check git blame)
2. Search project documentation
3. Ask the team in code review
4. When in doubt, keep it

TRACKING PROGRESS
-----------------

Create a checklist in your issue/PR:
- [ ] Review PHASE 1 classes (legacy)
- [ ] Remove verified legacy classes
- [ ] Test build
- [ ] Review PHASE 2 classes (UI)
- [ ] Remove verified UI classes
- [ ] Test build and app
- [ ] Review PHASE 3 classes (services)
- [ ] Final verification

================================================================================
Good luck with the cleanup! Remember: it's better to be cautious than to break
something. When in doubt, leave it in and mark for future review.
================================================================================
