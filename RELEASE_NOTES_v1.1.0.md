# Prayer Tracker v1.1.0 - Release Notes

## 🎉 Major Feature Update!

This release brings **11 new features** to Prayer Tracker, making it more comprehensive, beautiful, and user-friendly.

---

## ✨ What's New

### 🕋 Prayer & Ramadan Features
- **Imsak Time Card**: A prominent red-highlighted card in Ramadan mode showing the exact time to stop eating (20 minutes before Fajr)
- **Jumu'ah Prayer**: Automatically displays Jumu'ah prayer on Fridays at Dhuhr time, with Dhuhr hidden on Fridays
- **Enhanced Islamic Events**: 20+ Islamic events with both English and Arabic names:
  - Eid al-Fitr (3 days)
  - Eid al-Adha (4 days)
  - Day of Ashura
  - Mawlid an-Nabi
  - Isra & Mi'raj
  - Laylat al-Qadr
  - Day of Arafah
  - And many more...

### 🔊 Audio Features
- **Adhan Audio Playback**: Play Islamic call to prayer audio when prayer notifications fire
- **Volume Control**: Adjust Adhan volume (0-100%)
- **Reciter Selection**: Choose your preferred Adhan reciter (Mishary Rashid, Al-Sudais, etc.)

### 📤 Sharing & Accessibility
- **WhatsApp Sharing**: Share prayer times as a beautiful image with:
  - Date (Gregorian and Hijri)
  - Location
  - All prayer times
  - Islamic event name (if applicable)
- **App Shortcuts**: Quick access from home screen to:
  - Prayer Times
  - Qibla Compass
  - Tasbih Counter
  - Ramadan Mode

### 🎨 UI & Design Improvements
- **Enhanced Compass**: Material 3 Expressive design with:
  - Gradient ring
  - Kaaba cube symbol
  - Cardinal directions (N, E, S, W)
  - Improved readability
- **New Color Scheme**: Custom colors for better visual hierarchy
- **Modern Typography**: Better font styling throughout

### 🌍 Localization
- **Arabic** (العربية)
- **Urdu** (اردو)
- **Malay** (Bahasa Melayu)
- **Indonesian** (Bahasa Indonesia)
- Full RTL support for Arabic and Urdu

### ⚙️ Technical Improvements
- Prepared project structure for Wear OS support
- Optimized prayer calculations
- Reduced unnecessary recompositions
- Better caching for improved performance

---

## 📊 Statistics

- **25 files** changed
- **+1,319 lines** of code added
- **11 major features** implemented
- **4 languages** supported

---

## 📋 Feature Breakdown

### Core Features (3)
1. ✅ Imsak Time in Ramadan Mode
2. ✅ Jumu'ah Prayer on Fridays
3. ✅ Enhanced Islamic Events

### Audio & Notifications (1)
4. ✅ Adhan Audio Playback

### Sharing & Shortcuts (2)
5. ✅ WhatsApp Sharing (Image)
6. ✅ App Shortcuts

### UI/UX Improvements (2)
7. ✅ Enhanced Compass Visual
8. ✅ Material 3 Expressive Design

### Localization (1)
9. ✅ Multi-Language Support (4 languages)

### Technical (2)
10. ✅ Wear OS Support (Prepared)
11. ✅ Performance Optimization

---

## 🚀 How to Use New Features

### Imsak Time
1. Open Ramadan Mode
2. Look for the **red card** at the top
3. It shows the time to stop eating (20 min before Fajr)

### Jumu'ah Prayer
1. On Fridays, open Prayer Times
2. You'll see **Jumu'ah** instead of Dhuhr
3. Prayer times automatically adjust for Friday

### Adhan Audio
1. Go to Settings → Prayer Notifications
2. Enable "Play Adhan Audio"
3. Adjust volume as needed
4. When prayer time arrives, Adhan will play automatically

### Share Prayer Times
1. Open Prayer Times screen
2. Tap the **Share** button (top right)
3. Select WhatsApp or any other app
4. Beautiful image with all prayer times will be shared

### App Shortcuts
1. Long-press the Prayer Tracker app icon
2. Select from quick actions:
   - Prayer Times
   - Qibla Compass
   - Tasbih Counter
   - Ramadan Mode

### Change Language
1. Change your device language to Arabic, Urdu, Malay, or Indonesian
2. The app will automatically switch to that language

---

## ⚠️ Important Notes

### Adhan Audio Files
To enable Adhan audio playback, you need to add MP3 files to `app/src/main/res/raw/`:
- `adhan_fajr.mp3`
- `adhan_dhuhr.mp3`
- `adhan_asr.mp3`
- `adhan_maghrib.mp3`
- `adhan_isha.mp3`
- `adhan_generic.mp3`

**Recommended sources:**
- Mishary Rashid Alafasy
- Abdul Rahman Al-Sudais
- Saad Al-Ghamdi

### Wear OS Support
Wear OS module is prepared but not yet implemented. To enable:
1. Uncomment `include(":wear")` in `settings.gradle.kts`
2. Create the Wear module
3. Add Wear-specific code

---

## 🐛 Known Issues & Limitations

- Adhan audio requires MP3 files to be added manually
- Wear OS support is prepared but not fully implemented
- Some features may require location permissions
- Notification tone picker requires system ringtone picker

---

## 📥 Download

- **GitHub Releases**: [PrayerTracker-v1.1.0.apk](https://github.com/sinanalizz123-max/prayer-tracker/releases/tag/v1.1.0)
- **Version**: 1.1.0 (versionCode: 5)
- **Minimum SDK**: 26 (Android 8.0+)

---

## 💬 Feedback

We welcome your feedback! Please report any issues or suggestions:
- Open an issue on GitHub
- Contact the maintainer

---

## 🙏 Credits

- **Adhan Library**: [batoulapps/adhan-java](https://github.com/batoulapps/adhan-java)
- **Material 3**: [Google Material Design](https://m3.material.io/)
- **Jetpack Compose**: [Android Compose](https://developer.android.com/jetpack/compose)

---

**Released on**: January 2026  
**Version**: 1.1.0  
**Commit**: d37b458
