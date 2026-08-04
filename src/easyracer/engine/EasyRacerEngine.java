package easyracer.engine;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.MediaUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class EasyRacerEngine extends AndroidViewComponent {
    private final ComponentContainer container;
    private final RacerView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HashMap<String, Bitmap> imageCache = new HashMap<String, Bitmap>();
    private final HashMap<String, Bitmap> opponentImages = new HashMap<String, Bitmap>();
    private final HashMap<String, String> opponentImagePaths = new HashMap<String, String>();
    private final Random random = new Random();
    private final ArrayList<GameObject> coins = new ArrayList<GameObject>();
    private final ArrayList<GameObject> opponents = new ArrayList<GameObject>();
    private final ArrayList<GameObject> obstacles = new ArrayList<GameObject>();
    private final ArrayList<GameObject> checkpoints = new ArrayList<GameObject>();
    private final SharedPreferences saves;

    private Bitmap carBitmap, bikeBitmap, roadBitmap, leftRoadBitmap, rightRoadBitmap, coinBitmap, opponentBitmap, leftNavBitmap, rightNavBitmap;
    private String carImagePath = "", bikeImagePath = "", opponentImagePath = "";
    private String carName = "Player";
    private boolean bikeMode = false, raceRunning = false, gameStarted = false, autoScroll = true, infiniteMode = true;
    private boolean fuelEnabled = false, nitroEnabled = false, hudEnabled = true, miniMapEnabled = true;
    private boolean speedometerEnabled = true, trafficEnabled = false, landscapeDefault = true;
    private boolean leftPressed = false, rightPressed = false;
    private boolean accelerometerEnabled = false;
    private float accelerometerSensitivity = 1.2f, accelerometerDeadZone = 0.8f;
    private boolean scoreSidebarOpen = false;
    private float carX = 300, carY = 650, carWidth = 120, carHeight = 190, angle = 0;
    private float speed = 0, acceleration = 0.35f, brakeStrength = 0.65f, maxSpeed = 18f, turnSpeed = 4f;
    private float weight = 1f, grip = 0.85f, friction = 0.04f, roadGrip = 1f, speedMultiplier = 1f;
    private float roadWidth = 720, roadHeight = 1280, roadSpeed = 6f, roadOffset = 0;
    private String roadType = "City", scrollDirection = "Vertical", weather = "Sunny", theme = "City";
    private int roadLanes = 3;
    private String roadSurface = "Asphalt", roadMarking = "Dashed", roadSideStyle = "City", roadDifficulty = "Normal";
    private int score = 0, coinsCollected = 0, health = 100, fuel = 100, nitro = 100, lap = 1, maxLap = 3;
    private String currentPlayerId = "Player";
    private long raceStartMs = 0, countdownSeconds = 0;
    private String speedometerStyle = "Digital", cameraMode = "Follow Car";
    private String engineSound = "", brakeSound = "", crashSound = "", hornSound = "", coinSound = "", nitroSound = "", victorySound = "", gameOverSound = "";
    private String carTint = "", wheelImage = "", spoilerImage = "", exhaustImage = "", headlightImage = "";
    private float wheelSize = 1f, bodyScale = 1f;
    private float cameraZoom = 1f, cameraOffsetX = 0, cameraOffsetY = 0;
    private Runnable loop = new Runnable() { @Override public void run() { tick(); if (raceRunning) handler.postDelayed(this, 16); } };

    public EasyRacerEngine(ComponentContainer container) {
        super(container);
        this.container = container;
        this.view = new RacerView(container.$context());
        this.saves = container.$context().getSharedPreferences("EasyRacerEngine", Context.MODE_PRIVATE);
        container.$add(this);
        setLandscapeMode(true);
        Width(ViewGroup.LayoutParams.MATCH_PARENT);
        Height(ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override public View getView() { return view; }

    @SimpleFunction(description = "Adds the EasyRacer Engine game surface into an Arrangement component.")
    public void AddToArrangement(AndroidViewComponent arrangement) {
        ViewGroup parent = (ViewGroup) arrangement.getView();
        if (view.getParent() != null) ((ViewGroup) view.getParent()).removeView(view);
        parent.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @SimpleFunction(description = "Creates the player car using current car properties.") public void CreateCar() { bikeMode = false; ResetCar(); }
    @SimpleFunction(description = "Creates the player bike using the same engine and bike image.") public void CreateBike() { bikeMode = true; ResetCar(); }
    @SimpleFunction(description = "Deletes the current player vehicle from the race.") public void DeleteCar() { raceRunning = false; speed = 0; view.invalidate(); }
    @SimpleFunction(description = "Resets vehicle position, speed, health, fuel, nitro, and lap.") public void ResetCar() { carX = Math.max(80, view.getWidth() / 2f); carY = Math.max(180, view.getHeight() - 220); speed = 0; angle = 0; health = 100; fuel = 100; nitro = 100; lap = 1; view.invalidate(); }
    @SimpleFunction(description = "Respawns the vehicle after a crash.") public void Respawn() { speed = 0; health = Math.max(1, health); carX = view.getWidth() / 2f; carY = view.getHeight() - 220; CarStopped(); view.invalidate(); }
    @SimpleFunction(description = "Starts the game from the on-screen Start button or a block. This hides Start, resets the race timer, and begins physics, scoring, AI, collisions, and HUD updates.") public void StartRace() { if (!raceRunning) { gameStarted = true; raceRunning = true; raceStartMs = System.currentTimeMillis(); RaceStarted(); handler.post(loop); view.invalidate(); } }
    @SimpleFunction(description = "Pauses the game and shows the on-screen Play button so the race can resume without resetting score, vehicle, or timer.") public void PauseRace() { raceRunning = false; view.invalidate(); }
    @SimpleFunction(description = "Resumes a paused race. Use this for a Play button block after PauseRace; it does not reset score or position.") public void PlayRace() { if (gameStarted && !raceRunning) { raceRunning = true; handler.post(loop); RaceResumed(); view.invalidate(); } }
    @SimpleFunction(description = "Finishes the race, stores the score in the built-in database, and dispatches RaceFinished.") public void FinishRace() { raceRunning = false; gameStarted = false; StoreScore(currentPlayerId, score); RaceFinished(score); view.invalidate(); }
    @SimpleFunction(description = "Stops the current race from the on-screen Stop button without saving a finish score. Speed is cleared and the Start button becomes available again.") public void StopRace() { raceRunning = false; gameStarted = false; speed = 0; leftPressed = false; rightPressed = false; CarStopped(); view.invalidate(); }


    @SimpleFunction(description = "Sets the screen to landscape by default for a real racing-game feel. Call with false only if you want to return control to the app screen orientation.") public void SetLandscapeMode(boolean enabled) { landscapeDefault = enabled; setLandscapeMode(enabled); }
    @SimpleFunction(description = "Creates and balances a road preset. Inputs: type (City, Highway, Desert, Snow, Dirt, Track, Cyberpunk), lanes (visible lanes), surface (asphalt, dirt, sand, ice), marking (dashed, solid, double), sideStyle (city, forest, desert, snow), grip, friction, speed, width, and difficulty. The engine arranges these into road physics, visuals, lane lines, scenery, and scrolling speed automatically.") public void ConfigureRoadBlock(String type, int lanes, String surface, String marking, String sideStyle, float gripValue, float frictionValue, float speedValue, float widthValue, String difficulty) { roadType = type; roadLanes = Math.max(1, lanes); roadSurface = surface; roadMarking = marking; roadSideStyle = sideStyle; roadGrip = gripValue; friction = Math.max(0, frictionValue); roadSpeed = Math.max(0, speedValue); roadWidth = Math.max(280, widthValue); roadDifficulty = difficulty; applyRoadType(); view.invalidate(); }
    @SimpleFunction(description = "One-block road builder for common road types. Input: type. Supported values are City, Highway, Desert, Snow, Dirt, Track, Forest, Beach, Mountain, and Cyberpunk. The block chooses safe defaults for grip, friction, lanes, colors, roadside scenery, and speed.") public void CreateRoadTypeBlock(String type) { roadType = type; applyRoadPreset(type); view.invalidate(); }
    @SimpleFunction(description = "Enables accelerometer steering. Inputs: enabled, sensitivity, and deadZone. Connect an AccelerometerSensor.Changed event to NavigateWithAccelerometer(xAccel, yAccel, zAccel) after enabling this block.") public void EnableAccelerometerNavigation(boolean enabled, float sensitivity, float deadZone) { accelerometerEnabled = enabled; accelerometerSensitivity = Math.max(0.1f, sensitivity); accelerometerDeadZone = Math.max(0, deadZone); }
    @SimpleFunction(description = "Navigates the car from AccelerometerSensor readings. Input xAccel steers left/right, yAccel accelerates or brakes, and zAccel is accepted for compatibility. Use inside AccelerometerSensor.AccelerationChanged.") public void NavigateWithAccelerometer(float xAccel, float yAccel, float zAccel) { if (!accelerometerEnabled) return; if (Math.abs(xAccel) > accelerometerDeadZone) { float oldTurn = turnSpeed; turnSpeed *= accelerometerSensitivity; moveSideways(xAccel > 0 ? -1 : 1); turnSpeed = oldTurn; } if (yAccel < -accelerometerDeadZone) Accelerate(); else if (yAccel > accelerometerDeadZone) Brake(); }
    @SimpleFunction(description = "Stores a score in the built-in local database using SharedPreferences. Inputs: playerId and score. The block also updates the high score automatically.") public void StoreScore(String playerId, int value) { String id = cleanPlayer(playerId); int best = Math.max(value, saves.getInt("score_" + id, 0)); saves.edit().putInt("score_" + id, best).putInt("lastScore", value).putInt("highScore", Math.max(value, saves.getInt("highScore", 0))).putString("highScorePlayer", value >= saves.getInt("highScore", 0) ? id : saves.getString("highScorePlayer", id)).apply(); }
    @SimpleFunction(description = "Retrieves a stored score from the built-in local database. Input: playerId. Returns that player's best saved score.") public int RetrieveScore(String playerId) { return saves.getInt("score_" + cleanPlayer(playerId), 0); }
    @SimpleFunction(description = "Returns the highest score stored in the built-in local database.") public int RetrieveHighScore() { return saves.getInt("highScore", 0); }
    @SimpleFunction(description = "Opens the professional score sidebar overlay, showing current score, saved score, and High Score label beside the highest score.") public void OpenScoreSidebar() { scoreSidebarOpen = true; view.invalidate(); }
    @SimpleFunction(description = "Closes the score sidebar overlay so players can focus on driving.") public void CloseScoreSidebar() { scoreSidebarOpen = false; view.invalidate(); }

    @SimpleFunction(description = "Accelerates the vehicle. Use with a button or clock for manual control.") public void Accelerate() { if (fuelEnabled && fuel <= 0) { FuelEmpty(); return; } speed = Math.min(maxSpeed * speedMultiplier, speed + acceleration / Math.max(0.2f, weight)); CarMoving(); }
    @SimpleFunction(description = "Brakes the vehicle and supports reverse at low speed.") public void Brake() { speed = Math.max(-maxSpeed * 0.35f, speed - brakeStrength); }
    @SimpleFunction(description = "Moves the car left without rotating it, keeping the car facing forward.") public void TurnLeft() { MoveLeft(); }
    @SimpleFunction(description = "Moves the car right without rotating it, keeping the car facing forward.") public void TurnRight() { MoveRight(); }
    @SimpleFunction(description = "Moves the car left without rotating it, keeping the car facing forward.") public void MoveLeft() { moveSideways(-1); }
    @SimpleFunction(description = "Moves the car right without rotating it, keeping the car facing forward.") public void MoveRight() { moveSideways(1); }
    @SimpleFunction(description = "Uses nitro boost if enabled and available.") public void UseNitro() { if (nitroEnabled && nitro > 0) { nitro = Math.max(0, nitro - 8); speed = Math.min(maxSpeed * 1.7f, speed + 5); NitroStarted(); if (nitro == 0) NitroEnded(); } }

    @SimpleFunction(description = "Sets player car image from an uploaded App Inventor asset filename, asset path, URL, or file path. Example: icon.png") public void SetCarImage(String path) { carImagePath = cleanPath(path); carBitmap = load(carImagePath); view.invalidate(); }
    @SimpleFunction(description = "Sets player bike image from asset path or file path.") public void SetBikeImage(String path) { bikeImagePath = cleanPath(path); bikeBitmap = load(bikeImagePath); view.invalidate(); }
    @SimpleFunction(description = "Sets the centered main scrolling road image from asset path or file path. When set, the built-in generated road is hidden and the image fills the middle 70% road area.") public void SetRoadImage(String path) { roadBitmap = load(path); }
    @SimpleFunction(description = "Sets the scrolling left road-side image from asset path or file path. It fills the left 15% side area by default.") public void SetLeftRoadImage(String path) { leftRoadBitmap = load(path); }
    @SimpleFunction(description = "Sets the scrolling right road-side image from asset path or file path. It fills the right 15% side area by default.") public void SetRightRoadImage(String path) { rightRoadBitmap = load(path); }
    @SimpleFunction(description = "Sets road width in pixels.") public void SetRoadWidth(float width) { roadWidth = width; }
    @SimpleFunction(description = "Sets road height in pixels.") public void SetRoadHeight(float height) { roadHeight = height; }
    @SimpleFunction(description = "Sets default opponent image from an uploaded App Inventor asset filename, asset path, URL, or file path. Example: opponent.png. If no opponent exists yet, one is placed on the road immediately so it is visible before the race starts, like the main car.") public void SetOpponentImage(String path) { opponentImagePath = cleanPath(path); opponentImagePaths.put(cleanOpponentName("Opponent"), opponentImagePath); opponentBitmap = load(opponentImagePath); if (opponentBitmap != null) opponentImages.put(cleanOpponentName("Opponent"), opponentBitmap); ensureDefaultOpponentVisible(); view.invalidate(); }
    @SimpleFunction(description = "Sets default opponent car image from an uploaded App Inventor asset filename, asset path, URL, or file path. Alias for SetOpponentImage.") public void SetOpponentCarImage(String path) { SetOpponentImage(path); }
    @SimpleProperty(description = "Default opponent car image filename, asset path, URL, or file path. This property is the same as SetOpponentCarImage, for projects that use the opponentCarImage property block.") public void OpponentCarImage(String path) { SetOpponentImage(path); }
    @SimpleProperty(description = "Returns the current default opponent car image path.") public String OpponentCarImage() { return opponentImagePath; }
    @SimpleFunction(description = "Names an opponent car or bike and assigns its image. Use the same name when creating or randomly spawning that opponent.") public void SetOpponentVehicle(String name, String imagePath) { String cleanedPath = cleanPath(imagePath); String key = cleanOpponentName(name); if (cleanedPath.length() > 0) opponentImagePaths.put(key, cleanedPath); Bitmap b = load(cleanedPath); if (b != null) opponentImages.put(key, b); if (opponentBitmap == null && b != null) opponentBitmap = b; view.invalidate(); }
    @SimpleFunction(description = "Alias for SetOpponentVehicle for projects that call the block SetOpponentVehicleImage(name, imagePath).") public void SetOpponentVehicleImage(String name, String imagePath) { SetOpponentVehicle(name, imagePath); }
    @SimpleFunction(description = "Sets default coin image from an uploaded App Inventor asset filename, asset path, URL, or file path. Example: coin.png") public void SetCoinImage(String path) { coinBitmap = load(path); }
    @SimpleFunction(description = "Sets the left navigation button image from an uploaded App Inventor asset filename, asset path, URL, or file path.") public void SetLeftNavigationButtonImage(String path) { leftNavBitmap = load(path); }
    @SimpleFunction(description = "Sets the right navigation button image from an uploaded App Inventor asset filename, asset path, URL, or file path.") public void SetRightNavigationButtonImage(String path) { rightNavBitmap = load(path); }
    @SimpleFunction(description = "Sets the left arrow button image. Alias for SetLeftNavigationButtonImage.") public void SetLeftArrowButtonImage(String path) { SetLeftNavigationButtonImage(path); }
    @SimpleFunction(description = "Sets the right arrow button image. Alias for SetRightNavigationButtonImage.") public void SetRightArrowButtonImage(String path) { SetRightNavigationButtonImage(path); }
    @SimpleFunction(description = "Creates an AI opponent at x,y and immediately redraws the race so the opponent is visible.") public void CreateOpponent(float x, float y) { addOpponent(cleanOpponentName("Opponent"), null, x, y, 120, 190); }
    @SimpleFunction(description = "Creates a named AI opponent car or bike at x,y with an optional image path and immediately redraws it.") public void CreateOpponentVehicle(String name, String imagePath, float x, float y) { addOpponent(cleanOpponentName(name), imagePath, x, y, 120, 190); }
    @SimpleFunction(description = "Creates a named AI opponent car or bike at x,y with custom size and an optional image path.") public void CreateOpponentVehicleWithSize(String name, String imagePath, float x, float y, float width, float height) { addOpponent(cleanOpponentName(name), imagePath, x, y, width, height); }
    @SimpleFunction(description = "Places the whole named opponent car or bike at a random lane position on the road. Before the race starts it appears on screen immediately; during a race it usually spawns above the screen so it drives into view.") public void SpawnRandomOpponent(String name) { addOpponent(cleanOpponentName(name), null, randomRoadX(120), randomOpponentY(190), 120, 190); }
    @SimpleFunction(description = "Places a named opponent car or bike with an image at a random lane position on the road. Before the race starts it appears on screen immediately; during a race it usually spawns above the screen so it drives into view.") public void SpawnRandomOpponentVehicle(String name, String imagePath) { addOpponent(cleanOpponentName(name), imagePath, randomRoadX(120), randomOpponentY(190), 120, 190); }
    @SimpleFunction(description = "Alias for SpawnRandomOpponentVehicle for projects that use opponent car wording.") public void SpawnRandomOpponentCar(String name, String imagePath) { SpawnRandomOpponentVehicle(name, imagePath); }
    @SimpleFunction(description = "Spawns a coin at x,y.") public void SpawnCoin(float x, float y) { coins.add(new GameObject(x, y, 44, 44, "coin")); }
    @SimpleFunction(description = "Creates a checkpoint rectangle.") public void CreateCheckpoint(float x, float y, float width, float height) { checkpoints.add(new GameObject(x, y, width, height, "checkpoint")); }
    @SimpleFunction(description = "Adds an obstacle such as tree, rock, cone, oil, water, fire, or pothole.") public void CreateObstacle(String type, float x, float y, float width, float height) { obstacles.add(new GameObject(x, y, width, height, type)); }
    @SimpleFunction(description = "Spawns a built-in power up: Shield, Nitro, Double Coin, Repair, Slow Motion, Magnet, or Invincible.") public void SpawnPowerUp(String type, float x, float y) { obstacles.add(new GameObject(x, y, 58, 58, "PowerUp:" + type)); }
    @SimpleFunction(description = "Stores a sound asset path for Engine, Brake, Crash, Horn, Coin, Nitro, Victory, or Game Over.") public void SetSound(String name, String path) { String n = name == null ? "" : name.toLowerCase(); if (n.contains("engine")) engineSound = path; else if (n.contains("brake")) brakeSound = path; else if (n.contains("crash")) crashSound = path; else if (n.contains("horn")) hornSound = path; else if (n.contains("coin")) coinSound = path; else if (n.contains("nitro")) nitroSound = path; else if (n.contains("victory")) victorySound = path; else if (n.contains("over")) gameOverSound = path; }
    @SimpleFunction(description = "Configures simple car customization: tint, wheel size, and optional accessory images.") public void CustomizeCar(String tint, float wheelSizeValue, String wheelPath, String spoilerPath, String exhaustPath, String headlightPath) { carTint = tint; wheelSize = wheelSizeValue; wheelImage = wheelPath; spoilerImage = spoilerPath; exhaustImage = exhaustPath; headlightImage = headlightPath; }
    @SimpleFunction(description = "Sets a fixed player vehicle size in pixels.") public void SetVehicleSize(float width, float height) { carWidth = Math.max(40, width); carHeight = Math.max(60, height); view.invalidate(); }
    @SimpleFunction(description = "Sets body scale for simple visual customization from the default larger fixed vehicle size.") public void SetBodyScale(float scale) { bodyScale = Math.max(0.2f, scale); SetVehicleSize(120f * bodyScale, 190f * bodyScale); }
    @SimpleFunction(description = "Enables or disables automatic fuel usage.") public void EnableFuel(boolean enabled) { fuelEnabled = enabled; }
    @SimpleFunction(description = "Adds fuel up to 100.") public void AddFuel(int amount) { fuel = clamp(fuel + amount, 0, 100); }
    @SimpleFunction(description = "Enables or disables nitro.") public void EnableNitro(boolean enabled) { nitroEnabled = enabled; }
    @SimpleFunction(description = "Repairs health up to 100.") public void Repair(int amount) { health = clamp(health + amount, 0, 100); }
    @SimpleFunction(description = "Applies damage and triggers crash events when needed.") public void Damage(int amount) { health = clamp(health - amount, 0, 100); if (health == 0) { CarCrash(); PlayerLose(); } }
    @SimpleFunction(description = "Returns score from coins, distance, speed, laps, and time.") public int GetScore() { return score; }
    @SimpleFunction(description = "Resets score, coins, timer, and distance-based progress.") public void ResetScore() { score = 0; coinsCollected = 0; raceStartMs = System.currentTimeMillis(); }
    @SimpleFunction(description = "Saves coins, high score, unlocks, settings, car name, theme, and road type.") public void Save() { saves.edit().putInt("coins", coinsCollected).putInt("score", score).putString("carName", carName).putString("theme", theme).putString("roadType", roadType).apply(); }
    @SimpleFunction(description = "Loads saved coins, score, settings, car name, theme, and road type.") public void Load() { coinsCollected = saves.getInt("coins", 0); score = saves.getInt("score", 0); carName = saves.getString("carName", carName); theme = saves.getString("theme", theme); roadType = saves.getString("roadType", roadType); applyRoadType(); }
    @SimpleFunction(description = "Applies a one-click template: Endless Highway, Formula Track, City Traffic, Desert Rally, or Motorcycle Challenge.") public void ApplyTemplate(String template) { theme = template; trafficEnabled = template.toLowerCase().contains("traffic"); roadType = template.toLowerCase().contains("desert") ? "Desert" : template.toLowerCase().contains("formula") ? "Track" : "Highway"; applyRoadType(); }
    @SimpleFunction(description = "Applies a one-click theme: City, Village, Desert, Snow, Space, Cyberpunk, Forest, Beach, or Mountain.") public void ApplyTheme(String name) { theme = name; roadType = name; applyRoadType(); }
    @SimpleFunction(description = "Returns a compact JSON snapshot for multiplayer synchronization.") public String MultiplayerSnapshot() { try { JSONObject o = new JSONObject(); o.put("x", carX); o.put("y", carY); o.put("speed", speed); o.put("lap", lap); o.put("score", score); return o.toString(); } catch (Exception e) { return "{}"; } }

    @SimpleProperty public float CarSpeed() { return speed; } @SimpleProperty public void CarSpeed(float v) { speed = v; }
    @SimpleProperty public float Acceleration() { return acceleration; } @SimpleProperty public void Acceleration(float v) { acceleration = v; }
    @SimpleProperty public float BrakeStrength() { return brakeStrength; } @SimpleProperty public void BrakeStrength(float v) { brakeStrength = v; }
    @SimpleProperty public float MaximumSpeed() { return maxSpeed; } @SimpleProperty public void MaximumSpeed(float v) { maxSpeed = v; }
    @SimpleProperty public float TurningSpeed() { return turnSpeed; } @SimpleProperty public void TurningSpeed(float v) { turnSpeed = v; }
    @SimpleProperty public float CarWidth() { return carWidth; } @SimpleProperty public void CarWidth(float v) { carWidth = v; }
    @SimpleProperty public float CarHeight() { return carHeight; } @SimpleProperty public void CarHeight(float v) { carHeight = v; }
    @SimpleProperty public String CarName() { return carName; } @SimpleProperty public void CarName(String v) { carName = v; currentPlayerId = cleanPlayer(v); }
    @SimpleProperty public float Weight() { return weight; } @SimpleProperty public void Weight(float v) { weight = Math.max(0.1f, v); }
    @SimpleProperty public float Grip() { return grip; } @SimpleProperty public void Grip(float v) { grip = v; }
    @SimpleProperty public float RoadFriction() { return friction; } @SimpleProperty public void RoadFriction(float v) { friction = v; }
    @SimpleProperty public float RoadGrip() { return roadGrip; } @SimpleProperty public void RoadGrip(float v) { roadGrip = v; }
    @SimpleProperty public String RoadType() { return roadType; } @SimpleProperty public void RoadType(String v) { roadType = v; applyRoadType(); }
    @SimpleProperty public float SpeedMultiplier() { return speedMultiplier; } @SimpleProperty public void SpeedMultiplier(float v) { speedMultiplier = v; }
    @SimpleProperty public float RoadWidth() { return roadWidth; } @SimpleProperty public void RoadWidth(float v) { roadWidth = v; }
    @SimpleProperty public float RoadHeight() { return roadHeight; } @SimpleProperty public void RoadHeight(float v) { roadHeight = v; }
    @SimpleProperty public float RoadSpeed() { return roadSpeed; } @SimpleProperty public void RoadSpeed(float v) { roadSpeed = v; }
    @SimpleProperty public boolean AutoScroll() { return autoScroll; } @SimpleProperty public void AutoScroll(boolean v) { autoScroll = v; }
    @SimpleProperty public boolean InfiniteMode() { return infiniteMode; } @SimpleProperty public void InfiniteMode(boolean v) { infiniteMode = v; }
    @SimpleProperty public String ScrollDirection() { return scrollDirection; } @SimpleProperty public void ScrollDirection(String v) { scrollDirection = v; }
    @SimpleProperty public int FuelLevel() { return fuel; }
    @SimpleProperty public int NitroAmount() { return nitro; }
    @SimpleProperty public int Health() { return health; } @SimpleProperty public void Health(int v) { health = clamp(v, 0, 100); }
    @SimpleProperty public int CurrentLap() { return lap; }
    @SimpleProperty public int MaximumLap() { return maxLap; } @SimpleProperty public void MaximumLap(int v) { maxLap = Math.max(1, v); }
    @SimpleProperty public int RaceTime() { return raceStartMs == 0 ? 0 : (int)((System.currentTimeMillis() - raceStartMs) / 1000); }
    @SimpleProperty public int Countdown() { return (int)countdownSeconds; } @SimpleProperty public void Countdown(int seconds) { countdownSeconds = seconds; }
    @SimpleProperty public String SpeedometerStyle() { return speedometerStyle; } @SimpleProperty public void SpeedometerStyle(String v) { speedometerStyle = v; }
    @SimpleProperty public boolean HUDEnabled() { return hudEnabled; } @SimpleProperty public void HUDEnabled(boolean v) { hudEnabled = v; }
    @SimpleProperty public boolean MiniMapEnabled() { return miniMapEnabled; } @SimpleProperty public void MiniMapEnabled(boolean v) { miniMapEnabled = v; }
    @SimpleProperty public boolean SpeedometerEnabled() { return speedometerEnabled; } @SimpleProperty public void SpeedometerEnabled(boolean v) { speedometerEnabled = v; }
    @SimpleProperty public String CameraMode() { return cameraMode; } @SimpleProperty public void CameraMode(String v) { cameraMode = v; }
    @SimpleProperty public float CameraZoom() { return cameraZoom; } @SimpleProperty public void CameraZoom(float v) { cameraZoom = Math.max(0.25f, v); }
    @SimpleProperty public float CameraOffsetX() { return cameraOffsetX; } @SimpleProperty public void CameraOffsetX(float v) { cameraOffsetX = v; }
    @SimpleProperty public float CameraOffsetY() { return cameraOffsetY; } @SimpleProperty public void CameraOffsetY(float v) { cameraOffsetY = v; }
    @SimpleProperty public String Weather() { return weather; } @SimpleProperty public void Weather(String v) { weather = v; applyWeather(); }
    @SimpleProperty public boolean TrafficEnabled() { return trafficEnabled; } @SimpleProperty public void TrafficEnabled(boolean v) { trafficEnabled = v; }

    @SimpleEvent public void RaceStarted() { EventDispatcher.dispatchEvent(this, "RaceStarted"); }
    @SimpleEvent public void RaceResumed() { EventDispatcher.dispatchEvent(this, "RaceResumed"); }
    @SimpleEvent public void RaceFinished(int finalScore) { EventDispatcher.dispatchEvent(this, "RaceFinished", finalScore); }
    @SimpleEvent public void CarStopped() { EventDispatcher.dispatchEvent(this, "CarStopped"); }
    @SimpleEvent public void CarMoving() { EventDispatcher.dispatchEvent(this, "CarMoving"); }
    @SimpleEvent public void NitroStarted() { EventDispatcher.dispatchEvent(this, "NitroStarted"); }
    @SimpleEvent public void NitroEnded() { EventDispatcher.dispatchEvent(this, "NitroEnded"); }
    @SimpleEvent public void FuelEmpty() { EventDispatcher.dispatchEvent(this, "FuelEmpty"); }
    @SimpleEvent public void CheckpointReached(int index) { EventDispatcher.dispatchEvent(this, "CheckpointReached", index); }
    @SimpleEvent public void CoinCollected(int value, int totalCoins) { EventDispatcher.dispatchEvent(this, "CoinCollected", value, totalCoins); }
    @SimpleEvent public void WhenCarHitsWall() { EventDispatcher.dispatchEvent(this, "WhenCarHitsWall"); }
    @SimpleEvent public void WhenCarHitsCoin() { EventDispatcher.dispatchEvent(this, "WhenCarHitsCoin"); }
    @SimpleEvent public void WhenCarHitsPowerup(String type) { EventDispatcher.dispatchEvent(this, "WhenCarHitsPowerup", type); }
    @SimpleEvent public void WhenCarCrash() { EventDispatcher.dispatchEvent(this, "WhenCarCrash"); }
    @SimpleEvent public void CarCrash() { EventDispatcher.dispatchEvent(this, "CarCrash"); }
    @SimpleEvent public void PlayerWin() { EventDispatcher.dispatchEvent(this, "PlayerWin"); }
    @SimpleEvent public void PlayerLose() { EventDispatcher.dispatchEvent(this, "PlayerLose"); }
    @SimpleEvent public void TimeFinished() { EventDispatcher.dispatchEvent(this, "TimeFinished"); }

    private void tick() {
        if (leftPressed) MoveLeft(); if (rightPressed) MoveRight();
        if (autoScroll) roadOffset += Math.max(roadSpeed, Math.abs(speed));
        speed *= Math.max(0, 1f - friction - (1f - grip * roadGrip) * 0.025f);
        if (Math.abs(speed) < 0.05f) { if (speed != 0) CarStopped(); speed = 0; }
        carY -= speed;
        if (infiniteMode) { carY = Math.max(80, Math.min(view.getHeight() - 80, carY)); }
        if (fuelEnabled && Math.abs(speed) > 0.2f) { fuel = clamp(fuel - 1, 0, 100); if (fuel == 0) FuelEmpty(); }
        score = Math.max(score, RaceTime()) + Math.max(0, (int)Math.abs(speed));
        pushBackToMainRoad();
        updateObjects(coins, roadSpeed); updateObjects(opponents, roadSpeed * 0.8f); updateObjects(obstacles, roadSpeed); updateObjects(checkpoints, roadSpeed);
        checkCollisions();
        if (countdownSeconds > 0 && RaceTime() >= countdownSeconds) { countdownSeconds = 0; TimeFinished(); FinishRace(); }
        view.invalidate();
    }
    private void updateObjects(ArrayList<GameObject> list, float dy) {
        for (GameObject o : list) {
            if (!autoScroll) continue;
            if (o.type != null && o.type.toLowerCase().contains("opponent")) updateOpponent(o, dy);
            else o.y += dy;
        }
    }
    private void updateOpponent(GameObject o, float dy) {
        if (o.laneX <= 0) o.laneX = nearestLaneCenter(o.x, o.w);
        o.y += dy + o.vy;
        float steer = o.laneX - o.x;
        o.x += Math.max(-2.8f, Math.min(2.8f, steer * 0.035f));
        float left = mainRoadLeft() + o.w / 2f + 10f;
        float right = mainRoadRight() - o.w / 2f - 10f;
        if (view.getWidth() > 0 && right > left) o.x = Math.max(left, Math.min(right, o.x));
        if (view.getHeight() > 0 && o.y - o.h / 2f > view.getHeight() + 80f) {
            o.y = randomSpawnY(o.h);
            o.laneX = randomRoadX(o.w);
            o.x = o.laneX;
        }
    }
    private void moveSideways(int dir) { float effectiveGrip = Math.max(0.1f, grip * roadGrip); angle = 0; carX += dir * steeringMoveDistance(effectiveGrip); if (effectiveGrip < 0.35f) carX += dir * Math.abs(speed) * 0.6f; }
    private float steeringMoveDistance(float effectiveGrip) { return Math.max(7f, Math.abs(speed) * 1.35f) * effectiveGrip; }
    private void checkCollisions() {
        RectF car = rect(carX, carY, carWidth, carHeight);
        float left = mainRoadLeft(); float right = mainRoadRight();
        if (car.left < left || car.right > right) { WhenCarHitsWall(); Damage(5); speed *= -0.25f; carX = Math.max(left + carWidth/2, Math.min(right - carWidth/2, carX)); }
        for (int i = coins.size() - 1; i >= 0; i--) if (RectF.intersects(car, coins.get(i).rect())) { coins.remove(i); coinsCollected++; score += 100; WhenCarHitsCoin(); CoinCollected(100, coinsCollected); }
        for (GameObject o : obstacles) if (RectF.intersects(car, o.rect())) { if (o.type.toLowerCase().contains("powerup")) { WhenCarHitsPowerup(o.type); applyPowerUp(o.type); } else if (o.type.toLowerCase().contains("oil") || o.type.toLowerCase().contains("water")) { WhenCarHitsPowerup(o.type); grip *= 0.7f; } else { WhenCarCrash(); CarCrash(); Damage(15); speed *= -0.4f; } }
        for (GameObject o : opponents) if (RectF.intersects(car, o.rect())) { WhenCarCrash(); CarCrash(); Damage(20); speed *= -0.5f; }
        for (int i = 0; i < checkpoints.size(); i++) if (RectF.intersects(car, checkpoints.get(i).rect())) { CheckpointReached(i + 1); if (i == checkpoints.size() - 1) { lap++; if (lap > maxLap) { PlayerWin(); FinishRace(); } } }
    }
    private float mainRoadLeft() { return view.getWidth() * 0.15f; }
    private float mainRoadRight() { return view.getWidth() * 0.85f; }
    private void pushBackToMainRoad() {
        if (view.getWidth() <= 0) return;
        float leftLimit = mainRoadLeft() + carWidth / 2f;
        float rightLimit = mainRoadRight() - carWidth / 2f;
        if (carX < leftLimit) carX = Math.min(leftLimit, carX + Math.max(3f, roadSpeed * 0.9f));
        else if (carX > rightLimit) carX = Math.max(rightLimit, carX - Math.max(3f, roadSpeed * 0.9f));
    }
    private RectF rect(float x, float y, float w, float h) { return new RectF(x - w/2, y - h/2, x + w/2, y + h/2); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private void applyRoadType() { String t = roadType == null ? "" : roadType.toLowerCase(); if (t.contains("ice") || t.contains("snow")) { roadGrip = Math.min(roadGrip, 0.35f); friction = 0.015f; } else if (t.contains("dirt") || t.contains("desert") || t.contains("sand")) { roadGrip = Math.min(roadGrip, 0.6f); friction = 0.07f; } else if (roadGrip <= 0) { roadGrip = 1f; friction = 0.04f; } }
    private void applyRoadPreset(String type) { String t = type == null ? "" : type.toLowerCase(); roadLanes = t.contains("track") ? 2 : t.contains("highway") ? 4 : 3; roadSurface = t.contains("desert") || t.contains("beach") ? "Sand" : t.contains("dirt") || t.contains("forest") || t.contains("mountain") ? "Dirt" : t.contains("snow") ? "Ice" : "Asphalt"; roadMarking = t.contains("track") ? "Solid" : "Dashed"; roadSideStyle = type; roadDifficulty = t.contains("snow") || t.contains("mountain") ? "Hard" : "Normal"; roadGrip = t.contains("snow") ? 0.25f : t.contains("dirt") || t.contains("desert") ? 0.55f : 1f; friction = t.contains("snow") ? 0.015f : t.contains("dirt") || t.contains("desert") ? 0.07f : 0.04f; roadSpeed = t.contains("highway") ? 9f : t.contains("track") ? 7f : 6f; }
    private void setLandscapeMode(boolean enabled) { try { Activity a = (Activity) container.$context(); a.setRequestedOrientation(enabled ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); } catch (Exception ignored) { } }
    private String cleanPlayer(String playerId) { return playerId == null || playerId.trim().length() == 0 ? "Player" : playerId.trim(); }
    private String cleanOpponentName(String name) { return name == null || name.trim().length() == 0 ? "Opponent" : name.trim(); }
    private void addOpponent(String name, String imagePath, float x, float y, float width, float height) {
        String key = cleanOpponentName(name);
        String cleanedPath = cleanPath(imagePath);
        if (cleanedPath.length() > 0) opponentImagePaths.put(key, cleanedPath);
        Bitmap b = load(cleanedPath);
        if (b != null) opponentImages.put(key, b);
        else if (!opponentImages.containsKey(key) && opponentBitmap != null) opponentImages.put(key, opponentBitmap);
        float safeWidth = Math.max(30, width);
        float safeHeight = Math.max(40, height);
        boolean viewReady = view.getWidth() > 0 && view.getHeight() > 0;
        float spawnX = x > 0 && viewReady ? x : randomRoadX(safeWidth);
        float spawnY = y > 0 && viewReady ? y : Math.max(safeHeight / 2f + 12f, viewReady ? view.getHeight() * 0.25f : safeHeight / 2f + 12f);
        GameObject opponent = new GameObject(spawnX, spawnY, safeWidth, safeHeight, "opponent", key);
        opponent.needsLayoutPosition = !viewReady;
        opponent.imagePath = cleanedPath.length() > 0 ? cleanedPath : opponentImagePaths.containsKey(key) ? opponentImagePaths.get(key) : opponentImagePath;
        opponent.vy = Math.max(1.5f, roadSpeed * 0.35f);
        opponent.laneX = nearestLaneCenter(spawnX, safeWidth);
        opponents.add(opponent);
        view.invalidate();
    }
    private void placePendingOpponentsOnRoad() {
        for (GameObject opponent : opponents) {
            if (!opponent.needsLayoutPosition) continue;
            opponent.x = randomRoadX(opponent.w);
            opponent.y = Math.max(opponent.h / 2f + 12f, view.getHeight() * 0.25f);
            opponent.laneX = nearestLaneCenter(opponent.x, opponent.w);
            opponent.needsLayoutPosition = false;
        }
    }
    private float randomRoadX(float objectWidth) {
        float left = mainRoadLeft() + objectWidth / 2f + 12f;
        float right = mainRoadRight() - objectWidth / 2f - 12f;
        if (view.getWidth() <= 0 || right <= left) return Math.max(80, view.getWidth() / 2f);
        return left + random.nextFloat() * (right - left);
    }

    private void ensureDefaultOpponentVisible() {
        if (!opponents.isEmpty()) return;
        addOpponent(cleanOpponentName("Opponent"), opponentImagePath, randomRoadX(120), randomOpponentY(190), 120, 190);
    }
    private float randomOpponentY(float objectHeight) {
        if (gameStarted || raceRunning) return randomSpawnY(objectHeight);
        float h = view.getHeight() > 0 ? view.getHeight() : 720f;
        return Math.max(objectHeight / 2f + 12f, h * 0.28f);
    }
    private float randomSpawnY(float objectHeight) {
        float h = view.getHeight() > 0 ? view.getHeight() : 720f;
        return -objectHeight / 2f - random.nextFloat() * Math.max(objectHeight, h * 0.65f);
    }
    private float nearestLaneCenter(float x, float objectWidth) {
        int lanes = Math.max(1, roadLanes);
        float left = mainRoadLeft();
        float laneWidth = Math.max(objectWidth + 24f, (mainRoadRight() - left) / lanes);
        int lane = Math.max(0, Math.min(lanes - 1, (int)((x - left) / laneWidth)));
        return left + laneWidth * lane + laneWidth / 2f;
    }
    private void applyPowerUp(String type) { String t = type == null ? "" : type.toLowerCase(); if (t.contains("nitro")) nitro = 100; if (t.contains("repair")) Repair(30); if (t.contains("double")) score += 200; if (t.contains("shield") || t.contains("invincible")) health = 100; if (t.contains("slow")) roadSpeed *= 0.7f; }
    private void applyWeather() { String w = weather == null ? "" : weather.toLowerCase(); if (w.contains("rain")) roadGrip *= 0.75f; if (w.contains("snow")) roadGrip *= 0.5f; if (w.contains("storm")) { roadGrip *= 0.65f; roadSpeed *= 0.9f; } }
    private String cleanPath(String path) { return path == null ? "" : path.trim(); }
    private Bitmap load(String path) {
        String key = cleanPath(path);
        if (key.length() == 0) return null;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        Bitmap b = decodeBitmap(key);
        if (b == null && key.startsWith("/android_asset/")) b = decodeBitmap("file://" + key);
        if (b == null && key.indexOf('/') < 0) b = decodeBitmap("file:///android_asset/" + key);
        if (b == null && key.indexOf('/') < 0) b = decodeBitmap("file:///android_asset/assets/" + key);
        if (b == null && key.indexOf('/') < 0) b = decodeBitmap("assets/" + key);
        if (b == null) b = decodeBitmap(new File(key).getName());
        if (b != null) imageCache.put(key, b);
        return b;
    }
    private Bitmap decodeBitmap(String key) {
        InputStream in = null;
        try {
            if (key.startsWith("http://") || key.startsWith("https://")) in = new URL(key).openStream();
            else if (key.startsWith("file:///android_asset/")) in = container.$context().getAssets().open(key.substring("file:///android_asset/".length()));
            else if (key.startsWith("file://")) in = new URL(key).openStream();
            else if (key.startsWith("/")) in = new FileInputStream(key);
            else in = MediaUtil.openMedia(container.$form(), key);
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) { }
        }
    }

    private class RacerView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF leftButton = new RectF(), rightButton = new RectF();
        private final RectF startButton = new RectF(), pauseButton = new RectF(), playButton = new RectF(), stopButton = new RectF(), scoreButton = new RectF(), closeSidebarButton = new RectF();
        private final ArrayList<TouchWave> waves = new ArrayList<TouchWave>();
        RacerView(Context c) { super(c); setBackgroundColor(Color.rgb(10, 14, 22)); setFocusable(true); }
        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { super.onSizeChanged(w, h, oldw, oldh); placePendingOpponentsOnRoad(); }
        @Override protected void onDraw(Canvas c) { super.onDraw(c); drawSky(c); drawRoad(c); drawList(c, coins, coinBitmap, Color.YELLOW); drawList(c, obstacles, null, Color.RED); drawList(c, opponents, opponentBitmap, Color.BLUE); drawVehicle(c); if (hudEnabled) drawHud(c); drawControls(c); if (scoreSidebarOpen) drawScoreSidebar(c); }
        @Override public boolean onTouchEvent(MotionEvent e) { float x = e.getX(), y = e.getY(); int action = e.getActionMasked(); boolean down = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE; if (action == MotionEvent.ACTION_DOWN) { addWave(x, y); if (startButton.contains(x, y) && !gameStarted) StartRace(); else if (pauseButton.contains(x, y) && raceRunning) PauseRace(); else if (playButton.contains(x, y) && gameStarted && !raceRunning) PlayRace(); else if (stopButton.contains(x, y) && gameStarted) StopRace(); else if (scoreButton.contains(x, y)) { scoreSidebarOpen = !scoreSidebarOpen; invalidate(); } else if (scoreSidebarOpen && closeSidebarButton.contains(x, y)) CloseScoreSidebar(); } if (!down) { leftPressed = rightPressed = false; invalidate(); return true; } leftPressed = leftButton.contains(x, y);
            rightPressed = rightButton.contains(x, y);
            invalidate(); return true; }
        private void drawSky(Canvas c) { p.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.rgb(16, 27, 45), Color.rgb(5, 8, 14), Shader.TileMode.CLAMP)); c.drawRect(0, 0, getWidth(), getHeight(), p); p.setShader(null); }
        private void drawRoad(Canvas c) {
            float leftSideRight = getWidth() * 0.15f;
            float mainLeft = leftSideRight;
            float mainRight = getWidth() * 0.85f;
            float rightSideLeft = mainRight;
            roadWidth = mainRight - mainLeft;
            drawScrollingImageOrColor(c, leftRoadBitmap, new RectF(0, 0, leftSideRight, getHeight()), sideColor());
            drawScrollingImageOrColor(c, rightRoadBitmap, new RectF(rightSideLeft, 0, getWidth(), getHeight()), sideColor());
            if (roadBitmap != null) {
                drawScrollingImageOrColor(c, roadBitmap, new RectF(mainLeft, 0, mainRight, getHeight()), roadColor());
                return;
            }
            float left = mainLeft;
            p.setShader(new LinearGradient(left, 0, mainRight, 0, new int[] { Color.rgb(35,35,38), roadColor(), Color.rgb(35,35,38) }, null, Shader.TileMode.CLAMP)); c.drawRoundRect(new RectF(left, -18, mainRight, getHeight()+18), 24, 24, p); p.setShader(null); p.setColor(Color.rgb(235,235,220)); p.setStrokeWidth(8); c.drawLine(left + 12, 0, left + 12, getHeight(), p); c.drawLine(mainRight - 12, 0, mainRight - 12, getHeight(), p); p.setStrokeWidth(5); int lanes = Math.max(1, roadLanes); for (int lane = 1; lane < lanes; lane++) { float lx = left + (roadWidth / lanes) * lane; for (int y = (int)(roadOffset % 96) - 96; y < getHeight(); y += 96) c.drawLine(lx, y, lx, y + ("Solid".equalsIgnoreCase(roadMarking) ? 88 : 46), p); } p.setColor(Color.argb(70, 255, 255, 255)); for (int y = (int)(roadOffset % 140) - 140; y < getHeight(); y += 140) c.drawOval(new RectF(left + 35, y, left + 95, y + 25), p);
        }
        private void drawScrollingImageOrColor(Canvas c, Bitmap bitmap, RectF area, int color) {
            if (bitmap == null) { p.setColor(color); c.drawRect(area, p); return; }
            float tileHeight = roadHeight > 0 ? roadHeight : getHeight();
            float top = -roadOffset % tileHeight;
            RectF dst = new RectF(area.left, top, area.right, top + tileHeight);
            while (dst.top < getHeight()) { c.drawBitmap(bitmap, null, dst, p); dst.offset(0, tileHeight); }
        }
        private int roadColor() { String t = roadSurface == null ? "" : roadSurface.toLowerCase(); if (t.contains("sand")) return Color.rgb(166, 128, 72); if (t.contains("ice")) return Color.rgb(134, 174, 190); if (t.contains("dirt")) return Color.rgb(92, 66, 45); return Color.rgb(54, 57, 62); }
        private int sideColor() { String t = roadSideStyle == null ? "" : roadSideStyle.toLowerCase(); if (t.contains("desert") || t.contains("beach")) return Color.rgb(190, 152, 88); if (t.contains("snow")) return Color.rgb(210, 225, 230); if (t.contains("forest") || t.contains("mountain")) return Color.rgb(27, 75, 42); if (t.contains("cyber")) return Color.rgb(34, 20, 55); return Color.rgb(31, 78, 56); }
        private void drawVehicle(Canvas c) { Bitmap b = bikeMode ? bikeBitmap : carBitmap; if (b == null) { b = bikeMode ? load(bikeImagePath) : load(carImagePath); if (bikeMode) bikeBitmap = b; else carBitmap = b; } RectF dst = rect(carX, carY, carWidth, carHeight); c.save(); c.rotate(angle, carX, carY); if (b != null) c.drawBitmap(b, null, dst, p); else { p.setColor(Color.argb(120,0,0,0)); c.drawOval(new RectF(dst.left+8,dst.bottom-18,dst.right-8,dst.bottom+10),p); p.setColor(bikeMode ? Color.CYAN : Color.rgb(22, 190, 96)); c.drawRoundRect(dst, 18, 18, p); p.setColor(Color.rgb(160, 230, 255)); c.drawRoundRect(new RectF(dst.left+18,dst.top+24,dst.right-18,dst.top+62),10,10,p); p.setColor(Color.BLACK); c.drawRect(dst.left-8,dst.top+28,dst.left+8,dst.top+58,p); c.drawRect(dst.right-8,dst.top+28,dst.right+8,dst.top+58,p); c.drawRect(dst.left-8,dst.bottom-58,dst.left+8,dst.bottom-28,p); c.drawRect(dst.right-8,dst.bottom-58,dst.right+8,dst.bottom-28,p); } c.restore(); }
        private void drawList(Canvas c, ArrayList<GameObject> list, Bitmap b, int color) { for (GameObject o : list) { RectF r = o.rect(); boolean isOpponent = o.type != null && o.type.toLowerCase().contains("opponent"); Bitmap drawBitmap = isOpponent ? resolveOpponentBitmap(o, b) : b; if (drawBitmap != null) { p.setStyle(Paint.Style.FILL); p.setAlpha(255); c.drawBitmap(drawBitmap, null, r, p); p.setAlpha(255); if (isOpponent) drawOpponentOutline(c, r); } else if (isOpponent) { drawOpponentFallback(c, r); } else { p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawRoundRect(r, 12, 12, p); p.setColor(Color.argb(80,255,255,255)); c.drawCircle(o.x, o.y - o.h/4, Math.max(6, o.w/5), p); } } }
        private Bitmap resolveOpponentBitmap(GameObject o, Bitmap defaultBitmap) { String key = cleanOpponentName(o.name); Bitmap drawBitmap = opponentImages.get(key); if (drawBitmap == null && o.imagePath != null && o.imagePath.length() > 0) { drawBitmap = load(o.imagePath); if (drawBitmap != null) opponentImages.put(key, drawBitmap); } if (drawBitmap == null && opponentImagePaths.containsKey(key)) { drawBitmap = load(opponentImagePaths.get(key)); if (drawBitmap != null) opponentImages.put(key, drawBitmap); } if (drawBitmap == null) drawBitmap = defaultBitmap; if (drawBitmap == null && opponentImagePath.length() > 0) { drawBitmap = load(opponentImagePath); if (drawBitmap != null) { opponentBitmap = drawBitmap; opponentImages.put(cleanOpponentName("Opponent"), drawBitmap); } } return drawBitmap; }
        private void drawOpponentOutline(Canvas c, RectF r) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4); p.setColor(Color.argb(210, 255, 80, 90)); c.drawRoundRect(r, 16, 16, p); p.setStyle(Paint.Style.FILL); }
        private void drawOpponentFallback(Canvas c, RectF r) { p.setColor(Color.argb(120,0,0,0)); c.drawOval(new RectF(r.left+8,r.bottom-18,r.right-8,r.bottom+10),p); p.setColor(Color.rgb(210, 56, 64)); c.drawRoundRect(r, 18, 18, p); p.setColor(Color.rgb(255, 210, 120)); c.drawRoundRect(new RectF(r.left+18, r.top+24, r.right-18, r.top+62), 10, 10, p); p.setColor(Color.rgb(75, 14, 20)); c.drawRoundRect(new RectF(r.left+18, r.bottom-66, r.right-18, r.bottom-24), 10, 10, p); p.setColor(Color.BLACK); c.drawRect(r.left-8, r.top+28, r.left+8, r.top+58, p); c.drawRect(r.right-8, r.top+28, r.right+8, r.top+58, p); c.drawRect(r.left-8, r.bottom-58, r.left+8, r.bottom-28, p); c.drawRect(r.right-8, r.bottom-58, r.right+8, r.bottom-28, p); }
        private void drawHud(Canvas c) { p.setTextSize(20); p.setColor(Color.argb(128,0,0,0)); c.drawRoundRect(new RectF(16, 16, 250, 150), 18, 18, p); p.setColor(Color.WHITE); c.drawText("Speed " + (int)Math.abs(speed * 10), 32, 50, p); c.drawText("Score " + score, 32, 84, p); c.drawText("Lap " + lap + "/" + maxLap, 32, 118, p); c.drawText("Health " + health, 32, 140, p); if (fuelEnabled) c.drawText("Fuel " + fuel, 32, 166, p); if (nitroEnabled) c.drawText("Nitro " + nitro, 32, 192, p); }
        private void drawControls(Canvas c) { float h = getHeight(), w = getWidth(); leftButton.set(22, h-132, 142, h-24);
            rightButton.set(w-142, h-132, w-22, h-24);
            startButton.set(w/2-76, h-98, w/2-8, h-30);
            stopButton.set(w/2+8, h-98, w/2+76, h-30);
            pauseButton.set(w-96, 22, w-34, 84);
            playButton.set(pauseButton);
            scoreButton.set(w-182, 22, w-112, 84);
            drawWaves(c);
            drawButton(c, leftButton, "‹", leftPressed, leftNavBitmap);
            drawButton(c, rightButton, "›", rightPressed, rightNavBitmap);
            if (!gameStarted) drawButton(c, startButton, "▶", false);
            if (gameStarted) drawButton(c, stopButton, "■", false);
            if (raceRunning) drawButton(c, pauseButton, "Ⅱ", false);
            if (gameStarted && !raceRunning) drawButton(c, playButton, "▶", false);
            drawButton(c, scoreButton, "★", false); }
        private void drawButton(Canvas c, RectF r, String label, boolean pressed) { drawButton(c, r, label, pressed, null); }
        private void drawButton(Canvas c, RectF r, String label, boolean pressed, Bitmap icon) { int top = pressed ? Color.rgb(18, 132, 180) : Color.rgb(31, 54, 82); int bottom = pressed ? Color.rgb(10, 83, 124) : Color.rgb(13, 25, 43); p.setShader(new LinearGradient(r.left, r.top, r.left, r.bottom, top, bottom, Shader.TileMode.CLAMP)); c.drawOval(r, p); p.setShader(null); p.setColor(Color.argb(95, 255, 255, 255)); p.setStrokeWidth(2); p.setStyle(Paint.Style.STROKE); c.drawOval(new RectF(r.left+2, r.top+2, r.right-2, r.bottom-2), p); p.setColor(Color.rgb(88, 220, 255)); p.setStrokeWidth(3); c.drawOval(r, p); if (icon != null) { p.setStyle(Paint.Style.FILL); c.drawBitmap(icon, null, new RectF(r.left+12, r.top+12, r.right-12, r.bottom-12), p); return; } p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(label.length() > 1 ? 34 : 42); Paint.FontMetrics fm = p.getFontMetrics(); c.drawText(label, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, p); p.setTextAlign(Paint.Align.LEFT); }
        private void addWave(float x, float y) { waves.add(new TouchWave(x, y, System.currentTimeMillis())); invalidate(); }
        private void drawWaves(Canvas c) { long now = System.currentTimeMillis(); for (int i = waves.size() - 1; i >= 0; i--) { TouchWave wave = waves.get(i); float progress = (now - wave.startedMs) / 520f; if (progress >= 1f) { waves.remove(i); continue; } float radius = 22 + progress * 86; int alpha = (int)(150 * (1f - progress)); p.setShader(new RadialGradient(wave.x, wave.y, radius, Color.argb(alpha, 89, 221, 255), Color.argb(0, 89, 221, 255), Shader.TileMode.CLAMP)); c.drawCircle(wave.x, wave.y, radius, p); p.setShader(null); } if (!waves.isEmpty()) invalidate(); }
        private void drawScoreSidebar(Canvas c) { float w = Math.min(390, Math.max(300, getWidth() * 0.42f)); RectF panel = new RectF(getWidth()-w, 0, getWidth(), getHeight()); p.setColor(Color.argb(115, 0, 0, 0)); c.drawRect(0, 0, panel.left, getHeight(), p); p.setShader(new LinearGradient(panel.left, 0, panel.right, getHeight(), Color.rgb(14, 25, 42), Color.rgb(5, 10, 19), Shader.TileMode.CLAMP)); c.drawRect(panel, p); p.setShader(null); p.setColor(Color.rgb(88, 220, 255)); c.drawRect(panel.left, 0, panel.left + 4, getHeight(), p); p.setColor(Color.argb(70, 255, 255, 255)); c.drawRoundRect(new RectF(panel.left + 18, 18, panel.right - 18, 86), 22, 22, p); p.setColor(Color.WHITE); p.setTextSize(30); p.setFakeBoldText(true); c.drawText("Race Dashboard", panel.left+34, 57, p); p.setFakeBoldText(false); p.setTextSize(15); p.setColor(Color.rgb(170, 205, 225)); c.drawText("Player: " + currentPlayerId, panel.left+34, 78, p); float y = 112; y = drawScoreCard(c, panel, y, "CURRENT SCORE", String.valueOf(score), Color.rgb(88, 220, 255)); y = drawScoreCard(c, panel, y, "SAVED BEST", String.valueOf(RetrieveScore(currentPlayerId)), Color.rgb(111, 255, 176)); y = drawScoreCard(c, panel, y, "HIGH SCORE", String.valueOf(RetrieveHighScore()), Color.rgb(255, 206, 86)); p.setTextSize(14); p.setColor(Color.rgb(150, 175, 195)); c.drawText("Health", panel.left+28, y+12, p); drawMeter(c, panel.left+28, y+22, panel.right-28, y+34, health, Color.rgb(111, 255, 176)); c.drawText("Lap " + lap + " of " + maxLap, panel.left+28, y+64, p); drawMeter(c, panel.left+28, y+74, panel.right-28, y+86, (int)(lap * 100f / Math.max(1, maxLap)), Color.rgb(88, 220, 255)); closeSidebarButton.set(panel.left+24, getHeight()-78, panel.right-24, getHeight()-24); drawButton(c, closeSidebarButton, "×", false); }
        private float drawScoreCard(Canvas c, RectF panel, float y, String label, String value, int accent) { RectF card = new RectF(panel.left+24, y, panel.right-24, y+70); p.setColor(Color.argb(155, 16, 32, 52)); c.drawRoundRect(card, 18, 18, p); p.setColor(accent); c.drawRoundRect(new RectF(card.left, card.top, card.left+5, card.bottom), 5, 5, p); p.setColor(Color.rgb(155, 182, 204)); p.setTextSize(13); c.drawText(label, card.left+18, card.top+24, p); p.setColor(Color.WHITE); p.setTextSize(28); p.setFakeBoldText(true); c.drawText(value, card.left+18, card.top+56, p); p.setFakeBoldText(false); return y + 84; }
        private void drawMeter(Canvas c, float left, float top, float right, float bottom, int percent, int color) { RectF bg = new RectF(left, top, right, bottom); p.setColor(Color.argb(120, 255, 255, 255)); c.drawRoundRect(bg, 8, 8, p); RectF fill = new RectF(left, top, left + (right - left) * clamp(percent, 0, 100) / 100f, bottom); p.setColor(color); c.drawRoundRect(fill, 8, 8, p); }
    }
    private static class TouchWave { float x,y; long startedMs; TouchWave(float x,float y,long startedMs){this.x=x;this.y=y;this.startedMs=startedMs;} }
    private static class GameObject { float x,y,w,h,vy,laneX; boolean needsLayoutPosition; String type,name,imagePath; GameObject(float x,float y,float w,float h,String type){this(x,y,w,h,type,"");} GameObject(float x,float y,float w,float h,String type,String name){this.x=x;this.y=y;this.w=w;this.h=h;this.type=type;this.name=name;} RectF rect(){return new RectF(x-w/2,y-h/2,x+w/2,y+h/2);} }
}
