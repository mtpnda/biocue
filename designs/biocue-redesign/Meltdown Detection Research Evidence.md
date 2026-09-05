# Research Evidence Base: Wearable Detection of Sensory Overload & Meltdowns in Autism

*Compiled: August 2026 — supporting documentation for a smartwatch-based detection algorithm (target device: Samsung Galaxy Watch 4 Classic)*

---

## 1. Core Claim: Meltdowns/Aggression Can Be Predicted from Wrist-Worn Biosensors

### Goodwin et al. (2019) — Foundational Study
- **Citation:** Goodwin, M.S., Mazefsky, C.A., Ioannidis, S., Erdogmus, D., & Siegel, M. (2019). Predicting aggression to others in youth with autism using a wearable biosensor. *Autism Research*, 12(8), 1286–1296. DOI: 10.1002/aur.2151
- **Design:** 20 autistic youth (ages 6–17, 85% minimally verbal), 69 naturalistic observation sessions in a specialized inpatient psychiatry unit, wearing the Empatica E4 wristband.
- **Signals used:** Cardiovascular (PPG-derived heart rate and inter-beat intervals), electrodermal activity (EDA, 4 Hz), 3-axis accelerometry (32 Hz).
- **Finding:** Antecedent physiological and motion data predicted aggression to others with relatively high accuracy (~84% one minute in advance in early reporting). The device was well tolerated; no participants withdrew.
- **Relevance to project:** Establishes that the HR + IBI + movement combination available on consumer smartwatches carries a predictive signal.

### Imbiriba, Demirkaya et al. (2023) — Large Multi-Site Replication
- **Citation:** Imbiriba, T., Demirkaya, A., et al. (2023). Wearable biosensing to predict imminent aggressive behavior in psychiatric inpatient youths with autism. *JAMA Network Open*.
- **Design:** Four inpatient psychiatric hospitals; 429 observation sessions totaling 497 hours; 6,665 documented aggressive behaviors (59.8% self-injury, 31.0% emotion dysregulation, 9.3% aggression toward others).
- **Finding:** Logistic regression predicted aggressive behavior 3 minutes before onset with mean AUROC 0.80 (95% CI 0.79–0.81). Neural networks and SVMs performed comparably, but logistic regression was the most consistent across experiments.
- **Relevance to project:** (a) Validates the 1–3 minute early-warning window; (b) supports starting with simple, interpretable models (logistic regression) rather than deep learning.

---

## 2. Heart Rate Variability (HRV) as a Stress/Arousal Marker in Autism

### Cheng et al. (2020) — Meta-Analysis (34 Studies)
- **Citation:** Cheng, Y.-C., et al. (2020). Heart rate variability in individuals with autism spectrum disorders: A meta-analysis. *Research in Autism Spectrum Disorders / Neuroscience & Biobehavioral Reviews*. DOI: 10.1016/j.neubiorev.2020.10.007 (ScienceDirect S014976342030525X)
- **Findings:**
  - Autistic individuals showed significantly lower baseline HRV for parasympathetic indices (Hedges' g = −0.52, p < 0.0001) and respiratory sinus arrhythmia (g = −0.59, p = 0.001).
  - Social stress produced a marked reduction in HRV reactivity in autistic individuals.
  - Reduced HRV, especially low RSA under social stress, is proposed as a candidate biomarker.
- **Relevance to project:** (a) HRV drop is a valid arousal signal in this population; (b) baseline HRV differs from neurotypical norms — **detection thresholds must be personalized per individual**, not taken from population norms.

### General Stress Evidence
- Systematic review of 17 studies in medical professionals found HRV metrics (SDNN, RMSSD, pNN50, LF/HF) change in a predictable pattern under acute real-world stress (PMC9974008). Supports RMSSD as the real-time computation target from the watch's inter-beat-interval stream.

---

## 3. Electrodermal Activity (EDA) as an Arousal Precursor

*(Context: EDA is not available on Galaxy Watch 4 hardware; documented here as the evidence origin and as an upgrade path.)*

### Poh, Swenson & Picard (2010) — Foundational Wearable EDA Validation
- **Citation:** Poh, M.Z., Swenson, N.C., & Picard, R.W. (2010). A wearable sensor for unobtrusive, long-term assessment of electrodermal activity. *IEEE Transactions on Biomedical Engineering*, 57(5), 1243–1252.
- **Relevance:** Origin of the wearable-EDA field; this MIT Media Lab line of work (Rosalind Picard) began specifically to help nonverbal autistic children communicate stress, and led to Empatica devices.

### Frontiers in Psychiatry (2019) — EDA and Problem Behavior Feasibility
- **Citation:** Examining the association between electrodermal activity and problem behavior in severe autism spectrum disorder: A feasibility study. *Frontiers in Psychiatry*, 10:654. DOI: 10.3389/fpsyt.2019.00654
- **Design:** EDA sampled from 8 individuals with severe ASD in naturalistic school programming.
- **Finding:** Arousal measures may provide early indication of subsequent problem behavior, but between-individual variability in EDA patterns can limit predictive ability.
- **Relevance:** EDA adds signal but is noisy and individually variable — its absence on the Galaxy Watch 4 is not fatal to the design, since HR/HRV/motion carried the predictive weight in the Goodwin studies.

---

## 4. Accelerometry: Detecting Repetitive/Stereotypical Movement

### Rad & Goodwin et al. (2018)
- **Citation:** Rad, N.M., et al. Deep learning for automatic stereotypical motor movement detection using wearable sensors in autism spectrum disorders. *Signal Processing* (preprint arXiv:1709.05956).
- **Finding:** Wearable accelerometer data can automatically detect stereotypical motor movements (rocking, hand-flapping) in autistic individuals.
- **Relevance to project:** Validates the FFT-based "repetitiveness" feature (0.5–4 Hz band power) in the algorithm's feature-extraction stage, and the use of movement signatures to distinguish stimming/agitation from ordinary locomotion.

---

## 5. Real-World, Single-Subject ML Classification

### Agitated Behaviors Detection in Children with ASD Using Wearable Data (2023)
- **Citation:** Springer LNCS, 2023. DOI: 10.1007/978-3-031-43950-6_8
- **Design:** Six months of Empatica E4 data (blood volume pulse, EDA, acceleration) from a 9-year-old autistic child during everyday activities.
- **Finding:** XGBoost achieved the highest accuracy, precision, recall, and F1 for detecting agitated behaviors among the classifiers tested (SVM, Random Forest, XGBoost, TabNet).
- **Relevance to project:** A template for the per-person learning loop — once caregiver-labeled episodes accumulate, a personalized gradient-boosted model can replace the initial rule-based scoring.

---

## 6. Evidence-to-Design Mapping (Summary Table)

| Algorithm component | Supporting evidence |
|---|---|
| HR elevation + HRV (RMSSD) drop as primary features | Goodwin 2019; Imbiriba 2023; Cheng 2020 meta-analysis |
| 1–3 minute early-warning alert window | Imbiriba 2023 (3 min, AUROC 0.80); Goodwin 2019 (1 min, ~84%) |
| Personalized per-individual baselines | Cheng 2020 (atypical baseline HRV in ASD); Frontiers 2019 (high inter-individual EDA variability) |
| Start with logistic regression, not deep learning | Imbiriba 2023 (logistic regression most consistent performer) |
| Repetitive-movement feature via accelerometer FFT | Rad & Goodwin 2018 |
| Per-person ML upgrade after labeled data accumulates | Springer 2023 single-subject XGBoost study |
| Wrist wearables tolerated by minimally verbal youth | Goodwin 2019 (no withdrawals, usable data in all cases) |

---

## 7. Known Evidence Gaps (Disclose in Any Proposal)

1. **Setting gap:** Nearly all predictive studies were conducted in inpatient psychiatric or school settings with profoundly autistic youth — community/home validation with consumer devices is limited.
2. **Device gap:** The evidence base used research-grade sensors (Empatica E4/EmbracePlus); consumer smartwatch PPG is noisier, especially during movement.
3. **Population gap:** Most participants were children/adolescents with profound autism; generalization to autistic adults or those with lower support needs is not yet established.
4. **Outcome gap:** Studies predicted aggression/self-injury onset; prediction of *sensory overload* specifically (before behavioral escalation) is inferred, not directly validated.

---

*Disclaimer: This document is for research and product-development reference only and does not constitute medical advice.*
