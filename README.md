# Space Mouse

Control the Old School RuneScape camera with a 3D mouse (SpaceMouse, SpaceNavigator,
SpaceMouse Compact, ...).

The plugin does not open the device itself. Your 3D mouse driver maps each axis to an
ordinary key, and the plugin turns "key held" into a smooth, configurable camera
rotation or zoom. That keeps it inside the RuneLite API: no native libraries, no
reflection, no subprocesses and no synthetic input events.

```
SpaceMouse --> 3DxWare / spacenavd --axis->key--> key event --> SpaceMousePlugin
                                                                     |
                                    setCameraYawTarget / setCameraPitchTarget
                                    runScript(CAMERA_DO_ZOOM)
```

## Setup

### 1. Install and enable the plugin

The plugin is only half of the setup: out of the box no key is pressed, so nothing
happens until the driver is mapped.

### 2. Map the driver axes to keys

**Windows (3DxWare)**

Copy [`driver/windows-3dxware/RuneLite.xml`](driver/windows-3dxware/RuneLite.xml) into
`%APPDATA%\3Dconnexion\3DxWare\Cfg\` and restart `3DxService.exe`:

```powershell
Copy-Item .\driver\windows-3dxware\RuneLite.xml "$env:APPDATA\3Dconnexion\3DxWare\Cfg\"
Stop-Process -Name 3DxService -Force
Start-Process "C:\Program Files\3Dconnexion\3DxWare\3DxWinCore\3DxService.exe"
```

The profile matches `java.exe` + window class `SunAwtFrame`, so it applies to the
RuneLite client. It maps:

| Axis | Motion | Key | Camera |
| --- | --- | --- | --- |
| `Rz` | twist left / right | numpad 4 / 6 | yaw |
| `Ry` | tilt left / right | numpad 8 / 2 | pitch |
| `Rx` | push forward / back | numpad + / - | zoom in / out |
| `Z` | lift / press | numpad + / - | zoom in / out |
| `X`, `Y` | sideways / in-out translation | - | disabled |

On the hardware this was tested with, the forward/back motion drives `Y` and `Rx`
together, so `Y` is disabled to avoid doubling the zoom. `Z` fires cleanly once its
deadband is raised, so it shares the zoom keys. If your device behaves differently,
edit the profile: each axis direction is a separate `<Axis>` block with an
`<Input>` range and an `<Output>` keystroke.

`<Key>` values are **USB HID keyboard usage IDs**, not PS/2 scancodes
(`0x29` = Esc, `0x5C` = keypad 4, `0x68` = F13).

**Linux (spacenavd)**

Map the axes with `xbindkeys` (or any tool that turns axis motion into key events) to
the same keys, e.g. `spacenavd` axes to `KP_4`/`KP_6`, `KP_8`/`KP_2`, `KP_Add`/`KP_Subtract`.

### 3. Adjust the keybinds if needed

Every key is configurable. If numpad keys are inconvenient, bind F13-F18 in the driver
(`0x68`-`0x6D`) and set the same keys in the plugin configuration; those never produce
text.

## Configuration

| Setting | Default | Description |
| --- | --- | --- |
| Rotate left / right | numpad 4 / 6 | Hold to yaw |
| Rotate up / down | numpad 8 / 2 | Hold to pitch |
| Yaw speed | 180 deg/s | Rotation rate while held |
| Pitch speed | 90 deg/s | Rotation rate while held |
| Zoom in / out | numpad + / - | Hold to zoom |
| Zoom speed | 300 units/s | Zoom rate while held |
| Invert yaw / pitch / zoom | off | Swap each pair of keys |

Rotation and zoom stop when the client loses focus, when you log out, or when a text
field (chat, bank search, ...) has focus.

## Notes

- Pitch is clamped to the client's own range (about 22.5-67.3 degrees) and yaw wraps at
  16384 camera units per turn, so the camera cannot get into an invalid state.
- Zoom reads the live zoom level when a zoom starts, so scrolling with the mouse in
  between never makes the camera jump.
- The driver keys are ordinary keys. While a text field has focus the plugin ignores
  them, but the characters can still reach the field. Use F13-F18 if that is a problem.
- Translation axes have no legal camera target in RuneLite, which is why they are unused.

## Building

```
./gradlew build        # compile
./gradlew run          # launch a developer client with the plugin loaded
./gradlew shadowJar    # build a stand-alone jar
```

On Windows use `gradlew.bat` instead of `./gradlew`. The hub builds the plugin with
its standard template (`build=standard` in `runelite-plugin.properties`).

## License

BSD 2-Clause. See [LICENSE](LICENSE).
