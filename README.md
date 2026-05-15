# SafeChat 🛡️
### *"An AI That Reads Between The Lines"*

A grooming-aware Android chat application built for female online safety. SafeChat embeds real-time AI-assisted grooming risk detection directly within a native messaging environment — protecting users at the point where risk actually occurs: inside the conversation itself.

> **Course:** CCS 3600 — Artificial Intelligence | Universiti Putra Malaysia
> **Group:** LAW STUDENTS

---

## Features

### 💬 Real-Time Chat with Grooming Detection
Every outgoing message is silently screened by a weighted keyword scoring engine before it is delivered. Messages are classified into three levels:

| Level | Token Score | Action |
|-------|-------------|--------|
| ✅ Safe | Below 20 | Message sent normally |
| ⚠️ Warning | 20 – 49 | Alert shown; user may proceed |
| 🚫 Blocked | 50+ | Message blocked from sending |

### 🤖 Safety Hub (AI Assistant)
A dedicated screen powered by the **Google Gemini API**. Users can ask the AI assistant about grooming warning signs, seek safety guidance, or get help preparing a report — all without leaving the app. Three modes are available:
- **Q&A** — general grooming awareness questions
- **Policy Analysis** — understanding relevant laws and policies
- **Report Assistant** — guided help drafting a formal report

### 📊 Risk Dashboard
A per-contact risk dashboard aggregates token scores across an entire conversation history, giving users a longitudinal view of whether a contact's communication pattern reflects escalating grooming behaviour over time.

### 📋 Report Flow
Users can flag a conversation directly from the chat screen. The conversation evidence is forwarded to the Gemini API for AI-assisted analysis, and a summary is presented to the user before formal submission.

### 🔐 Authentication
Sign up and sign in via email and password, with user profiles and profile images stored in Firebase Firestore.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java (Android SDK) |
| UI | XML layouts, View Binding, RecyclerView |
| Database | Firebase Firestore |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| AI / LLM | Google Gemini API (`gemini-flash-latest`) |
| HTTP Client | Retrofit 2 |
| Markdown Rendering | Markwon |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 16 (API 36) |

---

## Project Structure

```
app/src/main/java/com/example/chatapp/
├── activities/
│   ├── SignInActivity.java       # Entry point / login
│   ├── SignUpActivity.java       # Registration
│   ├── MainActivity.java         # Recent conversations list
│   ├── ChatActivity.java         # Chat screen + real-time detection
│   ├── SafetyHubActivity.java    # Gemini AI assistant
│   ├── RiskDashboardActivity.java# Per-contact risk scores
│   └── ReportActivity.java       # Report submission flow
├── adapters/
│   ├── ChatAdapter.java
│   ├── RecentConversationsAdapter.java
│   ├── SafetyHubAdapter.java
│   └── UsersAdapter.java
├── models/
│   ├── ChatMessage.java
│   ├── SafetyHubMessage.java
│   └── User.java
├── utilities/
│   ├── GroomingDetector.java     # Keyword scoring engine
│   ├── Constants.java            # API keys, Firestore keys
│   ├── PreferenceManager.java
│   ├── AccessTokenManager.java
│   └── MarkdownUtils.java
├── firebase/
│   └── MessagingService.java     # FCM push notifications
└── network/
    ├── ApiClient.java
    └── ApiService.java
```

---

## Setup & Installation

### Prerequisites
- Android Studio (latest stable)
- Android device or emulator running API 24+
- A Firebase project
- A Google Gemini API key

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/your-username/Anti-Gromming-Detection-ChatApp.git
cd Anti-Gromming-Detection-ChatApp
```

**2. Connect Firebase**
- Create a project at [Firebase Console](https://console.firebase.google.com/)
- Enable **Firestore Database** and **Cloud Messaging**
- Download `google-services.json` and place it in `app/`

**3. Add your Gemini API key**

Open `app/src/main/java/com/example/chatapp/utilities/Constants.java` and replace:
```java
public static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE";
```
Get a key at [Google AI Studio](https://aistudio.google.com/app/apikey).

> ⚠️ **Security note:** Never commit a real API key to a public repository. Use `local.properties` or Android `BuildConfig` fields with `gitignore` in production.

**4. Build and run**

Open the project in Android Studio and click **Run**, or build an APK via:
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

---

## How the Grooming Detection Works

The `GroomingDetector` class implements a weighted keyword lexicon. Each term carries a predefined token weight based on its severity as a grooming indicator, informed by criminological research and validated against the PAN 2012 Sexual Predator Identification Dataset.

**Example weights:**
```
"nude"              → 50 tokens   (High severity)
"meet alone"        → 40 tokens   (High severity)
"private photo"     → 40 tokens   (High severity)
"our secret"        → 30 tokens   (Medium-high)
"don't tell"        → 30 tokens   (Medium-high)
"webcam"            → 20 tokens   (Medium)
"sweetie"           → 10 tokens   (Low-medium)
```

Token scores accumulate per message. A single message scoring ≥ 50 is blocked entirely.

---

## Known Limitations

- Keyword-based detection can be circumvented by deliberate misspellings or coded language
- The system does not perform contextual disambiguation — legitimate uses of flagged terms may trigger warnings
- The Gemini Safety Hub operates on user-initiated queries only, not passive monitoring
- Currently English-language only; multilingual support is a planned future enhancement

---

## Future Work

- Supervised ML classifier layer to reduce false positives
- Contextual NLP model to disambiguate keyword matches
- Multilingual keyword lexicons
- Integration with formal reporting authorities or NGO support pipelines
- Appeal mechanism for contested blocked messages

---

## Research References

1. Borj, P. R., Raja, K., & Bours, P. (2022). Online grooming detection: A comprehensive survey. *Knowledge-Based Systems*, 259, 110039.
2. Nelatoori, K. B., & Kommanti, H. B. (2022). Attention-Based Bi-LSTM Network for Abusive Language Detection. *IETE Journal of Research*.
3. Prosser, E., & Edwards, M. (2024). Helpful or Harmful? Exploring the Efficacy of LLMs for Online Grooming Prevention. *ArXiv*.
4. Street, J., & Olajide, F. (2023). Evaluating a Non-platform-specific OCR/NLP system to detect Online Grooming. *ICCWS*.
5. Street, J., Ihianle, I., Olajide, F., & Lotfi, A. (2024). Enhanced Online Grooming Detection Employing Context Determination and Message-Level Analysis. *ArXiv*.
