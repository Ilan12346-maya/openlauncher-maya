# Key Improvements

### Performance
*   **Next-Gen Icon Engine:** Replaced legacy image handling with **Glide**, enabling high-performance asynchronous loading, localized disk caching, and significantly reduced RAM usage.
*   **Optimized Startup:** Implemented asynchronous app loading and caching strategies to ensure near-instant interaction and prevent UI stutters.
*   **Smoother Interactions:** Refined `CellContainer` touch logic and optimized icon pack processing (O(N) complexity) to minimize main thread overhead.

### Modernization
*   **Architecture Overhaul:** Decoupled monolithic `HomeActivity` into specialized managers (`ReceiverManager`, `AppLauncher`, `PermissionManager`) and centralized backup logic.
*   **Android 14 Ready:** Updated codebase for modern Android standards, including full migration to `OnBackPressedDispatcher`, `WindowInsetsController`, and modern permission flows.
*   **Clean Codebase:** Fully migrated from ButterKnife to **ViewBinding**, replaced deprecated `AsyncTask` with `ExecutorService`, and resolved resource conflicts for enhanced stability.

---

![graphic](https://raw.githubusercontent.com/OpenLauncherTeam/openlauncher/master/fastlane/metadata/android/en-US/images/featureGraphic.png)

# Personal OpenLauncher Fork

This fork was created because I could not find a launcher that met my specific requirements. I decided to fork OpenLauncher to create a version tailored to my personal preferences, specifically optimized for an 8.5" smartphone with a minimum DPI of 750 in developer options.

While this project is primarily for my own use, I am making it available for anyone who might share similar requirements or preferences. The codebase has been updated to reflect current standards, and several new features have been implemented.

---

### Project discontinued (Original Notice)
The OpenLauncher project is discontinued. The original project author has left the project many years ago and didn't come back. Also the project has not seen active development for years, or at least no considerable interest by the community to contribute. Technology doesn't stop in time and the Android platforms regulary gets new things that are expected by launchers. As well active development is require to support newer Android versions. I ([@gsantner](https://github.com/gsantner)) have been taking the time to keeping the project alive for many years, from now on I stop doing that. Move on and don't consider this codebase as it's dated. You likely are better off starting something fresh or fork a other more recent launcher project.

The suggestion is to use a different, active maintained launcher. If you are happy still with OpenLauncher as it is, feel free to keep using it as long you like to. If the latest release does not work, feel free to try a older version. BUT don't expect any future project changes. The last release can be downloaded from [F-Droid](https://f-droid.org/repository/browse/?fdid=com.benny.openlauncher).

All the best, Gregor.

### Description

This is an open source launcher project for Android devices that has been built completely from scratch. The main goal of this launcher is to find a healthy medium between customization and simplicity. At this point in time it implements most features required in a typical launcher but could benefit greatly from some general polish. If you would like to help out feel free to submit issues or ask about submitting a pull request with a feature you want to see in the launcher.

<div style="display:flex;">
<a href="https://f-droid.org/repository/browse/?fdid=com.benny.openlauncher">
    <img height="80" alt="Get it on F-Droid" src="https://f-droid.org/badge/get-it-on.png">
</a>
<a href="https://play.google.com/store/apps/details?id=com.benny.openlauncher">
    <img height="80" alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
</div>

<div style="display:flex;">
    <img src="https://raw.githubusercontent.com/OpenLauncherTeam/openlauncher/master/assets/screenshots.png">
</div>

### Status

If your instance is crashing frequently please update the app and reset the data and settings before creating an issue. This project is not actively developed at the moment since all of the main contributors either started working on other projects or find the current state of the launcher sufficient for daily use. If you would like to see a change please realize that it may not get added at all unless someone decides to write the functionality. Pull requests are welcome from anyone! Please ask about large features first, we can help navigate the codebase and talk about where best to add the functionality.

### Features

  * Paged desktop
  * Dock
  * Drag and drop
  * Hide apps
  * Scrollable background
  * Search bar
  * Icon packs

### Contributions

The project is always open for contributions and accepts pull requests. Please use the _auto reformat feature_ in Android Studio before sending a pull request. Translations can be contributed on GitHub. You can use Stringlate to translate the project directly on your Android phone. It allows you to post the translations on GitHub with little effort.

### Resources

  * Team: [bennykok](https://github.com/BennyKok) | [dkanada](https://github.com/dkanada) | [gsantner](https://github.com/gsantner)
  * Project: [Changelog](/CHANGELOG.md) | [License](/LICENSE)
  * F-Droid: [Metadata](https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/com.benny.openlauncher.txt) | [Page](https://f-droid.org/packages/com.benny.openlauncher/) | [Wiki](https://f-droid.org/wiki/page/com.benny.openlauncher) | [Build](https://f-droid.org/wiki/page/com.benny.openlauncher/lastbuild)
 
### License

The app is licensed with Apache 2.0.