### v0.9.6 (Maya Edition)
* Massive performance optimizations for icon loading and scaling (reduced UI thread overhead).
* Implementation of asynchronous icon caching and disk storage to prevent UI stutters.
* Asynchronous loading of saved apps during startup for near-instant interaction.
* Optimized icon pack processing (XML parsing O(N^2) -> O(N)).
* Modernized desktop grid design using clean crosses at intersections.
* Enhanced Desktop Edit Mode: Page 0 and Infinite Scrolling are now temporarily disabled while editing.
* Restricted Edit Mode exit: Single taps no longer exit the mode; requires Back button or Long-press.
* Launcher is now completely hidden from the "Recent Apps" list for a more integrated feel.
* Fixed IndexOutOfBoundsException and page count logic errors during desktop mode transitions.

### v0.9.4
* Added a new "Debug" section in settings with detailed logging and a built-in log viewer.
* Fixed a race condition where icons could disappear after a long press during activity restarts.
* Improved app launch reliability and added fallback mechanisms.
* Increased dock bottom margin to 16dp for better visual spacing.
* Removed redundant vertical offsets in iOS dock style.

### v0.5.1
* fix incorrect label clipping
* improve free space calculation for widgets
* slight changes to settings page
* fix crash related to group items
* maximum grid size set to twenty
* labels can be edited from the drag menu

### v0.5.0
* Settings have been redesigned and will be reset
* Add ability launch app with gesture
* Custom search bar layout without requiring google services
* Option to lock the desktop from changes
* Lots of bug fixes
* Layout fixes