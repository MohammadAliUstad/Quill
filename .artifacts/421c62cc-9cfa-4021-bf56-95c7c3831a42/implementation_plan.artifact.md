# Remove Volume Slider from Sound Sheet in Reader Module

Remove the volume slider from the ambient sound selection sheet in the reader module, as users can adjust the volume directly from their phone's hardware buttons/system controls. Clean up associated volume state, datastore preference, and ViewModel logic.

## User Review Required

> [!NOTE]
> Removing the in-app volume slider also removes the stored volume preference (`SOUND_VOLUME_KEY`) and ViewModel volume state, so ambient sounds will always play at full volume (`1.0f`), relying entirely on device volume controls.

## Open Questions

- None.

## Proposed Changes

### Reader Module - UI & ViewModel & Settings

#### [MODIFY] [SoundSelectionSheet.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/ui/components/soundSheet/SoundSelectionSheet.kt)
- Remove `volume: Float` parameter and `onVolumeChange: (Float) -> Unit` callback.
- Remove the volume section (icon, slider, percentage text, haptics).

#### [MODIFY] [ReaderScreen.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/ui/parent/ReaderScreen.kt)
- Remove collection of `soundVolume` state and passing `volume` / `onVolumeChange` to `SoundSelectionSheet`.

#### [MODIFY] [ReaderViewModel.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/viewmodel/reader/ReaderViewModel.kt)
- Remove `_soundVolume` state flow and `updateSoundVolume` function.
- Remove persistence of sound volume.
- Pass fixed max volume (`1.0f`) when calling `backgroundSoundRepository.play` and `playPreview`.

#### [MODIFY] [ReaderDataStore.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/settings/datastore/ReaderDataStore.kt)
- Remove `SOUND_VOLUME_KEY`, `soundVolumeFlow`, and `saveSoundVolume`.

#### [MODIFY] [ReaderSettings.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/settings/model/ReaderSettings.kt)
- Remove `soundVolume` property.

#### [MODIFY] [ReaderSettingsRepository.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/settings/repository/ReaderSettingsRepository.kt) & [ReaderSettingsRepositoryImpl.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/settings/repository/ReaderSettingsRepositoryImpl.kt)
- Remove `saveSoundVolume` interface method and implementation.

## Verification Plan

### Automated Tests
- Run Gradle build to ensure there are no compilation errors across the project:
  `gradle_build(":reader:assembleDebug")`

### Manual Verification
- Deploy the app, open a book in the reader module, open the sound sheet, and verify that the volume slider is removed while sound toggling and auto-play work correctly.
