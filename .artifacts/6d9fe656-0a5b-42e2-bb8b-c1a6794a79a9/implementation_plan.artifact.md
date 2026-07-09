# Implementation Plan - Persist Sound Volume & Upgrade UI Slider

The goal is to ensure the reader's background sound volume is saved across sessions and to upgrade the volume control UI in the `SoundSelectionSheet` to include a numerical indicator using the `CustomSettingsSlider` component.

## Proposed Changes

### Data & Preferences (Reader Module)

#### [MODIFY] [ReaderDataStore.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/pref/datastore/ReaderDataStore.kt)
- Add `SOUND_VOLUME_KEY` (Float).
- Implement `saveSoundVolume(volume: Float)`.

#### [MODIFY] [QuillPreferences.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/pref/model/QuillPreferences.kt)
- Add `soundVolume: Float` (default: 1.0f).

#### [MODIFY] [ReaderPrefRepository.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/pref/repository/ReaderPrefRepository.kt) & [ReaderPrefRepositoryImpl.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/pref/repository/ReaderPrefRepositoryImpl.kt)
- Update to expose and save the sound volume preference.

### Logic (Reader Module)

#### [MODIFY] [ReaderViewModel.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/viewmodel/ReaderViewModel.kt)
- In `loadBook`: Initialize the internal `_soundVolume` state from the saved preferences.
- In `updateSoundVolume`: Add a call to `preferencesRepository.saveSoundVolume(volume)` to persist changes immediately.

### UI Components (Reader Module)

#### [MODIFY] [SoundSelectionSheet.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/ui/components/soundSheet/SoundSelectionSheet.kt)
- Replace the standard `androidx.compose.material3.Slider` with the `CustomSettingsSlider` component.
- Display the volume as a percentage (e.g., "85%") in the numerical indicator.
- Maintain the volume icon (VolumeOff/Down/Up) as `leadingContent` for the slider.

## Verification Plan

### Manual Verification
1.  **Volume Persistence**: Open a book, change the volume to 50%, close the book/app. Re-open and verify the volume remains at 50%.
2.  **Numerical UI**: Open the Ambient Sounds sheet and verify the volume slider now shows a label ("Volume") and a clear numerical value (0-100%).
3.  **Haptics**: Confirm that the "click" haptic still triggers when the numerical value updates.
