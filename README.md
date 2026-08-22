# Bubble Penetration

📦 **Version:** 1 (1.0)  
⚙️ **Build Tool:** Gradle 9.7.1

## 🤖 Android Configuration

- **Application ID:** org.codeberg.scovillo.bubble
- **Compile SDK:** android-36
- **Min SDK:** 14
- **Target SDK:** 36

## 📱 Description

Bubble Penetration is a fast-paced arcade game built for quick sessions and serious highscore runs.

Colorful bubbles and golden stars drift across the field - your job is to tap the right targets before time runs out.

How it works:

- Collect the currently required color to gain points and time.
- Tap the wrong bubble and you lose precious seconds and the active combo.
- Grab stars for bonus time and extra score.
- Keep your streak alive to trigger combo multipliers up to x16.

What you get:

- Pure reaction-based gameplay with no fluff.
- Increasing speed that keeps every round intense.
- Highscores exactly how you want them: compete on the global leaderboard, connect your own compatible leaderboard, or play completely offline with local scores.
- Fully free and open source.

Every run feels different, and every second matters - how long can you survive?

### Your score, your choice

Want to compete with players around the world? The built-in online leaderboard is ready to go.
Prefer to play among friends or run your own leaderboard? Simply enter its address in the settings.
And if you would rather stay offline, just turn off the online leaderboard - your best scores stay on your device and the bubble popping continues without an internet connection.

The game is built using free and open-source software libraries.

## 🎁 Support Bubble Penetration

If you enjoy Bubble Penetration and would like to support ongoing development, I would really appreciate a voluntary donation.

Donations help with:

- Server and infrastructure costs (for example, the leaderboard)
- Maintenance, bug fixes, and new features
- Long-term support of this free and open-source project
- The app will, of course, remain fully usable without any donation.

[![Donate using Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/scovillo/donate)

[![PayPal](https://www.paypalobjects.com/webstatic/icon/pp50.png)](https://paypal.me/muemmelmaus)

[![GitHub Sponsors](https://img.shields.io/badge/GitHub%20Sponsors-❤️-pink?logo=github&style=flat-square)](https://github.com/sponsors/scovillo)

You can also support the project without donating money:

- Share the app
- Send feedback or bug reports
- Contribute to the project (issues, code, translations)

Thank you for every kind of support! ❤️

## 🛠️ Build Instructions

```bash
./gradlew assembleDebug
```

For a release build:

```bash
./gradlew assembleRelease
```

📚 Dependencies

- org.jetbrains.kotlin:kotlin-stdlib:2.2.10
- androidx.appcompat:appcompat:1.2.0
- androidx.constraintlayout:constraintlayout:2.0.4
- org.jetbrains.kotlin:kotlin-test:2.2.10

📄 License

Bubble Penetration is free and open-source software.

Copyright (C) 2026 Lukas Scheerer

Licensed under the GNU General Public License v3.0.

You should have received a copy of the GNU General Public License along with
this program. If not, see https://www.gnu.org/licenses/.
