## EasyRacer Engine

EasyRacer Engine is a professional MIT App Inventor extension (`.aix`) for building 2D racing games with beginner-friendly blocks.

Students provide images, choose simple properties such as road type or grip, and call `StartRace()` to get a playable racing game. The extension handles scrolling roads, physics, collisions, coins, fuel, nitro, health, checkpoints, laps, HUD, weather, templates, saving, and AI-ready opponents internally.

### Quick start

1. Import the extension into MIT App Inventor.
2. Add `EasyRacerEngine` to a screen or arrangement.
3. Upload images in the MIT App Inventor **Media** panel, then call `SetCarImage`, `SetRoadImage`, `SetCoinImage`, or `SetOpponentImage` with the exact filename text, such as `icon.png`; optionally set `RoadType`, `MaximumSpeed`, and `Grip`.
4. Call `CreateCar()` and `StartRace()`.
5. Use `Accelerate`, `Brake`, `TurnLeft`, and `TurnRight` from buttons or a clock.

### Beginner block groups

Game, Cars, Bikes, Roads, Traffic, Physics, Camera, Sound, Weather, HUD, Score, Fuel, Nitro, Save, PowerUps, Animation, AI, Collision, Events, and Utilities.

### Professional racing UI and blocks

The extension now requests landscape mode by default, draws built-in left, right, accelerate, and brake navigation buttons on the game surface, and includes polished icon-only Start, Stop, Pause, Play, Scores, and close-sidebar controls with touch wave feedback. Start is hidden after the game starts, Stop ends the current run without saving a finish score or leaving stale pressed controls, Pause stops the game loop, and Play resumes without resetting race state.

New beginner blocks include configurable road builders, accelerometer steering, local score storage/retrieval, high-score retrieval, and open/close score sidebar controls. See [`docs/BlocksGuide.md`](docs/BlocksGuide.md) for a full block-by-block tutorial and recommended setup workflow.
