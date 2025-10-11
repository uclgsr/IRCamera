# Chapter 6: Proposed Future System Enhancements

## Figure 6.1: Future Expansion Roadmap

```mermaid
graph LR
    A[Current Platform<br/>GSR + Thermal + RGB] --> B[Add Physiological Sensors<br/>ECG, EMG, Respiration]
    A --> C[Edge Analytics<br/>On-device Feature Extraction]
    A --> D[Cloud Synchronization<br/>Secure Dataset Repository]

    B --> B1[BLE Expansion<br/>Multi-sensor hub]
    B --> B2[Hardware Trigger Module<br/>GPIO sync]
    C --> C1[Real-time Stress Index<br/>Hybrid model]
    C --> C2[Adaptive Sampling<br/>Power-aware capture]
    D --> D1[Research Portal<br/>Role-based access]
    D --> D2[Automated Annotation<br/>LLM-assisted labeling]

    classDef current fill:#d0f0c0,stroke:#2e7d32,stroke-width:2px
    classDef expansion fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    class A current
    class B,C,D,B1,B2,C1,C2,D1,D2 expansion
```

Roadmap highlights near-term expansions: integrating additional physiological sensors, deploying
edge analytics, and enabling cloud-based collaboration.
