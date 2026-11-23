# Analytics

WaStatusSaver uses [Mixpanel](https://mixpanel.com/) to understand how people use the app. This document explains which events the app sends, when it sends them, and what information comes with them.

> [!NOTE]
> Analytics needs a Mixpanel token. If the token is not set, the app does not crash and keeps working normally, but Mixpanel analytics fails silently and no events are recorded. See [Configure secrets](how-to-set-up-the-project.md#configure-secrets) to set the token.

## Contents

- [Overview](#overview)
- [Super properties](#super-properties)
- [Events](#events)
  - [App](#app)
  - [Navigation drawer](#navigation-drawer)
  - [Home screen](#home-screen)
  - [Onboarding](#onboarding)
  - [Media player](#media-player)
  - [Downloads](#downloads)
  - [Saved Status screen](#saved-status-screen)
  - [Privacy Policy screen](#privacy-policy-screen)

## Overview

An event records something that happened in the app, such as opening the navigation drawer. Its properties add the details, such as which option was picked.

Event names are written in snake_case and begin with a verb, for example `opened_nav_drawer`. Similar actions share one event and differ by a property: viewing the image, video or audio tab is always `viewed_media_tab`, with the tab given in `media_type`.

All analytics code is in the [`core/analytics`](../app/src/main/java/com/voiddevelopers/wastatussaver/core/analytics) package:

| Class | Purpose |
|---|---|
| `AppAnalytics` | Entry point used by the rest of the app, with one function per event |
| `AnalyticsEvents` | Event names |
| `AnalyticsProperties` | Property names |
| `Analytics` | Interface for analytics providers |
| `MixpanelAnalytics` | Mixpanel implementation of `Analytics` |
| `AppLifecycleTracker` | Reports when the app enters or leaves the foreground |

## Super properties

Super properties are sent with every event.

| Property | Values | Description |
|---|---|---|
| `selected_title` | `whatsapp`, `whatsapp_business` | The app selected on the Home screen |
| `has_saf_permission` | `true`, `false` | Whether storage access is granted for the selected app |
| `is_app_installed` | `true`, `false` | Whether the selected app is installed |

## Events

### App

| Event | When it is sent |
|---|---|
| `started_app` | The app is launched or returns from the background |
| `closed_app` | The app goes to the background |

### Navigation drawer

| Event | Properties | When it is sent |
|---|---|---|
| `opened_nav_drawer` | - | The drawer opens |
| `closed_nav_drawer` | - | The drawer closes |
| `clicked_nav_drawer_option` | `option` | A drawer option is tapped |

### Home screen

| Event | Properties | When it is sent |
|---|---|---|
| `changed_title` | - | The user switches between WhatsApp and WhatsApp Business |
| `viewed_media_tab` | `media_type`, `source` | A media tab is shown, either when the screen opens or when the user switches tabs |
| `clicked_refresh_status_media` | - | The refresh button is tapped |
| `clicked_download_app` | - | The button to install the selected app is tapped |
| `clicked_grant_storage_access` | - | The **Grant Access** button is tapped |
| `granted_storage_access` | - | The user grants access to the `.Statuses` folder |
| `denied_storage_access` | `reason` | Storage access is not granted |

For `viewed_media_tab`, `source` is `home`. For `changed_title`, `selected_title` holds the newly selected app.

The `reason` for denied storage access is one of:

| Value | Meaning |
|---|---|
| `cancelled` | The user left the folder picker without choosing a folder |
| `wrong_folder` | The user chose a folder other than `.Statuses` |

### Onboarding

| Event | Properties | When it is sent |
|---|---|---|
| `viewed_onboarding_dialog` | `source` | The onboarding dialog is shown |
| `clicked_onboarding_next` | `page`, `source` | **Next** is tapped |
| `clicked_onboarding_skip` | `page`, `source` | **Skip** is tapped |

Tapping **Next** on page 3 completes the onboarding.

The `source` is one of:

| Value | Meaning |
|---|---|
| `initial_launch` | The dialog was shown automatically |
| `how_to_use` | The dialog was opened from the navigation drawer |

### Media player

These events cover the image viewer, the video player and the audio player.

| Event | Properties | When it is sent |
|---|---|---|
| `viewed_media` | `media_type`, `source` | The player opens |
| `closed_media` | `media_type`, `source` | The player is closed |
| `clicked_share_media` | `media_type`, `source` | The share button is tapped |

The `source` is the screen the player was opened from: `home` or `saved_status`.

### Downloads

| Event | Properties | When it is sent |
|---|---|---|
| `completed_media_download` | `media_type`, `source` | A download completes |

The `source` is where the download was started: `home` for the Home screen, or `viewer` for the media player.

### Saved Status screen

| Event | Properties | When it is sent |
|---|---|---|
| `viewed_saved_status_screen` | - | The screen is opened |
| `closed_saved_status_screen` | - | The screen is closed |
| `viewed_media_tab` | `media_type`, `source` | A media tab is shown, either when the screen opens or when the user switches tabs |

For `viewed_media_tab`, `source` is `saved_status`.

### Privacy Policy screen

| Event | Properties | When it is sent |
|---|---|---|
| `viewed_privacy_policy` | - | The screen is opened |
| `closed_privacy_policy` | - | The screen is closed |
| `loaded_privacy_policy` | - | The page loads successfully |
| `failed_privacy_policy` | `error_code`, `error_description` | The page fails to load because of a network or HTTP error |
| `clicked_refresh_privacy_policy` | - | The refresh button is tapped |
