# BiteSense: On-Device Arthropod Bite Detection and First-Aid Assistance with a Fine-Tuned Gemma 3n

**Authors:** Ağabey Alioğlu · Can Zağnos · Dorukhan Arslan · Ece Ünal · Yunus Önür  

---

## 📌 Overview

BiteSense is an **Android application** designed to provide **instant, offline diagnosis of arthropod bites**—including ticks, mosquitoes, spiders, and more—while offering actionable **first-aid guidance**.

---

## 🌍 Motivation

Vector-borne diseases (VBDs) kill approximately **700,000 people every year** and account for **17% of the global infectious disease burden** [1].  
In **Turkey**, **Crimean-Congo Hemorrhagic Fever (CCHF)** poses a serious risk, particularly in **rural livestock-farming provinces** such as Sivas, Tokat, Yozgat, Gümüşhane, and Tunceli.

Early and accurate bite identification is critical, but access to **specialists** and **internet-dependent AI tools** is limited in these regions. BiteSense bridges this gap by running advanced multimodal AI **fully offline**.

---

## Installation

### APK Download

The pre-built APK is available for download at: https://commencis-my.sharepoint.com/:u:/p/agabey_alioglu/EeibALoMOcRAqt8bv2F9GhwBLnS3BWDTgVjEUWUo4J4pQw?e=13Um4c

**Important:** When accessing the link, please select "Open with browser" if prompted.

### Backend Configuration

For security purposes, the backend URL is not included in the repository or APK. The backend URL can be provided upon request for testing the application's functionality.

### Model Configuration

Due to repository size constraints, the Gemma 3n model must be obtained separately:

1. Download the model from: https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/tree/main
2. Place the downloaded file in: `android-app/app/src/main/assets/`
3. Rename the file to: `gemma-3n-e2b.task`