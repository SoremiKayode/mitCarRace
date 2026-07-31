# EasyRacer Engine Blocks Guide

This guide explains every beginner-facing block added for building a professional landscape racing game in MIT App Inventor.

## 1. Install and place the extension

1. Build or import the EasyRacer Engine `.aix` extension.
2. Drag `EasyRacerEngine` onto your screen.
3. Optional: add a Horizontal or Vertical Arrangement and call `AddToArrangement(arrangement)` so the game fills that arrangement.
4. The engine requests landscape orientation by default. Use `SetLandscapeMode(false)` only if your app must control orientation itself.

## 2. Game flow blocks

### `StartRace()`
Starts the game loop, triggers `RaceStarted`, resets the race timer, and hides the on-screen **START** button. Call it from a custom button or let the built-in on-screen START button call it. The road scrolls immediately to create forward-driving motion, even before the player presses accelerate.

### `PauseRace()`
Pauses physics, scrolling, scoring, and collision updates. The built-in **PAUSE** button calls this while the race is running.

### `PlayRace()`
Resumes a paused game without resetting the score, car position, lap, or timer. The built-in **PLAY** button is visible only after a started race is paused.

### `FinishRace()`
Stops the race, stores the final score in the local score database, and triggers `RaceFinished(finalScore)`.

## 3. Landscape and built-in navigation controls

The game surface draws professional touch controls automatically:

- Circular **Left navigation button** on the left end of the screen. It changes the player car's x position while pressed.
- Circular **Right navigation button** on the right end of the screen. It changes the player car's x position while pressed.
- `SetLeftNavigationButtonImage(path)` and `SetRightNavigationButtonImage(path)` let you replace those built-in arrow labels with uploaded button images.
- Icon-only **START**, **STOP**, **PAUSE**, and **PLAY** buttons for race state.
- Icon-only **SCORES** button to open the score sidebar and an icon-only close button inside the sidebar.
- Icon-only accelerator and brake controls near the right side of the road.

Each control uses the same hit target as its visible gradient button, so players do not need to find hidden tap zones.

## 4. Road blocks

### `CreateRoadTypeBlock(type)`
Use this when you want a quick road setup. The `type` input accepts values such as:

- `City`
- `Highway`
- `Desert`
- `Snow`
- `Dirt`
- `Track`
- `Forest`
- `Beach`
- `Mountain`
- `Cyberpunk`

The engine automatically arranges lane count, surface style, lane markings, roadside colors, grip, friction, difficulty, and road speed.

### `ConfigureRoadBlock(type, lanes, surface, marking, sideStyle, grip, friction, speed, width, difficulty)`
Use this detailed block when you want full control. Inputs needed:

- `type`: The road theme name, such as `City`, `Highway`, `Desert`, `Snow`, `Dirt`, or `Track`.
- `lanes`: Number of visible lanes. Use `2` for small tracks, `3` for city roads, and `4` for highways.
- `surface`: Driving surface, such as `Asphalt`, `Dirt`, `Sand`, or `Ice`.
- `marking`: Lane marking style, such as `Dashed`, `Solid`, or `Double`.
- `sideStyle`: Roadside environment, such as `City`, `Forest`, `Desert`, `Snow`, or `Cyberpunk`.
- `grip`: Tire-road grip. Higher values turn better; lower values skid more. Use about `1.0` for asphalt, `0.55` for dirt or desert, and `0.25` for snow or ice.
- `friction`: How quickly the vehicle slows down. Use about `0.04` for asphalt, `0.07` for dirt, and `0.015` for ice.
- `speed`: Road scroll speed. Use `6` for normal play, `9` for highway, and `4` for beginner courses.
- `width`: Road width in pixels. Use at least `280`; wider values make the game easier.
- `difficulty`: A readable label such as `Easy`, `Normal`, or `Hard` for your own project logic.

## 5. Accelerometer navigation blocks

### `EnableAccelerometerNavigation(enabled, sensitivity, deadZone)`
Turns tilt steering on or off. Inputs needed:

- `enabled`: `true` to use the accelerometer.
- `sensitivity`: Steering strength. Start with `1.2`.
- `deadZone`: Ignore tiny hand movement. Start with `0.8`.

### `NavigateWithAccelerometer(xAccel, yAccel, zAccel)`
Place this inside `AccelerometerSensor.AccelerationChanged`. Connect the sensor's `xAccel`, `yAccel`, and `zAccel` values into this block. The x-axis steers left or right, and the y-axis accelerates or brakes.

## 6. Score database blocks

### `StoreScore(playerId, score)`
Stores a player's best score in the built-in local database and updates the global high score. Race score also increases from the number of seconds journeyed, so longer runs earn points even without coins.

### `RetrieveScore(playerId)`
Returns the best saved score for a player.

### `RetrieveHighScore()`
Returns the highest stored score across saved races.

### `OpenScoreSidebar()` and `CloseScoreSidebar()`
Open or close the sidebar overlay. The sidebar displays current score, saved score, and a **High Score** label beside the highest score.

## 7. Recommended setup steps

1. Add the extension to a landscape screen.
2. Call `AddToArrangement` if you want it inside an arrangement.
3. Call `SetCarImage` and optionally `SetRoadImage`, `SetLeftRoadImage`, and `SetRightRoadImage`. The main road image is centered and uses 70% of the game width; left and right side road images use 15% each. If `SetRoadImage` is set, the generated road artwork is hidden.
4. Call `CreateRoadTypeBlock("Highway")` or use `ConfigureRoadBlock` for custom physics.
5. Call `CreateCar()`.
6. Let the player press the on-screen **START** button or call `StartRace()` from your own button.
7. Add an `AccelerometerSensor` if tilt control is wanted, then call `EnableAccelerometerNavigation(true, 1.2, 0.8)` and `NavigateWithAccelerometer` from the sensor event.
8. Call `OpenScoreSidebar()` from a Scores button, or use the built-in icon-only **SCORES** overlay button. Use the built-in **STOP** button when the player should abandon a run without storing a finish score.


## Image blocks: using uploaded App Inventor assets

For `SetCarImage`, `SetOpponentImage` / `SetOpponentCarImage`, `SetCoinImage`, `SetBikeImage`, `SetRoadImage`, `SetLeftRoadImage`, `SetRightRoadImage`, `SetLeftNavigationButtonImage`, and `SetRightNavigationButtonImage`, first upload the image file in MIT App Inventor's **Media** panel. Then pass the exact uploaded filename as a text value, for example `icon.png`, `car.png`, `coin.png`, or `opponent.png`.

Recommended order:

1. Upload the PNG/JPG/WEBP file to **Media**.
2. In blocks, use a text block containing only the filename, such as `icon.png`.
3. Call the image setter before creating or spawning that object. For example, call `SetCarImage("car.png")` before `CreateCar()`, call `SetOpponentImage("opponent.png")` before `CreateOpponent(...)`, and call `SetCoinImage("coin.png")` before `SpawnCoin(...)`.

Do not use the Image component itself as the value. Use the image asset's filename text. Filenames are case-sensitive on Android, so `Icon.png` and `icon.png` are different names.

## 8. Events to use

- `RaceStarted`: show game UI or play music.
- `RaceResumed`: update your pause/play UI.
- `RaceFinished(finalScore)`: show final score and rewards.
- `CarCrash`: play a crash sound or reduce lives.
- `CoinCollected(value, totalCoins)`: update coin labels.
- `CheckpointReached(index)`: update progress.
- `PlayerWin` and `PlayerLose`: show win/lose screens.

## 9. Professional visual tips

- Use a transparent PNG car image for best results.
- Use road width around `700` to `900` pixels in landscape layouts.
- Use `Highway` for fast endless games and `Track` for lap-based racing.
- Keep obstacle sizes larger than `40x40` pixels so touch-screen players can recognize them quickly.
