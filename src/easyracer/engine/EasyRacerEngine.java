package easyracer.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
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
    private boolean bikeMode = false, raceRunning = false, autoScroll = true, infiniteMode = true;
    private boolean fuelEnabled = false, nitroEnabled = false, hudEnabled = true, miniMapEnabled = true;
    private boolean speedometerEnabled = true, trafficEnabled = false;
    private float carX = 300, carY = 650, carWidth = 90, carHeight = 150, angle = 0;
    private float speed = 0, acceleration = 0.35f, brakeStrength = 0.65f, maxSpeed = 18f, turnSpeed = 4f;
    private float weight = 1f, grip = 0.85f, friction = 0.04f, roadGrip = 1f, speedMultiplier = 1f;
    private float roadWidth = 720, roadHeight = 1280, roadSpeed = 6f, roadOffset = 0;
    private String roadType = "City", scrollDirection = "Vertical", weather = "Sunny", theme = "City";
    private int score = 0, coinsCollected = 0, health = 100, fuel = 100, nitro = 100, lap = 1, maxLap = 3;
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
    @SimpleFunction(description = "Starts the race loop with automatic physics, scoring, AI, collisions, and HUD updates.") public void StartRace() { if (!raceRunning) { raceRunning = true; raceStartMs = System.currentTimeMillis(); RaceStarted(); handler.post(loop); } }
    @SimpleFunction(description = "Pauses the race loop.") public void PauseRace() { raceRunning = false; }
    @SimpleFunction(description = "Finishes the race and dispatches RaceFinished.") public void FinishRace() { raceRunning = false; RaceFinished(score); }

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
    @SimpleProperty public String CarName() { return carName; } @SimpleProperty public void CarName(String v) { carName = v; }
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
    private void applyRoadType() { String t = roadType == null ? "" : roadType.toLowerCase(); roadGrip = t.contains("ice") || t.contains("snow") ? 0.2f : t.contains("dirt") || t.contains("desert") ? 0.55f : 1f; friction = t.contains("ice") ? 0.015f : t.contains("dirt") ? 0.07f : 0.04f; }
    private void applyPowerUp(String type) { String t = type == null ? "" : type.toLowerCase(); if (t.contains("nitro")) nitro = 100; if (t.contains("repair")) Repair(30); if (t.contains("double")) score += 200; if (t.contains("shield") || t.contains("invincible")) health = 100; if (t.contains("slow")) roadSpeed *= 0.7f; }
    private void applyWeather() { String w = weather == null ? "" : weather.toLowerCase(); if (w.contains("rain")) roadGrip *= 0.75f; if (w.contains("snow")) roadGrip *= 0.5f; if (w.contains("storm")) { roadGrip *= 0.65f; roadSpeed *= 0.9f; } }
    private Bitmap load(String path) { try { if (path == null || path.length() == 0) return null; if (imageCache.containsKey(path)) return imageCache.get(path); InputStream in = MediaUtil.openMedia(container.$form(), path); Bitmap b = BitmapFactory.decodeStream(in); imageCache.put(path, b); return b; } catch (Exception e) { return null; } }

    private class RacerView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RacerView(Context c) { super(c); setBackgroundColor(Color.rgb(24, 24, 24)); }
        @Override protected void onDraw(Canvas c) { super.onDraw(c); drawRoad(c); drawList(c, coins, coinBitmap, Color.YELLOW); drawList(c, obstacles, null, Color.RED); drawList(c, opponents, opponentBitmap, Color.BLUE); drawVehicle(c); if (hudEnabled) drawHud(c); }
        private void drawRoad(Canvas c) { p.setColor(Color.rgb(60, 60, 60)); float left = (getWidth()-roadWidth)/2f; c.drawRect(left, 0, left+roadWidth, getHeight(), p); p.setColor(Color.WHITE); p.setStrokeWidth(6); for (int y = (int)(roadOffset % 80) - 80; y < getHeight(); y += 80) c.drawLine(getWidth()/2f, y, getWidth()/2f, y+38, p); if (roadBitmap != null) { RectF dst = new RectF(left, -roadOffset % roadHeight, left+roadWidth, roadHeight - roadOffset % roadHeight); c.drawBitmap(roadBitmap, null, dst, p); if (infiniteMode) c.drawBitmap(roadBitmap, null, new RectF(left, dst.bottom, left+roadWidth, dst.bottom+roadHeight), p); } }
        private void drawVehicle(Canvas c) { Bitmap b = bikeMode ? bikeBitmap : carBitmap; RectF dst = rect(carX, carY, carWidth, carHeight); if (b != null) c.drawBitmap(b, null, dst, p); else { p.setColor(bikeMode ? Color.CYAN : Color.GREEN); c.drawRoundRect(dst, 14, 14, p); } }
        private void drawList(Canvas c, ArrayList<GameObject> list, Bitmap b, int color) { p.setColor(color); for (GameObject o : list) { if (b != null && "coin".equals(o.type)) c.drawBitmap(b, null, o.rect(), p); else c.drawRoundRect(o.rect(), 10, 10, p); } }
        private void drawHud(Canvas c) { p.setTextSize(34); p.setColor(Color.WHITE); c.drawText("Speed " + (int)Math.abs(speed * 10), 20, 45, p); c.drawText("Score " + score, 20, 85, p); c.drawText("Lap " + lap + "/" + maxLap, 20, 125, p); c.drawText("Health " + health, 20, 165, p); if (fuelEnabled) c.drawText("Fuel " + fuel, 20, 205, p); if (nitroEnabled) c.drawText("Nitro " + nitro, 20, 245, p); if (miniMapEnabled) { p.setStyle(Paint.Style.STROKE); c.drawRect(getWidth()-130, 25, getWidth()-25, 160, p); p.setStyle(Paint.Style.FILL); } }
    }
    private static class GameObject { float x,y,w,h; String type; GameObject(float x,float y,float w,float h,String type){this.x=x;this.y=y;this.w=w;this.h=h;this.type=type;} RectF rect(){return new RectF(x-w/2,y-h/2,x+w/2,y+h/2);} }
}
