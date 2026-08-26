# Keep rules for the release build. R8 is on from #8 onwards, so anything the app
# reaches by *name* rather than by reference has to be named here — R8 cannot see a
# string, and the failure mode is a crash on a phone, not a warning at build time.

# Room loads the generated ShabitDatabase_Impl with Class.forName(name + "_Impl"),
# so the class and its no-arg constructor survive only if they are kept by name.
# room-runtime ships this rule itself; it is repeated here because losing it silently
# breaks every read on the device and nothing in CI would catch it.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Glance's actionRunCallback<T>() stores T's class *name* in the RemoteViews and
# instantiates it reflectively when the tap arrives, in a fresh process. Without this,
# tap-to-check on the widget dies with a ClassNotFoundException.
#
# `{ *; }` rather than just `<init>()`: keeping the constructor alone is not enough.
# R8 does not see Glance's dispatch into the interface, so with the narrower rule it
# strips onAction from the surviving class — verified in mapping.txt, where the class
# came through with nothing but <init> and <clinit> on it.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }

# The receivers and the activity are reached from the manifest, which AGP already
# turns into keep rules, and ShabitWidget/RolloverAlarm are ordinary references from
# there. Nothing else in the app is resolved by name: HabitIcons is a table of R
# fields rather than Resources.getIdentifier, precisely so shrinking is safe.
