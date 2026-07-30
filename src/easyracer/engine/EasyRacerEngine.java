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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class EasyRacerEngine extends AndroidViewComponent {
    private final ComponentContainer container;
    private final RacerView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HashMap<String, Bitmap> imageCache = new HashMap<String, Bitmap>();
    private final ArrayList<GameObject> coins = new ArrayList<GameObject>();
    private final ArrayList<GameObject> opponents = new ArrayList<GameObject>();
    private final ArrayList<GameObject> obstacles = new ArrayList<GameObject>();
    private final ArrayList<GameObject> checkpoints = new ArrayList<GameObject>();
    private final SharedPreferences saves;

    private Bitmap carBitmap, bikeBitmap, roadBitmap, coinBitmap, opponentBitmap;
    private String carName = "Player";
    private boolean bikeMode = false, raceRunning = false, gameStarted = false, autoScroll = true, infiniteMode = true;
    private boolean fuelEnabled = false, nitroEnabled = false, hudEnabled = true, miniMapEnabled = true;
    private boolean speedometerEnabled = true, trafficEnabled = false, landscapeDefault = true;
    private boolean leftPressed = false, rightPressed = false, acceleratePressed = false, brakePressed = false;
    private boolean accelerometerEnabled = false;
    private float accelerometerSensitivity = 1.2f, accelerometerDeadZone = 0.8f;
    private boolean scoreSidebarOpen = false;
    private float carX = 300, carY = 650, carWidth = 90, carHeight = 150, angle = 0;
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


    @SimpleFunction(description = "Sets the screen to landscape by default for a real racing-game feel. Call with false only if you want to return control to the app screen orientation.") public void SetLandscapeMode(boolean enabled) { landscapeDefault = enabled; setLandscapeMode(enabled); }
    @SimpleFunction(description = "Creates and balances a road preset. Inputs: type (City, Highway, Desert, Snow, Dirt, Track, Cyberpunk), lanes (visible lanes), surface (asphalt, dirt, sand, ice), marking (dashed, solid, double), sideStyle (city, forest, desert, snow), grip, friction, speed, width, and difficulty. The engine arranges these into road physics, visuals, lane lines, scenery, and scrolling speed automatically.") public void ConfigureRoadBlock(String type, int lanes, String surface, String marking, String sideStyle, float gripValue, float frictionValue, float speedValue, float widthValue, String difficulty) { roadType = type; roadLanes = Math.max(1, lanes); roadSurface = surface; roadMarking = marking; roadSideStyle = sideStyle; roadGrip = gripValue; friction = Math.max(0, frictionValue); roadSpeed = Math.max(0, speedValue); roadWidth = Math.max(280, widthValue); roadDifficulty = difficulty; applyRoadType(); view.invalidate(); }
    @SimpleFunction(description = "One-block road builder for common road types. Input: type. Supported values are City, Highway, Desert, Snow, Dirt, Track, Forest, Beach, Mountain, and Cyberpunk. The block chooses safe defaults for grip, friction, lanes, colors, roadside scenery, and speed.") public void CreateRoadTypeBlock(String type) { roadType = type; applyRoadPreset(type); view.invalidate(); }
    @SimpleFunction(description = "Enables accelerometer steering. Inputs: enabled, sensitivity, and deadZone. Connect an AccelerometerSensor.Changed event to NavigateWithAccelerometer(xAccel, yAccel, zAccel) after enabling this block.") public void EnableAccelerometerNavigation(boolean enabled, float sensitivity, float deadZone) { accelerometerEnabled = enabled; accelerometerSensitivity = Math.max(0.1f, sensitivity); accelerometerDeadZone = Math.max(0, deadZone); }
    @SimpleFunction(description = "Navigates the car from AccelerometerSensor readings. Input xAccel steers left/right, yAccel accelerates or brakes, and zAccel is accepted for compatibility. Use inside AccelerometerSensor.AccelerationChanged.") public void NavigateWithAccelerometer(float xAccel, float yAccel, float zAccel) { if (!accelerometerEnabled) return; if (Math.abs(xAccel) > accelerometerDeadZone) { float oldTurn = turnSpeed; turnSpeed *= accelerometerSensitivity; turn(xAccel > 0 ? -1 : 1); turnSpeed = oldTurn; } if (yAccel < -accelerometerDeadZone) Accelerate(); else if (yAccel > accelerometerDeadZone) Brake(); }
    @SimpleFunction(description = "Stores a score in the built-in local database using SharedPreferences. Inputs: playerId and score. The block also updates the high score automatically.") public void StoreScore(String playerId, int value) { String id = cleanPlayer(playerId); int best = Math.max(value, saves.getInt("score_" + id, 0)); saves.edit().putInt("score_" + id, best).putInt("lastScore", value).putInt("highScore", Math.max(value, saves.getInt("highScore", 0))).putString("highScorePlayer", value >= saves.getInt("highScore", 0) ? id : saves.getString("highScorePlayer", id)).apply(); }
    @SimpleFunction(description = "Retrieves a stored score from the built-in local database. Input: playerId. Returns that player's best saved score.") public int RetrieveScore(String playerId) { return saves.getInt("score_" + cleanPlayer(playerId), 0); }
    @SimpleFunction(description = "Returns the highest score stored in the built-in local database.") public int RetrieveHighScore() { return saves.getInt("highScore", 0); }
    @SimpleFunction(description = "Opens the professional score sidebar overlay, showing current score, saved score, and High Score label beside the highest score.") public void OpenScoreSidebar() { scoreSidebarOpen = true; view.invalidate(); }
    @SimpleFunction(description = "Closes the score sidebar overlay so players can focus on driving.") public void CloseScoreSidebar() { scoreSidebarOpen = false; view.invalidate(); }

    @SimpleFunction(description = "Accelerates the vehicle. Use with a button or clock for manual control.") public void Accelerate() { if (fuelEnabled && fuel <= 0) { FuelEmpty(); return; } speed = Math.min(maxSpeed * speedMultiplier, speed + acceleration / Math.max(0.2f, weight)); CarMoving(); }
    @SimpleFunction(description = "Brakes the vehicle and supports reverse at low speed.") public void Brake() { speed = Math.max(-maxSpeed * 0.35f, speed - brakeStrength); }
    @SimpleFunction(description = "Turns left using grip-aware automatic skid physics.") public void TurnLeft() { turn(-1); }
    @SimpleFunction(description = "Turns right using grip-aware automatic skid physics.") public void TurnRight() { turn(1); }
    @SimpleFunction(description = "Uses nitro boost if enabled and available.") public void UseNitro() { if (nitroEnabled && nitro > 0) { nitro = Math.max(0, nitro - 8); speed = Math.min(maxSpeed * 1.7f, speed + 5); NitroStarted(); if (nitro == 0) NitroEnded(); } }

    @SimpleFunction(description = "Sets player car image from asset path or file path. PNG, JPG, and WEBP are supported by Android.") public void SetCarImage(String path) { carBitmap = load(path); }
    @SimpleFunction(description = "Sets player bike image from asset path or file path.") public void SetBikeImage(String path) { bikeBitmap = load(path); }
    @SimpleFunction(description = "Sets scrolling road image from asset path or file path.") public void SetRoadImage(String path) { roadBitmap = load(path); }
    @SimpleFunction(description = "Sets road width in pixels.") public void SetRoadWidth(float width) { roadWidth = width; }
    @SimpleFunction(description = "Sets road height in pixels.") public void SetRoadHeight(float height) { roadHeight = height; }
    @SimpleFunction(description = "Sets default opponent image.") public void SetOpponentImage(String path) { opponentBitmap = load(path); }
    @SimpleFunction(description = "Sets default coin image.") public void SetCoinImage(String path) { coinBitmap = load(path); }
    @SimpleFunction(description = "Creates an AI opponent at x,y.") public void CreateOpponent(float x, float y) { GameObject o = new GameObject(x, y, 90, 150, "opponent"); opponents.add(o); }
    @SimpleFunction(description = "Spawns a coin at x,y.") public void SpawnCoin(float x, float y) { coins.add(new GameObject(x, y, 44, 44, "coin")); }
    @SimpleFunction(description = "Creates a checkpoint rectangle.") public void CreateCheckpoint(float x, float y, float width, float height) { checkpoints.add(new GameObject(x, y, width, height, "checkpoint")); }
    @SimpleFunction(description = "Adds an obstacle such as tree, rock, cone, oil, water, fire, or pothole.") public void CreateObstacle(String type, float x, float y, float width, float height) { obstacles.add(new GameObject(x, y, width, height, type)); }
    @SimpleFunction(description = "Spawns a built-in power up: Shield, Nitro, Double Coin, Repair, Slow Motion, Magnet, or Invincible.") public void SpawnPowerUp(String type, float x, float y) { obstacles.add(new GameObject(x, y, 58, 58, "PowerUp:" + type)); }
    @SimpleFunction(description = "Stores a sound asset path for Engine, Brake, Crash, Horn, Coin, Nitro, Victory, or Game Over.") public void SetSound(String name, String path) { String n = name == null ? "" : name.toLowerCase(); if (n.contains("engine")) engineSound = path; else if (n.contains("brake")) brakeSound = path; else if (n.contains("crash")) crashSound = path; else if (n.contains("horn")) hornSound = path; else if (n.contains("coin")) coinSound = path; else if (n.contains("nitro")) nitroSound = path; else if (n.contains("victory")) victorySound = path; else if (n.contains("over")) gameOverSound = path; }
    @SimpleFunction(description = "Configures simple car customization: tint, wheel size, and optional accessory images.") public void CustomizeCar(String tint, float wheelSizeValue, String wheelPath, String spoilerPath, String exhaustPath, String headlightPath) { carTint = tint; wheelSize = wheelSizeValue; wheelImage = wheelPath; spoilerImage = spoilerPath; exhaustImage = exhaustPath; headlightImage = headlightPath; }
    @SimpleFunction(description = "Sets body scale for simple visual customization.") public void SetBodyScale(float scale) { bodyScale = Math.max(0.2f, scale); carWidth *= bodyScale; carHeight *= bodyScale; }
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
        if (leftPressed) TurnLeft(); if (rightPressed) TurnRight(); if (acceleratePressed) Accelerate(); if (brakePressed) Brake();
        if (autoScroll) roadOffset += roadSpeed;
        speed *= Math.max(0, 1f - friction - (1f - grip * roadGrip) * 0.025f);
        if (Math.abs(speed) < 0.05f) { if (speed != 0) CarStopped(); speed = 0; }
        carY -= speed;
        if (infiniteMode) { carY = Math.max(80, Math.min(view.getHeight() - 80, carY)); }
        if (fuelEnabled && Math.abs(speed) > 0.2f) { fuel = clamp(fuel - 1, 0, 100); if (fuel == 0) FuelEmpty(); }
        score += Math.max(0, (int)Math.abs(speed));
        updateObjects(coins, roadSpeed); updateObjects(opponents, roadSpeed * 0.8f); updateObjects(obstacles, roadSpeed); updateObjects(checkpoints, roadSpeed);
        checkCollisions();
        if (countdownSeconds > 0 && RaceTime() >= countdownSeconds) { countdownSeconds = 0; TimeFinished(); FinishRace(); }
        view.invalidate();
    }
    private void updateObjects(ArrayList<GameObject> list, float dy) { for (GameObject o : list) if (autoScroll) o.y += dy; }
    private void turn(int dir) { float effectiveGrip = grip * roadGrip; angle += dir * turnSpeed * effectiveGrip; carX += dir * Math.max(1, Math.abs(speed)) * effectiveGrip; if (effectiveGrip < 0.35f) carX += dir * Math.abs(speed) * 0.6f; }
    private void checkCollisions() {
        RectF car = rect(carX, carY, carWidth, carHeight);
        if (car.left < (view.getWidth() - roadWidth) / 2f || car.right > (view.getWidth() + roadWidth) / 2f) { WhenCarHitsWall(); Damage(5); speed *= -0.25f; carX = Math.max((view.getWidth()-roadWidth)/2f + carWidth/2, Math.min((view.getWidth()+roadWidth)/2f - carWidth/2, carX)); }
        for (int i = coins.size() - 1; i >= 0; i--) if (RectF.intersects(car, coins.get(i).rect())) { coins.remove(i); coinsCollected++; score += 100; WhenCarHitsCoin(); CoinCollected(100, coinsCollected); }
        for (GameObject o : obstacles) if (RectF.intersects(car, o.rect())) { if (o.type.toLowerCase().contains("powerup")) { WhenCarHitsPowerup(o.type); applyPowerUp(o.type); } else if (o.type.toLowerCase().contains("oil") || o.type.toLowerCase().contains("water")) { WhenCarHitsPowerup(o.type); grip *= 0.7f; } else { WhenCarCrash(); CarCrash(); Damage(15); speed *= -0.4f; } }
        for (GameObject o : opponents) if (RectF.intersects(car, o.rect())) { WhenCarCrash(); CarCrash(); Damage(20); speed *= -0.5f; }
        for (int i = 0; i < checkpoints.size(); i++) if (RectF.intersects(car, checkpoints.get(i).rect())) { CheckpointReached(i + 1); if (i == checkpoints.size() - 1) { lap++; if (lap > maxLap) { PlayerWin(); FinishRace(); } } }
    }
    private RectF rect(float x, float y, float w, float h) { return new RectF(x - w/2, y - h/2, x + w/2, y + h/2); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private void applyRoadType() { String t = roadType == null ? "" : roadType.toLowerCase(); if (t.contains("ice") || t.contains("snow")) { roadGrip = Math.min(roadGrip, 0.35f); friction = 0.015f; } else if (t.contains("dirt") || t.contains("desert") || t.contains("sand")) { roadGrip = Math.min(roadGrip, 0.6f); friction = 0.07f; } else if (roadGrip <= 0) { roadGrip = 1f; friction = 0.04f; } }
    private void applyRoadPreset(String type) { String t = type == null ? "" : type.toLowerCase(); roadLanes = t.contains("track") ? 2 : t.contains("highway") ? 4 : 3; roadSurface = t.contains("desert") || t.contains("beach") ? "Sand" : t.contains("dirt") || t.contains("forest") || t.contains("mountain") ? "Dirt" : t.contains("snow") ? "Ice" : "Asphalt"; roadMarking = t.contains("track") ? "Solid" : "Dashed"; roadSideStyle = type; roadDifficulty = t.contains("snow") || t.contains("mountain") ? "Hard" : "Normal"; roadGrip = t.contains("snow") ? 0.25f : t.contains("dirt") || t.contains("desert") ? 0.55f : 1f; friction = t.contains("snow") ? 0.015f : t.contains("dirt") || t.contains("desert") ? 0.07f : 0.04f; roadSpeed = t.contains("highway") ? 9f : t.contains("track") ? 7f : 6f; }
    private void setLandscapeMode(boolean enabled) { try { Activity a = (Activity) container.$context(); a.setRequestedOrientation(enabled ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); } catch (Exception ignored) { } }
    private String cleanPlayer(String playerId) { return playerId == null || playerId.trim().length() == 0 ? "Player" : playerId.trim(); }
    private void applyPowerUp(String type) { String t = type == null ? "" : type.toLowerCase(); if (t.contains("nitro")) nitro = 100; if (t.contains("repair")) Repair(30); if (t.contains("double")) score += 200; if (t.contains("shield") || t.contains("invincible")) health = 100; if (t.contains("slow")) roadSpeed *= 0.7f; }
    private void applyWeather() { String w = weather == null ? "" : weather.toLowerCase(); if (w.contains("rain")) roadGrip *= 0.75f; if (w.contains("snow")) roadGrip *= 0.5f; if (w.contains("storm")) { roadGrip *= 0.65f; roadSpeed *= 0.9f; } }
    private Bitmap load(String path) { try { if (path == null || path.length() == 0) return null; if (imageCache.containsKey(path)) return imageCache.get(path); InputStream in = MediaUtil.openMedia(container.$form(), path); Bitmap b = BitmapFactory.decodeStream(in); imageCache.put(path, b); return b; } catch (Exception e) { return null; } }

    private class RacerView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF leftButton = new RectF(), rightButton = new RectF(), startButton = new RectF(), pauseButton = new RectF(), playButton = new RectF(), scoreButton = new RectF(), closeSidebarButton = new RectF();
        RacerView(Context c) { super(c); setBackgroundColor(Color.rgb(10, 14, 22)); setFocusable(true); }
        @Override protected void onDraw(Canvas c) { super.onDraw(c); drawSky(c); drawRoad(c); drawList(c, coins, coinBitmap, Color.YELLOW); drawList(c, obstacles, null, Color.RED); drawList(c, opponents, opponentBitmap, Color.BLUE); drawVehicle(c); if (hudEnabled) drawHud(c); drawControls(c); if (scoreSidebarOpen) drawScoreSidebar(c); }
        @Override public boolean onTouchEvent(MotionEvent e) { float x = e.getX(), y = e.getY(); boolean down = e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE; if (!down) { leftPressed = rightPressed = acceleratePressed = brakePressed = false; return true; } if (startButton.contains(x, y) && !gameStarted) StartRace(); else if (pauseButton.contains(x, y) && raceRunning) PauseRace(); else if (playButton.contains(x, y) && gameStarted && !raceRunning) PlayRace(); else if (scoreButton.contains(x, y)) { scoreSidebarOpen = !scoreSidebarOpen; invalidate(); } else if (closeSidebarButton.contains(x, y)) CloseScoreSidebar(); leftPressed = leftButton.contains(x, y); rightPressed = rightButton.contains(x, y); acceleratePressed = y < getHeight() * 0.45f && x > getWidth() * 0.72f; brakePressed = y > getHeight() * 0.55f && x > getWidth() * 0.72f; return true; }
        private void drawSky(Canvas c) { p.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.rgb(16, 27, 45), Color.rgb(5, 8, 14), Shader.TileMode.CLAMP)); c.drawRect(0, 0, getWidth(), getHeight(), p); p.setShader(null); }
        private void drawRoad(Canvas c) { float left = (getWidth()-roadWidth)/2f; p.setColor(sideColor()); c.drawRect(0, 0, left, getHeight(), p); c.drawRect(left+roadWidth, 0, getWidth(), getHeight(), p); p.setShader(new LinearGradient(left, 0, left+roadWidth, 0, Color.rgb(35,35,38), roadColor(), Color.rgb(35,35,38), Shader.TileMode.CLAMP)); c.drawRoundRect(new RectF(left, -18, left+roadWidth, getHeight()+18), 24, 24, p); p.setShader(null); p.setColor(Color.rgb(235,235,220)); p.setStrokeWidth(8); c.drawLine(left + 12, 0, left + 12, getHeight(), p); c.drawLine(left + roadWidth - 12, 0, left + roadWidth - 12, getHeight(), p); p.setStrokeWidth(5); int lanes = Math.max(1, roadLanes); for (int lane = 1; lane < lanes; lane++) { float lx = left + (roadWidth / lanes) * lane; for (int y = (int)(roadOffset % 96) - 96; y < getHeight(); y += 96) c.drawLine(lx, y, lx, y + ("Solid".equalsIgnoreCase(roadMarking) ? 88 : 46), p); } p.setColor(Color.argb(70, 255, 255, 255)); for (int y = (int)(roadOffset % 140) - 140; y < getHeight(); y += 140) c.drawOval(new RectF(left + 35, y, left + 95, y + 25), p); if (roadBitmap != null) { RectF dst = new RectF(left, -roadOffset % roadHeight, left+roadWidth, roadHeight - roadOffset % roadHeight); c.drawBitmap(roadBitmap, null, dst, p); if (infiniteMode) c.drawBitmap(roadBitmap, null, new RectF(left, dst.bottom, left+roadWidth, dst.bottom+roadHeight), p); } }
        private int roadColor() { String t = roadSurface == null ? "" : roadSurface.toLowerCase(); if (t.contains("sand")) return Color.rgb(166, 128, 72); if (t.contains("ice")) return Color.rgb(134, 174, 190); if (t.contains("dirt")) return Color.rgb(92, 66, 45); return Color.rgb(54, 57, 62); }
        private int sideColor() { String t = roadSideStyle == null ? "" : roadSideStyle.toLowerCase(); if (t.contains("desert") || t.contains("beach")) return Color.rgb(190, 152, 88); if (t.contains("snow")) return Color.rgb(210, 225, 230); if (t.contains("forest") || t.contains("mountain")) return Color.rgb(27, 75, 42); if (t.contains("cyber")) return Color.rgb(34, 20, 55); return Color.rgb(31, 78, 56); }
        private void drawVehicle(Canvas c) { Bitmap b = bikeMode ? bikeBitmap : carBitmap; RectF dst = rect(carX, carY, carWidth, carHeight); c.save(); c.rotate(angle, carX, carY); if (b != null) c.drawBitmap(b, null, dst, p); else { p.setColor(Color.argb(120,0,0,0)); c.drawOval(new RectF(dst.left+8,dst.bottom-18,dst.right-8,dst.bottom+10),p); p.setColor(bikeMode ? Color.CYAN : Color.rgb(22, 190, 96)); c.drawRoundRect(dst, 18, 18, p); p.setColor(Color.rgb(160, 230, 255)); c.drawRoundRect(new RectF(dst.left+18,dst.top+24,dst.right-18,dst.top+62),10,10,p); p.setColor(Color.BLACK); c.drawRect(dst.left-8,dst.top+28,dst.left+8,dst.top+58,p); c.drawRect(dst.right-8,dst.top+28,dst.right+8,dst.top+58,p); c.drawRect(dst.left-8,dst.bottom-58,dst.left+8,dst.bottom-28,p); c.drawRect(dst.right-8,dst.bottom-58,dst.right+8,dst.bottom-28,p); } c.restore(); }
        private void drawList(Canvas c, ArrayList<GameObject> list, Bitmap b, int color) { for (GameObject o : list) { if (b != null && "coin".equals(o.type)) c.drawBitmap(b, null, o.rect(), p); else { p.setColor(color); c.drawRoundRect(o.rect(), 12, 12, p); p.setColor(Color.argb(80,255,255,255)); c.drawCircle(o.x, o.y - o.h/4, Math.max(6, o.w/5), p); } } }
        private void drawHud(Canvas c) { p.setTextSize(28); p.setColor(Color.argb(190,0,0,0)); c.drawRoundRect(new RectF(16, 16, 280, 172), 18, 18, p); p.setColor(Color.WHITE); c.drawText("Speed " + (int)Math.abs(speed * 10), 32, 50, p); c.drawText("Score " + score, 32, 84, p); c.drawText("Lap " + lap + "/" + maxLap, 32, 118, p); c.drawText("Health " + health, 32, 152, p); if (fuelEnabled) c.drawText("Fuel " + fuel, 32, 186, p); if (nitroEnabled) c.drawText("Nitro " + nitro, 32, 220, p); }
        private void drawControls(Canvas c) { float h = getHeight(), w = getWidth(); leftButton.set(22, h-132, 142, h-24); rightButton.set(w-142, h-132, w-22, h-24); startButton.set(w/2-105, h-78, w/2+105, h-22); pauseButton.set(w-150, 22, w-30, 76); playButton.set(w-150, 88, w-30, 142); scoreButton.set(w-300, 22, w-170, 76); drawButton(c, leftButton, "◀"); drawButton(c, rightButton, "▶"); if (!gameStarted) drawButton(c, startButton, "START"); if (raceRunning) drawButton(c, pauseButton, "PAUSE"); if (gameStarted && !raceRunning) drawButton(c, playButton, "PLAY"); drawButton(c, scoreButton, "SCORES"); }
        private void drawButton(Canvas c, RectF r, String label) { p.setColor(Color.argb(205, 18, 31, 48)); c.drawRoundRect(r, 18, 18, p); p.setColor(Color.rgb(79, 214, 255)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); c.drawRoundRect(r, 18, 18, p); p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(label.length() > 2 ? 22 : 42); c.drawText(label, r.centerX(), r.centerY()+9, p); p.setTextAlign(Paint.Align.LEFT); }
        private void drawScoreSidebar(Canvas c) { float w = Math.min(360, getWidth() * 0.42f); RectF panel = new RectF(getWidth()-w, 0, getWidth(), getHeight()); p.setColor(Color.argb(235, 9, 17, 30)); c.drawRect(panel, p); p.setColor(Color.WHITE); p.setTextSize(30); c.drawText("Scores", panel.left+26, 54, p); p.setTextSize(23); c.drawText("Current: " + score, panel.left+26, 105, p); c.drawText("Saved: " + RetrieveScore(currentPlayerId), panel.left+26, 143, p); c.drawText("High Score: " + RetrieveHighScore(), panel.left+26, 181, p); c.drawText("High score", panel.right-138, 181, p); closeSidebarButton.set(panel.left+22, getHeight()-74, panel.right-22, getHeight()-24); drawButton(c, closeSidebarButton, "CLOSE"); }
    }
    private static class GameObject { float x,y,w,h; String type; GameObject(float x,float y,float w,float h,String type){this.x=x;this.y=y;this.w=w;this.h=h;this.type=type;} RectF rect(){return new RectF(x-w/2,y-h/2,x+w/2,y+h/2);} }
}
