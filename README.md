# OTP Authentication App

A simple passwordless login app built with Jetpack Compose for an Android assignment. The app lets users log in using their email and a 6-digit OTP, then shows a session screen with a live timer.

## How to Run

1. Clone this repo
2. Open in Android Studio
3. Make sure `google-services.json` is in the `app/` folder
4. Hit Run - that's it!

---

## 1. OTP Logic and Expiry Handling

The OTP system is pretty straightforward. Here's how it works:

### Generation
- When the user taps "Send OTP", I generate a random 6-digit number
- This gets stored along with the current timestamp
- For demo purposes, I show the OTP on screen (in a real app, this would be sent via email)

### Validation Rules
- OTP is valid for **60 seconds** only
- User gets **3 attempts** to enter the correct OTP
- After 3 wrong attempts, they have to request a new one

### Expiry Check
I simply compare the current time with when the OTP was generated:
```kotlin
fun isExpired(): Boolean {
    val elapsedSeconds = (System.currentTimeMillis() - generatedAt) / 1000
    return elapsedSeconds >= 60
}
```

### Resend Behavior
When user requests a new OTP:
- The old one is automatically invalidated (replaced in the map)
- Attempt counter resets to 0
- Timer restarts from 60 seconds
- There's a 30-second cooldown before they can resend again

---

## 2. Data Structures Used

### HashMap for OTP Storage
```kotlin
private val otpStore: MutableMap<String, OtpData> = mutableMapOf()
```
I went with a HashMap because:
- Quick O(1) lookups by email
- Easy to replace old OTP when generating new one (just overwrite the key)
- Simple key-value structure fits the use case perfectly

### Data Class for OTP Info
```kotlin
data class OtpData(
    val otp: String,
    val generatedAt: Long,
    val attemptCount: Int = 0
)
```
Using a data class made sense because:
- It's immutable by default which prevents accidental modifications
- The `copy()` function is handy for updating attempt count
- Clean and readable

### Sealed Class for UI States
```kotlin
sealed class AuthState {
    data class Login(...) : AuthState()
    data class Otp(...) : AuthState()
    data class Session(...) : AuthState()
}
```
I used sealed classes for the screen states because:
- The compiler forces me to handle all cases in `when` expressions
- Makes navigation between screens type-safe
- Each state can hold its own specific data

### StateFlow for State Management
Chose StateFlow over LiveData because:
- Works better with Compose
- Lifecycle-aware automatically
- Cleaner one-way data flow

---

## 3. External SDK Choice - Firebase Analytics

I chose **Firebase Analytics** for the external SDK requirement.

### Why Firebase?
1. **Already had it set up** - The project already had google-services.json configured
2. **Free to use** - No cost for the features I needed
3. **Easy integration** - Just add the dependency and initialize
4. **Good documentation** - Easy to figure out

### What I'm Logging
| Event | When it fires |
|-------|--------------|
| `otp_generated` | User requests OTP |
| `otp_validation_success` | Correct OTP entered |
| `otp_validation_failure` | Wrong OTP / expired / max attempts |
| `user_logout` | User logs out |

I also made sure to mask the email before logging for privacy (e.g., `te***@gmail.com`).

---

## 4. What I Used GPT For vs What I Did Myself

### Used GPT For:

- **Syntax help** - Sometimes I forgot the exact Compose modifier syntax
- **Regex for email validation** - I always have to look this up anyway
- **String formatting** - The `String.format()` for timer display

### Understood and Implemented Myself:
- **Overall architecture** - I decided to use ViewModel + StateFlow pattern based on what I learned in class
- **OTP logic design** - Figured out how to structure the expiry check, attempt counting, and resend flow
- **Data structure choices** - Chose HashMap for O(1) lookups, understood why sealed classes help
- **Timer implementation** - Used coroutines with delay() to avoid blocking the main thread
- **State management** - Designed how state flows from ViewModel to UI
- **Edge case handling** - Thought through what happens on expired OTP, max attempts, screen rotation, etc.
- **Firebase integration** - Added the SDK, created the wrapper class, decided what events to log

### My Learning Process:
I already knew the basics of Compose and ViewModel from coursework. For this assignment, I had to figure out:
- How to properly use `viewModelScope` for coroutines (read the docs)
- The difference between `remember` and `rememberSaveable` (watched a video)
- How Firebase Analytics event logging works (followed their quickstart guide)

---

## Project Structure

```
app/src/main/java/com/example/project1/
├── MainActivity.kt           # Entry point
├── analytics/
│   └── AnalyticsLogger.kt    # Firebase wrapper
├── data/
│   ├── OtpData.kt            # OTP data model
│   └── OtpManager.kt         # OTP business logic
├── ui/
│   ├── LoginScreen.kt        # Email input
│   ├── OtpScreen.kt          # OTP verification
│   ├── SessionScreen.kt      # Logged in screen
│   └── theme/                # Compose theming
└── viewmodel/
    ├── AuthState.kt          # UI states
    └── AuthViewModel.kt      # Business logic
```

---

## Features Implemented

- [x] Email + OTP login flow
- [x] 6-digit OTP with 60s expiry
- [x] Max 3 attempts
- [x] Resend OTP (invalidates old one)
- [x] Session screen with live duration timer
- [x] Firebase Analytics integration
- [x] Visual countdown timer (Bonus)
- [x] Sealed UI states (Bonus)
- [x] Retry cooldown - 30s wait before resend (Bonus)

---


Anuj Gupta
