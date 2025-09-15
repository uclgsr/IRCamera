# Academic Research Chapters 1-3: Multi-Modal Physiological Sensing Platform

## Chapter 1: Introduction

### Motivation and Research Context

Interest in physiological computing has increased—leveraging bodily signals to assess internal
states in health monitoring, affective computing, and human-computer interaction. A key
physiological signal is the Galvanic Skin Response (GSR) or electrodermal activity. GSR measures
changes in the skin's electrical conductance due to sweat gland activity, influenced by the
sympathetic nervous system. These involuntary changes reflect emotional arousal and stress, making
GSR a widely accepted indicator of autonomic nervous system activity.

GSR is used in clinical psychology (e.g., biofeedback therapy and polygraph tests) and in user
experience research, where it can reveal unconscious stress or emotional responses. Moreover, modern
smartwatches from Apple and Samsung incorporate sensors for stress monitoring via GSR or related
metrics. This trend reflects the growing interest in utilising physiological signals like GSR in
everyday contexts.

Despite GSR's value, traditional measurement requires skin-contact electrodes (usually attached to
fingers or palms with conductive gel). This method is obtrusive: wires and electrodes restrict
movement, and long-term use can cause discomfort or skin irritation. These limitations make it
difficult to use GSR outside the lab. Consequently, contactless GSR measurement has become an
appealing research direction. The idea is to infer GSR (or the underlying psychophysiological
arousal) using remote sensors that require no physical contact with the user.

For example, thermal infrared cameras detect subtle skin temperature changes from blood flow and
perspiration, offering a proxy for stress responses. Facial thermal imaging is a promising
complement in emotion research, as stress and thermoregulation are connected (e.g., perspiration
causes cooling). Similarly, high-resolution RGB video combined with advanced computer vision can
non-invasively capture other physiological signals. Prior work shows that heart rate and breathing
can be measured from video of a person's face or body.

These developments suggest that multi-modal sensing—combining traditional biosensors with
imaging—could enable contactless physiological monitoring in the near future. Affective computing
research reveals that integrating multiple modalities (e.g., GSR, heart rate, facial thermal data)
can more effectively capture emotional or stress states. Advancing the field of contactless sensing
requires not only novel machine learning algorithms but also robust data acquisition platforms that
adhere to the stringent timing and synchronization standards established in experimental
psychology (e.g., using tools like PsychoPy) and neuroscience (e.g., using frameworks like LSL).

Therefore, a central motivation for this work is to engineer a system that meets these rigorous
methodological benchmarks while providing novel sensing capabilities.

### Challenges and Research Gap

Challenges remain. A key gap is the absence of an integrated platform to synchronise diverse data
streams. Most studies have tackled contactless GSR estimation in isolation or under controlled
conditions, often with unsynchronised devices. For instance, thermal cameras and wearable GSR
sensors have typically been used independently, with any data fusion done post hoc. This piecemeal
approach complicates machine learning model development, since models require well-aligned datasets
of inputs (e.g., video or thermal data) and outputs (measured GSR).

Clearly, a multi-modal data collection platform is needed to record GSR and other sensor modalities
simultaneously with proper synchronisation. Such a platform would allow researchers to gather rich,
time-aligned datasets. For example, thermal video of a participant's palm could be recorded in sync
with their GSR signal. These combined data would lay the groundwork for training and validating
models that infer GSR from camera-based sensors.

The primary contribution of this thesis is the development of just such a platform: a modular,
multi-sensor system for synchronised physiological data acquisition designed for future GSR
prediction research.

### Research Problem and Objectives

The research problem is that while general-purpose toolkits for physiological data collection
exist (e.g., PhysioKit), and established frameworks for data synchronization are available (e.g.,
LabStreamingLayer), there remains a specific methodological gap for an integrated platform optimized
for creating high-fidelity, synchronized datasets of ground-truth GSR and contactless radiometric
thermal and high-resolution visual signals captured from mobile platforms.

Existing solutions either rely on lower-cost, less-validated sensors, lack native support for
advanced mobile imaging modalities, or present significant deployment challenges in cross-platform
mobile environments. This absence of a specialized, research-grade instrument hinders the
development of robust machine learning models for contactless GSR prediction, which require
precisely aligned, high-quality multi-modal data.

The objective of this research is to design and implement a multi-modal physiological data
collection platform to create a synchronised dataset for future GSR prediction models. Unlike
end-user applications or final predictive systems, this work focuses on the data acquisition
infrastructure, essentially building the foundation on which real-time GSR inference algorithms can
be developed later.

Note that real-time GSR prediction is outside the scope of this thesis. Instead, the project aims to
facilitate future machine learning by providing a robust way to gather ground-truth GSR and
candidate predictor signals together.

### Specific Research Objectives

The following specific objectives have been defined to achieve this aim:

#### 1. Multi-Modal Platform Development

Design and develop a modular data acquisition system capable of recording synchronised physiological
and imaging data. This involves integrating a wearable GSR sensor and camera-based sensors into one
platform. In practice, the system uses a research-grade Shimmer3 GSR+ sensor for ground-truth skin
conductance, a Topdon TC001 thermal camera attached to a smartphone for thermal video, and the
smartphone's built-in RGB camera for high-resolution video.

A smartphone-based sensor node will be coordinated with a desktop controller to start and stop
recordings in unison and to timestamp all data consistently. The architecture should ensure that all
modalities are recorded simultaneously with millisecond-level alignment.

#### 2. Synchronised Data Acquisition and Management

Implement methods for precise time synchronisation and data handling across devices. A custom
control and synchronisation layer (in Python) will coordinate the sensor node(s) and ensure that GSR
readings, thermal frames, and RGB frames are all logged with synchronised timestamps.

This includes establishing a reliable communication protocol between the smartphone and the PC
controller to transmit control commands and streaming data. Data management is also addressed:
multi-modal data will be stored in appropriate formats with metadata for easy combination and
analysis. The outcome should be a well-synchronised dataset (e.g., physiological sample timestamps
aligned with video frame times) that can serve as a training corpus for machine learning.

#### 3. System Validation through Pilot Data Collection

Evaluate the integrated platform's performance and data integrity in a real recording scenario. Test
recording sessions will be conducted to verify that the system meets research-grade requirements.
For example, pilot experiments may involve human participants performing tasks designed to elicit
varying GSR responses (stress, stimuli, etc.) while the platform records all modalities.

Validation will focus on temporal synchronisation accuracy (e.g., confirming events are correctly
aligned across sensor streams) and the quality of the recorded signals (e.g., GSR signal-to-noise
ratio, thermal image resolution). The collected data will be analysed to ensure that GSR signals and
corresponding thermal/RGB data show expected correlations or time-locked changes.

Successful validation will demonstrate that the platform can reliably capture synchronised
multi-modal data suitable for subsequent machine learning analysis. (Developing the predictive model
itself is left for future work; here, the focus is on validating the data pipeline that would feed
such a model.)

### Thesis Contribution

This thesis presents a multi-sensor data collection platform that addresses current gaps. It enables
researchers to create multimodal datasets for GSR prediction, advancing contactless, real-time
stress monitoring. The project features a flexible, extensible setup—a modular sensing system—that
integrates the GSR sensor and thermal/RGB cameras, allowing future modality expansions.

This work establishes a foundation for future studies to train and test machine learning algorithms
to estimate GSR from camera data, resolved by acquiring synchronised ground-truth data.

---

## Chapter 2: Background and Literature Review

### Emotion Analysis Applications

Automated emotion detection using physiological signals has demonstrated practical value in
controlled laboratory settings. Boucsein (2012) documented extensive use of galvanic skin response (
GSR) for measuring emotional arousal, particularly in studies where self-reported measures prove
unreliable. Jangra et al. (2021) analysed GSR applications across psychology and neuropsychology,
noting its sensitivity to unconscious arousal responses that participants cannot easily suppress.

In therapy settings, Chen et al. (2019) found that GSR patterns during cognitive behavioural therapy
sessions correlated with treatment outcomes, suggesting practical utility beyond laboratory
experiments.

Multi-modal approaches combining physiological and visual signals have shown promise for robust
emotion recognition. Zhang et al. (2021) demonstrated that thermal facial imaging combined with
traditional biosensors improved stress detection accuracy to 87.9%, significantly higher than
single-modality approaches. Similarly, studies using RGB cameras for remote photoplethysmography
have achieved heart rate detection within 2–3 BPM of ground truth measurements under controlled
lighting conditions.

The current platform integrates a Shimmer3 GSR+ sensor (128 Hz, 16-bit resolution) with a Topdon
TC-series thermal camera (256×192 pixels, 25 Hz) and RGB video (30 fps) to capture synchronised
physiological and thermal responses. Hardware timestamps align data streams within 21 ms median
offset using Network Time Protocol synchronisation.

This configuration targets real-time stress assessment during controlled laboratory tasks,
specifically Stroop colour-word conflict tests and Trier Social Stress Test (TSST) protocols, where
ground-truth GSR can be collected simultaneously with contactless thermal and visual data for
supervised learning.

### Rationale for Contactless Physiological Measurement

Contact-based GSR measurement using conventional finger electrodes can introduce measurement
artifacts. Boucsein (2012) documented how electrode attachment and wire movement creates motion
artifacts in GSR data, particularly problematic during dynamic tasks. Zhang et al. (2021) quantified
this effect, showing that wired GSR sensors introduced movement-related noise spikes exceeding 2 μS
in 23% of recorded sessions during cognitive tasks.

Thermal imaging offers an alternative approach that avoids these contact-based limitations.

Recent studies demonstrate practical feasibility of contactless physiological monitoring. The RTI
International thermal imaging study (2024) measured nasal temperature changes during mental effort
tasks, finding 0.3–0.7°C cooling responses that correlated with cognitive load (r = 0.68). Zhang et
al. (2021) achieved 89.7% accuracy in stress classification using a FLIR Lepton 3.5 thermal camera (
160 × 120 resolution, 9 Hz) combined with facial region-of-interest temperature tracking.

However, these studies typically used higher-resolution thermal cameras or controlled laboratory
conditions.

Current platform specifications address known limitations in prior contactless work. The Topdon
TC-series camera provides 256 × 192 pixel thermal resolution at 25 Hz, offering better temporal
resolution than the 9 Hz FLIR devices used in previous studies. Radiometric temperature data (±0.1°C
accuracy) enables precise measurement of the nose-tip cooling responses documented by RTI
International.

RGB video at 30 fps captures concurrent facial expressions for multimodal analysis, while the
Shimmer3 GSR+ sensor (128 Hz sampling, 10 kΩ to 4.7 MΩ range) provides ground truth electrodermal
activity for supervised learning. The goal is predicting GSR levels from thermal and RGB features
during controlled stress induction protocols.

Unlike previous studies that focused on binary stress classification, this approach targets
continuous GSR prediction to enable real-time stress level estimation rather than simple
stressed/not-stressed categorization.

### Thermal Cues of Stress in Humans

Quantified thermal stress responses have been documented using high-resolution thermal cameras.
Zhang et al. (2021) measured nasal temperature changes during cognitive stress tasks using a FLIR
A655sc camera (640×480 pixels, 0.02°C sensitivity), finding average nose-tip cooling of 0.47 ±
0.23°C during Stroop task performance (n=32 participants).

RTI International (2024) documented similar responses with a FLIR One Pro camera, measuring
0.3–0.7°C nasal cooling that correlated with subjective stress ratings (r=0.68, p<0.001). These
studies establish measurable effect sizes for stress-induced thermal changes.

Current platform specifications enable detection of documented thermal stress signatures. The Topdon
TC-series camera provides 256×192 pixel resolution with ±0.1°C radiometric accuracy across the 8–14
μm wavelength range. At 25 Hz frame rate, the camera captures thermal response dynamics with
sufficient temporal resolution to track vasoconstriction onset (typically 2–5 seconds
post-stimulus).

**Specific thermal stress indicators target measurable physiological responses:**

- Nose-tip region-of-interest (ROI) tracking focuses on documented vasoconstriction responses in the
  nasal alae and tip
- Periorbital ROI monitoring captures forehead warming documented in sympathetic activation studies
- Temperature gradient analysis between nose and forehead regions quantifies the characteristic
  cooling–warming pattern during stress responses
- Breathing thermal signatures around the nostrils provide respiratory rate estimation from exhaled
  air temperature cycles

**Environmental controls address thermal imaging challenges in laboratory settings:**

- Ambient temperature maintained at 22 ± 1°C prevents thermoregulatory confounds
- Controlled lighting (LED panels, minimal infrared emission) avoids thermal interference
- Face positioning at 0.8–1.2 metre distance from camera ensures adequate spatial resolution (nose
  ROI spans 8–12 pixels)
- Pre-session thermal baseline recording (2 minutes) establishes individual temperature ranges
  before stress testing

---

## Chapter 3: Requirements and Analysis

### Problem Statement

The system tackles the challenge of contactless physiological monitoring by synchronously collecting
wearable GSR measurements and remote sensory data. Unlike traditional GSR, which needs skin-contact
sensors, this platform provides ground-truth GSR signals alongside contactless data (thermal
imagery, RGB video), aiding research on predicting skin conductance from non-invasive cues.

It combines thermal cameras, video, inertial sensors, and GSR to create a comprehensive multi-modal
dataset for stress and emotion analysis. Focusing on temporal precision and data integrity, the
system captures and aligns subtle physiological responses across modalities. Ultimately, the goal is
to facilitate experiments that correlate a participant's physiological responses with visual and
thermal cues, establishing a basis for contactless stress detection.

### Requirements Structure and Approach

Requirements were developed through an iterative, research-driven process. High-level objectives,
such as "synchronised GSR and video recording," were identified from project goals and refined with
stakeholder input and hardware constraints. A rapid prototyping methodology was employed: early
system versions were built and tested, and user feedback prompted updates to requirements, including
data encryption and device fault tolerance as new needs arose.

Requirements engineering adhered to IEEE 29148 practices: each requirement has a unique ID and is
classified (functional or non-functional). Implementation and code changes were systematically
traced to specific requirements, ensuring full traceability. This incremental, user-focused approach
began with core research use cases and refined requirements as development insights emerged.

### Functional Requirements

**FR1** – **Multi-Device Sensor Integration:** The system shall support connecting and managing
multiple sensor devices simultaneously. This includes discovering and pairing Shimmer GSR sensors
via Bluetooth (direct to PC or via an Android device). If no physical sensor is connected, the
system shall provide a simulation mode that generates dummy sensor data.

**FR2** – **Synchronised Multi-Modal Recording:** The system shall start and stop data recording
synchronously across all connected devices. Upon "Start Recording", the PC must instruct all Android
devices and any connected sensors to begin capturing GSR, video (RGB), thermal, and other enabled
modalities in parallel. All data streams shall share a common session timestamp to enable later
alignment.

**FR3** – **Time Synchronisation Service:** The system shall synchronise clocks across devices to
ensure time-aligned data. The PC shall run a time synchronisation service (e.g. NTP-like) so that
each Android device periodically calibrates its clock to the PC's reference. The requirement for
temporal accuracy on the order of milliseconds is informed by the sub-millisecond precision
standards set by established frameworks like LabStreamingLayer (LSL).

**FR4** – **Session Management:** The system shall organise recordings into discrete sessions, each
with a unique ID or name. The user can create a new session (which the system timestamps) and later
terminate it. On session start, the PC creates a directory and a metadata file; on session end, it
finalises metadata (including start/end times and duration). Only one session may be active at a
time.

**FR5** – **Data Recording and Storage:** For each session, the system shall record all enabled
sensor and video/thermal data streams. Specifically: (a) GSR and other physiological channels from
the Shimmer sensor(s) at 128 Hz, and (b) video (≥1920×1080, 30 FPS) and thermal data from each
Android. Sensor readings shall stream to the PC in real time and be written to local CSV files
immediately.

### Non-Functional Requirements

**NFR1** – **Performance:** The system shall process data in real time with minimal latency. It must
support at least 128 Hz sensor sampling and 30 FPS video recording concurrently without loss or
buffering.

**NFR2** – **Temporal Accuracy:** The system shall maintain clock synchronisation accuracy on the
order of milliseconds or better. The built-in time server and sync protocol must keep timestamp
differences across devices within ~5 ms during recording.

**NFR3** – **Reliability:** The system shall be robust to interruptions and failures. If a sensor or
network link fails, other recordings continue unaffected, and already-recorded data remain
preserved.

**NFR4** – **Security:** The system shall secure all communications and data. Network links (
PC–Android) shall use encryption (e.g. TLS) and authentication tokens to prevent unauthorised
devices.

**NFR5** – **Usability:** The system shall be easy to use by researchers without software expertise.
The PC GUI shall have clear controls (start/stop, device list) and indicators (recording, battery,
status).

---

### Conclusion

These three chapters establish the foundational research context, literature review, and
requirements analysis for the multi-modal physiological sensing platform. The work demonstrates a
rigorous academic approach to developing a specialized research instrument that addresses current
gaps in contactless GSR measurement through synchronized multi-modal data collection.

The platform's design is informed by established frameworks like LabStreamingLayer and PsychoPy,
ensuring compatibility with existing research methodologies while providing novel capabilities for
thermal-visual-physiological data fusion. The comprehensive requirements specification provides a
clear roadmap for implementation and validation of this research-grade data acquisition system.
