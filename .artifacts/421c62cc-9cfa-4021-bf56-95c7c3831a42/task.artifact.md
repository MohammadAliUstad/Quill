# Task List - Remove Volume Slider from Sound Sheet

- [x] Remove sound volume from `ReaderDataStore.kt`
- [x] Remove sound volume from `ReaderSettings.kt`
- [x] Remove `saveSoundVolume` from `ReaderSettingsRepository.kt` and `ReaderSettingsRepositoryImpl.kt`
- [x] Update `ReaderViewModel.kt` to remove volume state, `updateSoundVolume`, and use `1.0f` volume
- [x] Update `ReaderScreen.kt` to remove volume state collection and passing `volume`/`onVolumeChange`
- [x] Update `SoundSelectionSheet.kt` to remove volume slider section and parameters
- [x] Verify project builds successfully with Gradle
