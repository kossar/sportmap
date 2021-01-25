"# sportmap" 

School project(Native Mobile Applications)
Tested mainly on Samsung Galaxy S10+. Android 10. Working fine in this device.

Sometimes Android Studio Emulator crashed without informative stacktrace. Restart of Android Studio helped for some time etc.

App created to help to orient in unknown terrain. 

Assignment:

HW03 Final Project - GPS SportMap
Final Deadline 2021-01-03 23:59:59
App like endomondo, nike run, strava, runtastic, etc.
General idea - app, that would help you in unknown terrain with navigation and training.

App is based on Google Maps (or openstreetmap or some other) and built-in GPS functionality.

Display map, and update your location on map display.

Allow toggling of "keep map constantly centered", "keep north-up / direction up / user chosen-up".

When tracking is started, start to draw trail from start to current location.
Allow user to set checkpoints (permanent) and waypoints (single) on track. Ie when adding new waypoint, remove previous.

Calculate and display direct and travelled distance from start, last checkpoint and last waypoint.
Time elapsed from start, overall pace (in minutes per km) in every three sections.

Save all the sessions and all the checkpoints to database.
Allow user to view/delete old sessions - display statistics and track on map.

For controlling the app from lock screen, implement sticky custom layout notification - UI similar to what you have under the map.
When user stops session, ask for confirmation. Do not let to start/stop session from lock-screen.

Constantly(after every position update) update notification to display current info - distances and pace.

GPS listener and notification intents broadcast listener has to be implemented in service, otherwise app cannot be kept running in background.
App has to support rotation (and state has to be restored) - move buttons from bottom to left-or-right screen edge in landscape mode.

Provide on-screen real compass (must be possible to turn on-and-off). Google maps rotation indicator is not a compass.
Provide session export possibility - ie as email attachment for example. File format - gpx.
https://www.topografix.com/gpx_manual.asp (checkpoints as waypoints in gpx format).
Gpx entries have to contain coordinates of every location update and user set WP coordinates  and timestamp (for later track analysis and animation).

Allow creating user account and sync data to central Web-API/Rest backend (provided by teacher).

Change app icon into something more meaningful.

Checkpoint - CP - some predefined marked location on terrain and on paper map. When you did find the CP, mark it's location in app. Gets saved to DB.

Waypoint - WP - temporary marker, used to measure smaller segments on terrain to find path to next CP. When placing new WP, previous one is removed. WPs do not get saved to DB.

Backend service - Web-API/Rest service. Web based visualization of map and tracks on map (realtime).
https://sportmap.akaver.com
