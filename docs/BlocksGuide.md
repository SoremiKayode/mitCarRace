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

- Circular **Left navigation button** on the left end of the screen. It changes only the player car's x position while pressed, so the car stays facing forward.
- Circular **Right navigation button** on the right end of the screen. It changes only the player car's x position while pressed, so the car stays facing forward.
- `SetLeftNavigationButtonImage(path)` / `SetLeftArrowButtonImage(path)` and `SetRightNavigationButtonImage(path)` / `SetRightArrowButtonImage(path)` let you replace those built-in arrow labels with uploaded button images.
- Icon-only **START**, **STOP**, **PAUSE**, and **PLAY** buttons for race state. Pause and Play share the same fixed top-right position so the button does not jump when toggled.
- Icon-only **SCORES** button to open the score sidebar and an icon-only close button inside the sidebar.
- Up/down accelerator and brake buttons are intentionally not drawn; use the `Accelerate()` and `Brake()` blocks from your own controls if needed.

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
3. Call `SetCarImage` or `SetCarImageWithDirection` and optionally `SetRoadImage`, `SetLeftRoadImage`, and `SetRightRoadImage`. The main road image is centered and uses 70% of the game width; left and right side road images use 15% each. If `SetRoadImage` is set, the generated road artwork is hidden.
4. Call `CreateRoadTypeBlock("Highway")` or use `ConfigureRoadBlock` for custom physics.
5. Call `CreateCar()`.
6. Let the player press the on-screen **START** button or call `StartRace()` from your own button.
7. Add an `AccelerometerSensor` if tilt control is wanted, then call `EnableAccelerometerNavigation(true, 1.2, 0.8)` and `NavigateWithAccelerometer` from the sensor event.
8. Call `OpenScoreSidebar()` from a Scores button, or use the built-in icon-only **SCORES** overlay button. Use the built-in **STOP** button when the player should abandon a run without storing a finish score.


## Image blocks: using uploaded App Inventor assets

For `SetCarImage`, `SetCarImageWithDirection`, `SetOpponentImage` / `SetOpponentImageWithDirection` / `SetOpponentCarImage`, `SetOpponentVehicleWithDirection`, `SetCoinImage`, `SetConeImage`, `SetRoadblockImage`, `SetBikeImage`, `SetBikeImageWithDirection`, `SetRoadImage`, `SetLeftRoadImage`, `SetRightRoadImage`, `SetLeftNavigationButtonImage` / `SetLeftArrowButtonImage`, and `SetRightNavigationButtonImage` / `SetRightArrowButtonImage`, first upload the image file in MIT App Inventor's **Media** panel. Then pass the exact uploaded filename as a text value, for example `icon.png`, `car.png`, `coin.png`, or `opponent.png`.

Recommended order:

1. Upload the PNG/JPG/WEBP file to **Media**.
2. In blocks, use a text block containing only the filename, such as `icon.png`.
3. Call the image setter before creating or spawning that object. For example, call `SetCarImageWithDirection("car.png", "Up")` before `CreateCar()`, call `SetOpponentImageWithDirection("opponent.png", "Down")` before `CreateOpponent(...)` if the uploaded opponent image originally points down, and call `SetCoinImage("coin.png")` before `SpawnCoin(...)`.

Do not use the Image component itself as the value. Use the image asset's filename text. Filenames are case-sensitive on Android, so `Icon.png` and `icon.png` are different names.


### Image original direction helpers

Use these helpers when an uploaded vehicle picture was drawn facing a direction other than up. The engine then rotates the bitmap to match its movement direction, preventing cars from appearing to drive in reverse. Direction inputs accept `Up`, `Down`, `Left`, `Right`, or degree values such as `0`, `90`, `180`, and `270`.

- `SetCarImageWithDirection(path, originalDirection)` sets the player car image and original facing direction in one block.
- `SetCarImageOriginalDirection(originalDirection)` changes only the player car image's original facing direction.
- `SetBikeImageWithDirection(path, originalDirection)` sets the player bike image and original facing direction in one block.
- `SetOpponentImageWithDirection(path, originalDirection)` sets the default opponent image and original facing direction in one block.
- `SetOpponentImageOriginalDirection(originalDirection)` changes only the default opponent image's original facing direction.
- `SetOpponentVehicleWithDirection(name, imagePath, originalDirection)` assigns a named opponent image and its original facing direction.

## 8. Events to use

- `RaceStarted`: show game UI or play music.
- `RaceResumed`: update your pause/play UI.
- `RaceFinished(finalScore)`: show final score and rewards.
- `CarCrash`: play a crash sound or reduce lives.
- `CoinCollected(value, totalCoins)`: update coin labels.
- `CheckpointReached(index)`: update progress.
- `PlayerWin` and `PlayerLose`: show win/lose screens.

## 9. Professional visual tips

- Use a transparent PNG car image for best results. The engine preserves each car image aspect ratio inside its vehicle box so wide or tall images are not stretched into a warped shape.
- Use road width around `700` to `900` pixels in landscape layouts.
- Use `Highway` for fast endless games and `Track` for lap-based racing.
- Keep obstacle sizes larger than `40x40` pixels so touch-screen players can recognize them quickly.


## Complete racing game recipe: main car, opponents, coins, roadblocks, and crashes

Use this checklist when you want a complete beginner car-racing game instead of only testing one block at a time. The examples use the bundled sample filenames in this repository (`mycar.png`, `opponentcar.png`, `coin.png`, `road.png`, and `cone.png`), but in MIT App Inventor you should upload your own files to the **Media** panel and type the exact filenames.

### Step 1: Prepare the screen and game surface

1. Set `Screen1.ScreenOrientation` to landscape, or call `SetLandscapeMode(true)` in `Screen1.Initialize`.
2. Drag `EasyRacerEngine` onto the screen. If you want the game inside a layout, add an arrangement and call `AddToArrangement(GameArrangement)`.
3. Optional but recommended: add labels for score, coins, health, and game messages, plus buttons for Restart, Pause, and Nitro if you do not want to use the built-in overlay controls.

### Step 2: Upload and assign images

Call the image setters before creating objects so the first frame already looks like your game:

1. `SetRoadImage("road.png")` for the scrolling road.
2. `SetCarImage("mycar.png")` for the player's main car.
3. `SetOpponentImage("opponentcar.png")` for the default opponent car.
4. `SetCoinImage("coin.png")` for collectible coins.
5. `SetConeImage("cone.png")` for cone obstacles.
6. `SetRoadblockImage("roadblock.png")` if you upload a roadblock, barrier, barricade, or block image; otherwise the extension draws a red-and-white roadblock fallback automatically.

### Step 3: Build the road and player car

In `Screen1.Initialize`, use one preset road or one custom road, then create the player:

1. Easy preset: `CreateRoadTypeBlock("Highway")`.
2. Custom preset: `ConfigureRoadBlock("Highway", 4, "Asphalt", "Dashed", "City", 1.0, 0.04, 8, 720, "Normal")`.
3. Set optional racing rules: `MaximumSpeed` to `18` (minimum `0`), `TurningSpeed` to `6` (minimum `0`), `CarDirection` to `0`, `OpponentDirection` to `180`, `MaximumLap` to `3`, `EnableFuel(true)`, and `EnableNitro(true)`.
4. Call `CreateCar()`.

### Step 4: Add opponent cars

Choose one of these approaches:

1. Simple traffic: call `AddOpponents(3)` after `SetOpponentImage`. The engine spreads cars across lanes and moves them automatically.
2. One exact opponent: call `CreateOpponent(420, 120)` to place an opponent at a chosen screen position.
3. Different opponent types: call `SetOpponentVehicle("Truck", "truck.png")`, then `SpawnRandomOpponent("Truck")` or `CreateOpponentVehicleWithSize("Truck", "truck.png", 520, -160, 150, 230)`.
4. Numbered opponent images: call `SetOpponentImageByNumber(1, "truck.png", "Up")`, `SetOpponentImageByNumber(2, "taxi.png", "Down")`, and so on. The number starts at 1 and matches the order created by `AddOpponents`, so each opponent can have its own image and original facing direction.

Opponents avoid overlapping each other while they drive. Each opponent prefers to stay in one lane and only seldom transitions to another lane when that lane is blocked. Every few respawns, one opponent is forced into the player's current lane as a head-on challenge, while nearby left and right escape lanes are cleared so the player still has a fair way out. The traffic lane planner also prevents a lane from mixing cars coming from above with cars coming from below when it is not creating that deliberate head-on challenge. Opponent-to-opponent contact is separated automatically and never triggers game-over or crash events. Opponents are damaging only when they overlap the main car: the extension calls `WhenCarCrash`, `CarCrash`, applies damage, and respawns that opponent.

### Step 5: Add coins

Use `SpawnCoin(x, y)` to put coins in safe lane positions. For example:

1. `SpawnCoin(320, -120)`
2. `SpawnCoin(480, -360)`
3. `SpawnCoin(640, -620)`

When the player touches a coin, the extension removes it, adds `100` score, increases total coins, and dispatches `WhenCarHitsCoin` and `CoinCollected(value, totalCoins)`. In `CoinCollected`, update your coin label and optionally spawn another coin above the screen.

### Step 6: Add roadblocks and other obstacles

Use `CreateObstacle(type, x, y, width, height)`. Damaging obstacle types include `roadblock`, `barrier`, `barricade`, `block`, `cone`, `tree`, `rock`, `fire`, and `pothole`. Grip hazards include `oil` and `water`; they reduce grip instead of causing a normal crash.

Recommended examples:

1. `CreateObstacle("roadblock", 520, -220, 150, 70)` for a wide barrier.
2. `CreateObstacle("cone", 360, -460, 62, 82)` for a cone.
3. `CreateObstacle("oil", 610, -720, 110, 62)` for a slippery hazard.

### Step 7: Start, steer, and restart the race

1. Start with the built-in on-screen Start button, or call `StartRace()` from your own Start button.
2. The built-in left/right buttons steer the main car only while the race is actively playing. You can also call `TurnLeft()` and `TurnRight()` from your own controls; those movement blocks are ignored while the race is stopped or paused.
3. If you use an `AccelerometerSensor`, call `EnableAccelerometerNavigation(true, 1.2, 0.8)` once, then call `NavigateWithAccelerometer(xAccel, yAccel, zAccel)` inside the sensor's `AccelerationChanged` event.
4. For a one-hit game over, call `EnableGameOverOnCrash(true)` before `StartRace()`.
5. For restart, call `StopRace()`, `ResetScore()`, `ResetCar()`, add fresh opponents/coins/obstacles, then call `StartRace()`.

### Step 8: Detect crashes and decide what happens

The engine already checks collisions every frame after `StartRace()`. Use these event blocks for game logic:

1. `EnableGameOverOnCrash(true)`: turns every crash into an immediate `PlayerLose` + `FinishRace()` game over. Use this for classic one-hit racing games.
2. `WhenCarCrash` or `CarCrash`: play a crash sound, show "Crash!", update a health label with `Health`, call `Respawn()`, or manually call `FinishRace()` if you want custom game-over logic.
3. `WhenCarHitsWall`: warn the player or reduce score when the car leaves the road.
4. `PlayerLose`: show a game-over screen when health reaches `0` or when one-hit game-over mode is enabled.
5. `IsCarCrashed()`: use this boolean in custom clock logic if you want to check collision state yourself.
6. `CrashCar(damageAmount)`: force a crash from your own rules, such as touching a custom App Inventor sprite.

### Step 9: Finish the game

1. Use checkpoints and laps for a race: create checkpoints with `CreateCheckpoint`, set `MaximumLap`, and handle `PlayerWin`.
2. Use endless mode for survival: keep spawning coins, opponents, and obstacles above the screen after each event.
3. In `RaceFinished(finalScore)`, show the final score. The extension stores the score automatically, so you can use `RetrieveHighScore()` for a high-score screen.

## 10. Complete block reference

This section lists each visible EasyRacer block, what it does, and the parameters it needs. In MIT App Inventor, use text blocks for `String` values, number blocks for `int`/`float` values, and true/false blocks for `boolean` values.

### Layout and screen blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `AddToArrangement(arrangement)` | `arrangement`: a Horizontal or Vertical Arrangement component. | Adds the game surface into the selected arrangement so the race fills that part of the screen. |
| `SetLandscapeMode(enabled)` | `enabled`: `true` for landscape, `false` to let the app choose orientation. | Requests landscape orientation for a racing-game layout. |

### Race flow blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `CreateCar()` | None. | Creates/resets the player car using the current car image, size, physics, and position settings. |
| `CreateBike()` | None. | Switches to bike mode and resets the player vehicle using the bike image if one was set. |
| `DeleteCar()` | None. | Stops the race and removes active player motion from the current run. |
| `ResetCar()` | None. | Resets player position, speed, angle, health, fuel, nitro, and lap. |
| `Respawn()` | None. | Places the vehicle back on the road after a crash, keeps at least 1 health, and triggers `CarStopped`. |
| `StartRace()` | None. | Starts the loop, hides the Start button, resets the race timer, and triggers `RaceStarted`. |
| `PauseRace()` | None. | Pauses the loop without resetting score, position, or lap. |
| `PlayRace()` | None. | Resumes a paused race and triggers `RaceResumed`. |
| `FinishRace()` | None. | Ends the race, stores the score for the current player, and triggers `RaceFinished(finalScore)`. |
| `StopRace()` | None. | Ends the current run without saving a finish score and shows the Start button again. |
| `EnableGameOverOnCrash(enabled)` | `enabled`: true/false. | When true, any crash with an opponent, wall, roadblock, cone, or damaging obstacle immediately triggers `PlayerLose` and `FinishRace()`. |

### Driving and control blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `Accelerate()` | None. | Increases forward speed unless fuel is enabled and empty. |
| `Brake()` | None. | Reduces speed and can reverse slowly at low speed. |
| `TurnLeft()` / `MoveLeft()` | None. | Moves the vehicle left inside the lane area using `TurningSpeed`; use `SetCarDirection` or `CarDirection` to rotate the main car image. |
| `TurnRight()` / `MoveRight()` | None. | Moves the vehicle right inside the lane area using `TurningSpeed`; use `SetCarDirection` or `CarDirection` to rotate the main car image. |
| `SetCarDirection(degrees)` | `degrees`: rotation angle. | Rotates the main car image. Use `0` for up, `90` for right, `180` for down, and `270` for left. |
| `SetOpponentDirection(degrees)` | `degrees`: base rotation angle. | Rotates opponent car images before automatic above/below lane flipping. Use `180` as the default for cars that normally face down the screen. |
| `UseNitro()` | None. | Spends nitro for a temporary speed boost when nitro is enabled and available. |
| `EnableAccelerometerNavigation(enabled, sensitivity, deadZone)` | `enabled`: true/false; `sensitivity`: steering multiplier; `deadZone`: ignored tilt amount. | Enables tilt steering. Start with `true`, `1.2`, and `0.8`. |
| `NavigateWithAccelerometer(xAccel, yAccel, zAccel)` | `xAccel`, `yAccel`, `zAccel`: values from `AccelerometerSensor.AccelerationChanged`. | Converts accelerometer readings into left/right steering plus acceleration/braking. |

### Road setup blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `CreateRoadTypeBlock(type)` | `type`: text such as `City`, `Highway`, `Desert`, `Snow`, `Dirt`, `Track`, `Forest`, `Beach`, `Mountain`, or `Cyberpunk`. | Applies safe preset values for lanes, surface, grip, friction, roadside style, and speed. |
| `ConfigureRoadBlock(type, lanes, surface, marking, sideStyle, grip, friction, speed, width, difficulty)` | `type`: theme text; `lanes`: lane count; `surface`: `Asphalt`, `Dirt`, `Sand`, or `Ice`; `marking`: `Dashed`, `Solid`, or `Double`; `sideStyle`: roadside theme; `grip`: turning traction; `friction`: slow-down amount; `speed`: road scroll speed; `width`: road width; `difficulty`: label text. | Builds a custom road with your exact visual and physics settings. |
| `SetRoadWidth(width)` | `width`: road width in pixels. | Sets the intended road width value. |
| `SetRoadHeight(height)` | `height`: road image tile height in pixels. | Controls how tall each scrolling road image tile is. |

### Image blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `SetCarImage(path)` | `path`: uploaded filename, asset path, file path, `file://` URL, or `http(s)` URL. | Sets the player car image. Use a transparent PNG for best results. |
| `SetBikeImage(path)` | `path`: image location text. | Sets the player bike image used by `CreateBike()`. |
| `SetOpponentImage(path)` | `path`: image location text, for example `opponent.png`. | Sets the default opponent image and redraws the game immediately. |
| `SetOpponentVehicle(name, imagePath)` / `SetOpponentVehicleImage(name, imagePath)` | `name`: opponent vehicle name; `imagePath`: uploaded filename or path. | Registers a named opponent car or bike image that can be reused for multiple opponents. |
| `SetOpponentImageByNumber(opponentNumber, imagePath, originalDirection)` | `opponentNumber`: 1-based opponent number; `imagePath`: uploaded filename or path; `originalDirection`: `Up`, `Down`, `Left`, `Right`, or degrees. | Assigns a unique image and original facing direction to a specific numbered opponent created by `AddOpponents`; existing opponents update immediately. |
| `SetOpponentCarImage(path)` / `OpponentCarImage` property | `path`: image location text. | Sets the default opponent car image; use this when your project has the `opponentCarImage` property block. |
| `SetCoinImage(path)` | `path`: image location text, for example `coin.png`. | Sets the coin image used by `SpawnCoin`. The engine also tries bundled `coin.png`/`images/coin.png` before drawing a built-in coin fallback. |
| `SetConeImage(path)` | `path`: image location text, for example `cone.png`. | Sets the image used when `CreateObstacle` type contains `cone`; otherwise a built-in cone fallback is drawn. |
| `SetRoadblockImage(path)` | `path`: image location text, for example `roadblock.png`. | Sets the image used when `CreateObstacle` type contains `roadblock`, `barrier`, `barricade`, or `block`; otherwise a built-in red-and-white fallback is drawn. |
| `SetRoadImage(path)` | `path`: image location text. | Replaces the generated middle road with a scrolling road image. |
| `SetLeftRoadImage(path)` | `path`: image location text. | Sets the scrolling image for the left 15% roadside area. |
| `SetRightRoadImage(path)` | `path`: image location text. | Sets the scrolling image for the right 15% roadside area. |
| `SetLeftNavigationButtonImage(path)` / `SetLeftArrowButtonImage(path)` | `path`: image location text. | Replaces the built-in left arrow label with a custom button image. |
| `SetRightNavigationButtonImage(path)` / `SetRightArrowButtonImage(path)` | `path`: image location text. | Replaces the built-in right arrow label with a custom button image. |

### Objects, collectibles, obstacles, and power-up blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `CreateOpponent(x, y)` | `x`: horizontal center position; `y`: vertical center position. | Spawns an AI opponent car at the given screen position and redraws immediately. |
| `AddOpponents(count)` | `count`: number of default opponent cars to add. | Adds many opponents at once, spread across lanes, alternating cars coming from above and below. A lane is reserved for one travel direction at a time, so a car coming from above will not share that lane with a car coming from below. |
| `AddOpponentVehicles(name, imagePath, count)` | `name`: opponent vehicle name; `imagePath`: optional uploaded filename/path; `count`: number to add. | Adds many named opponent cars or bikes at once with the same alternating two-direction traffic behavior. |
| `CreateOpponentVehicle(name, imagePath, x, y)` | `name`: opponent vehicle name; `imagePath`: optional uploaded filename/path; `x`, `y`: center position. | Spawns a named opponent car or bike with its own image, allowing multiple opponent vehicles on the road. |
| `CreateOpponentVehicleWithSize(name, imagePath, x, y, width, height)` | Same as above plus custom width and height. | Spawns the whole named opponent vehicle at the requested size. |
| `SpawnRandomOpponent(name)` | `name`: registered opponent vehicle name. | Places the named opponent at a random valid road position above the screen so it moves into view. |
| `SpawnRandomOpponentVehicle(name, imagePath)` / `SpawnRandomOpponentCar(name, imagePath)` | `name`: opponent vehicle name; `imagePath`: uploaded filename/path. | Registers/uses the image and places the whole opponent car or bike randomly on the road. |
| `SpawnCoin(x, y)` | `x`: horizontal center position; `y`: vertical center position. | Adds a collectible coin at the given screen position. |
| `CreateCheckpoint(x, y, width, height)` | `x`, `y`: center position; `width`, `height`: checkpoint rectangle size. | Adds a checkpoint area that triggers `CheckpointReached(index)` when touched. |
| `CreateObstacle(type, x, y, width, height)` | `type`: text such as `roadblock`, `barrier`, `tree`, `rock`, `cone`, `oil`, `water`, `fire`, or `pothole`; `x`, `y`, `width`, `height`: obstacle rectangle. | Adds a damaging obstacle or grip-changing hazard. |
| `SpawnPowerUp(type, x, y)` | `type`: `Shield`, `Nitro`, `Double Coin`, `Repair`, `Slow Motion`, `Magnet`, or `Invincible`; `x`, `y`: center position. | Adds a built-in power-up object. |

### Score, save, and sidebar blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `StoreScore(playerId, score)` | `playerId`: player name/id text; `score`: number to save. | Saves that player's best score and updates the global high score. |
| `RetrieveScore(playerId)` | `playerId`: player name/id text. | Returns that player's saved best score. |
| `RetrieveHighScore()` | None. | Returns the highest score saved on the device. |
| `OpenScoreSidebar()` | None. | Opens the professional score dashboard sidebar. |
| `CloseScoreSidebar()` | None. | Closes the score dashboard sidebar. The built-in close icon is drawn without a circular button background. |
| `ShowProfessionalNotifier(title, message, type, durationMs)` | `title`: heading text; `message`: body text; `type`: `Info`, `Success`, `Warning`, or `Error`; `durationMs`: milliseconds to show, or `0` until dismissed. | Shows a polished in-game notification banner instead of using the built-in MIT App Inventor Notifier component. |
| `DismissProfessionalNotifier()` | None. | Hides the current professional notifier banner. |
| `GetScore()` | None. | Returns the current score. |
| `ResetScore()` | None. | Clears current score and coins and restarts the race timer. |
| `Save()` | None. | Saves coins, score, car name, theme, and road type to local storage. |
| `Load()` | None. | Loads saved coins, score, car name, theme, and road type from local storage. |

### Vehicle customization, health, fuel, and sound blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `SetSound(name, path)` | `name`: sound category such as `Engine`, `Brake`, `Crash`, `Horn`, `Coin`, `Nitro`, `Victory`, or `Game Over`; `path`: uploaded sound filename/path. | Stores the sound asset path for that sound category. |
| `CustomizeCar(tint, wheelSizeValue, wheelPath, spoilerPath, exhaustPath, headlightPath)` | `tint`: color text; `wheelSizeValue`: number; remaining parameters: optional image filenames/paths. | Stores simple car customization settings for your project. |
| `SetVehicleSize(width, height)` | `width`, `height`: pixel size. | Sets the player vehicle's drawing/collision size. |
| `SetBodyScale(scale)` | `scale`: multiplier number, for example `1.0`. | Scales the default vehicle body size. |
| `EnableFuel(enabled)` | `enabled`: true/false. | Turns automatic fuel usage on or off. |
| `AddFuel(amount)` | `amount`: number of fuel points to add. | Adds fuel up to the maximum of 100. |
| `EnableNitro(enabled)` | `enabled`: true/false. | Turns nitro boosting on or off. |
| `Repair(amount)` | `amount`: health points to add. | Repairs health up to the maximum of 100. |
| `Damage(amount)` | `amount`: health points to remove. | Reduces health and triggers lose/crash events if health reaches zero. |
| `CrashCar(damageAmount)` | `damageAmount`: health points to remove. | Forces a crash from blocks, dispatches crash events, applies damage, and ends the game if `EnableGameOverOnCrash(true)` is active. |
| `IsCarCrashed()` | None. | Returns true when the car currently overlaps an opponent or damaging obstacle. |

### Template, theme, and multiplayer blocks

| Block | Parameters | What it does |
| --- | --- | --- |
| `ApplyTemplate(template)` | `template`: `Endless Highway`, `Formula Track`, `City Traffic`, `Desert Rally`, or `Motorcycle Challenge`. | Applies a full starter setup for a common racing game style. |
| `ApplyTheme(name)` | `name`: `City`, `Village`, `Desert`, `Snow`, `Space`, `Cyberpunk`, `Forest`, `Beach`, or `Mountain`. | Changes the visual theme and road preset. |
| `MultiplayerSnapshot()` | None. | Returns a compact JSON text snapshot containing player x/y position, speed, lap, and score. |

### Property blocks

| Property | Input needed when setting | What it controls or returns |
| --- | --- | --- |
| `CarSpeed` | Number. | Gets or sets the current speed. |
| `Acceleration` | Number. | Controls how quickly `Accelerate()` increases speed. |
| `BrakeStrength` | Number. | Controls how strongly `Brake()` reduces speed. |
| `MaximumSpeed` | Number. | Sets the maximum forward speed. Default is `18`; minimum is `0`. |
| `TurningSpeed` | Number. | Sets left/right steering strength. Default is `4`; common values are `4` to `6`; minimum is `0`. |
| `CarDirection` | Number. | Gets or sets the main car image rotation in degrees (`0`, `90`, `180`, or `270` are common). |
| `OpponentDirection` | Number. | Gets or sets the base opponent image rotation in degrees before automatic above/below lane flipping. |
| `CarWidth` / `CarHeight` | Number. | Gets or sets vehicle size in pixels. |
| `CarName` | Text. | Sets the display/current player id used for saved scores. |
| `Weight` | Number. | Heavier values reduce acceleration. |
| `Grip` | Number. | Higher values steer better; lower values skid more. |
| `RoadFriction` | Number. | Higher values slow the vehicle faster. |
| `RoadGrip` | Number. | Road traction multiplier applied with vehicle grip. |
| `RoadType` | Text. | Gets or applies a road type/preset name. |
| `SpeedMultiplier` | Number. | Multiplies the effective maximum speed. |
| `RoadWidth` / `RoadHeight` | Number. | Controls road dimensions and image tiling. |
| `RoadSpeed` | Number. | Controls automatic road scrolling speed. |
| `AutoScroll` | Boolean. | Enables or disables automatic road/object scrolling. |
| `InfiniteMode` | Boolean. | Keeps the car clamped inside the visible play area. |
| `GameOverOnCrash` | Boolean. | Property version of `EnableGameOverOnCrash`; true makes crashes immediately trigger `PlayerLose` and `FinishRace()`. |
| `ScrollDirection` | Text. | Stores scroll direction text for your project logic. |
| `FuelLevel` | Read-only. | Returns current fuel. |
| `NitroAmount` | Read-only. | Returns current nitro. |
| `Health` | Number. | Gets or sets health from 0 to 100. |
| `CurrentLap` | Read-only. | Returns the current lap. |
| `MaximumLap` | Number. | Sets the number of laps needed to win. |
| `RaceTime` | Read-only. | Returns seconds since the race timer started. |
| `Countdown` | Seconds. | Sets race countdown duration; when reached, the race finishes. |
| `SpeedometerStyle` | Text. | Stores the speedometer style name. |
| `HUDEnabled` | Boolean. | Shows or hides the HUD. |
| `MiniMapEnabled` | Boolean. | Enables/disables minimap state for project logic. |
| `SpeedometerEnabled` | Boolean. | Enables/disables speedometer state for project logic. |
| `CameraMode` | Text. | Stores camera mode text. |
| `CameraZoom` | Number. | Sets camera zoom value, minimum `0.25`. |
| `CameraOffsetX` / `CameraOffsetY` | Number. | Stores camera offset values. |
| `Weather` | Text. | Applies weather effects such as rain, snow, or storm grip changes. |
| `TrafficEnabled` | Boolean. | Enables/disables traffic state for project logic. |

### Event blocks

| Event | Parameters provided | When it runs |
| --- | --- | --- |
| `RaceStarted` | None. | After the race starts. |
| `RaceResumed` | None. | After a paused race resumes. |
| `RaceFinished(finalScore)` | `finalScore`: ending score. | When the race finishes normally. |
| `CarStopped` | None. | When speed reaches zero or the run is stopped. |
| `CarMoving` | None. | When acceleration starts moving the vehicle. |
| `NitroStarted` | None. | When nitro boost begins. |
| `NitroEnded` | None. | When nitro reaches zero during use. |
| `FuelEmpty` | None. | When fuel is empty and acceleration is requested. |
| `CheckpointReached(index)` | `index`: checkpoint number, starting at 1. | When the car touches a checkpoint. |
| `CoinCollected(value, totalCoins)` | `value`: coin points; `totalCoins`: total collected. | When the car collects a coin. |
| `WhenCarHitsWall` | None. | When the car leaves the main road bounds. |
| `WhenCarHitsCoin` | None. | When a coin collision happens. |
| `WhenCarHitsPowerup(type)` | `type`: power-up/hazard text. | When the car touches a power-up, oil, or water hazard. |
| `WhenCarCrash` | None. | When the car hits an obstacle or opponent. |
| `CarCrash` | None. | When a crash is processed. |
| `PlayerWin` | None. | When the player completes the required laps. |
| `PlayerLose` | None. | When health reaches zero or `EnableGameOverOnCrash(true)` ends the race after a crash. |
| `TimeFinished` | None. | When the countdown timer reaches its limit. |
