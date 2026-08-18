# Implementation Plan - Animate Sound Icon and Add Slider Tone Icon

This plan describes how to enhance the `SoundSelectionSheet` with a dynamic volume icon that responds to the slider's value and a tone icon embedded within the volume slider track, mimicking the Pixel OS volume control.

## User Review Required

> [!NOTE]
> I will be using `Crossfade` to smoothly animate the transition between volume icons.
> The tone icon inside the slider will be fixed to the left side of the track, similar to Pixel's media volume slider.

## Proposed Changes

### UI Components

#### [MODIFY] [SoundSelectionSheet.kt](file:///C:/Users/Moham/AndroidStudioProjects/Quill/reader/src/main/java/com/yugentech/quill/reader/ui/components/soundSheet/SoundSelectionSheet.kt)

1.  **Dynamic Volume Icon**:
    *   Implement logic to select an icon based on `volume`:
        *   `volume == 0f`: `Icons.AutoMirrored.Rounded.VolumeOff`
        *   `volume < 0.5f`: `Icons.AutoMirrored.Rounded.VolumeDown`
        *   `else`: `Icons.AutoMirrored.Rounded.VolumeUp`
    *   Wrap the leading `Icon` in a `Crossfade` for a smooth visual transition between states.

2.  **Embedded Tone Icon**:
    *   Update the `Slider`'s `track` lambda.
    *   Wrap the `SliderDefaults.Track` in a `Box`.
    *   Add an `Icon` (e.g., `Icons.Rounded.MusicNote`) positioned on the left side of the track.
    *   Ensure the icon color adapts to the track's active/inactive state or uses a fixed high-contrast color (like `onPrimaryContainer` / `onSurfaceVariant`).

## Verification Plan

### Automated Tests
- None, visual/interaction focus.

### Manual Verification
1.  Open the Sound Selection sheet.
2.  Slide the volume from 0 to 100%.
3.  Verify that the icon to the left of the slider changes from "Off" to "Low" to "High" waves smoothly.
4.  Verify that a small music/tone icon is visible inside the thick volume slider track on the left side.
