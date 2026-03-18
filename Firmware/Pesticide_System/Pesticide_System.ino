#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <apis.h>

// Hardware config
#define DHTPIN 18
#define SOIL_PIN 33
#define SPRAY_PIN 5
#define DHTTYPE DHT11
#define RESET_BUTTON_PIN 34

#define SPRAY_DURATION 5000                                 // Pump ON duration(5 sec)
#define SCROLL_INTERVAL 800                                 // Time between shifts
#define COMMAND_INTERVAL 5000                               // Firebase command poll rate(5 sec)
#define MONITOR_INTERVAL (2UL * 60UL * 60UL * 1000UL)       // 2-hour sensing & AI cycle
#define COOLDOWN_PERIOD (7UL * 24UL * 60UL * 60UL * 1000UL) // Spray safety cooldown(7 days)
#define AI_FRESHNESS_WINDOW (30UL * 60UL * 1000UL)          // AI result validity window(30 minutes)
#define AUTO_CAPTURE_OVERRIDE_WINDOW (20UL * 60UL * 1000UL) // Maintainance window(20 minutes)

// Data Structures
struct TelemetryPacket {
  float temperature;
  float humidity;
  int soilMoisture;
  unsigned long ts;
};

// Objects
DHT dht(DHTPIN, DHTTYPE);
LiquidCrystal_I2C display(0x27,16,2);

FirebaseData fbda;
FirebaseAuth auth;
FirebaseConfig config;

// Device Identity
String deviceId;
String deviceAuthUid;

void generateDeviceId() {
  uint64_t chipid = ESP.getEfuseMac();
  char id[20];
  sprintf(id, "ESP32_%04X%08X",
          (uint32_t)(chipid >> 32),
          (uint32_t)chipid);
  deviceId = String(id);
}

// System state
bool pairedCached = false;
bool lastPairedState = false;
bool monitoringEnabled = true;
bool autoCaptureEnabled = false;

String selectedCrop = "Unknown";
String lastLine1 = "", lastLine2 = "";
int scrollPos1 = 0, scrollPos2 = 0;
int lastInfectionProb = -1;
String lastSeverity = "";
String lastRisk = "";
unsigned long lastAIUpdate = 0;
unsigned long lastSprayTime = 0;
unsigned long autoCaptureDisabledAt = 0;

// Timers
unsigned long lastScroll1 = 0, lastScroll2 = 0;
unsigned long lastHeartBeat = 0;
unsigned long lastMonitorTime = 0;
unsigned long lastCommandCheck = 0;
unsigned long sprayStartTime = 0;
bool spraying = false;

// Offline Ring Buffer(RAM)
#define BUFFER_SIZE 5

TelemetryPacket buffer[BUFFER_SIZE];
int head = 0, tail = 0;
bool bufferFull = false;

void bufferPush(const TelemetryPacket &p) {
  buffer[head] = p;
  head = (head + 1) % BUFFER_SIZE;
  if(bufferFull) tail = (tail + 1) % BUFFER_SIZE;
  bufferFull = (head == tail);
}

bool bufferPop(TelemetryPacket &p) {
  if(head == tail && !bufferFull) return false;
  p = buffer[tail];
  tail = (tail + 1) % BUFFER_SIZE;
  bufferFull = false;
  return true;
}

// Function declarations
void connectWiFi();
void firebaseLogin();
void showOnDisplay(String l1, String l2 = "");
bool isPaired();
void onPaired();
void onUnpaired();
void createPairingEntry();
void checkRemoteCommands();
void fetchCrop();
void uploadSensorData(float t, float h, int soil);
void uploadInfectionData(int prob, String severity, String risk);
bool canSpray(int prob);
void startSpray();
void updateSpray();
void ackCommand(String cmd);
void updateStatus(String status);

// Setup
void setup() {
  Serial.begin(115200);
  Serial.setTimeout(2000);
  
  Wire.begin(21,22);
  dht.begin();
  display.init();
  display.backlight();
  
  pinMode(SPRAY_PIN, OUTPUT);
  digitalWrite(SPRAY_PIN, LOW);
  pinMode(RESET_BUTTON_PIN, INPUT_PULLUP); // Active Low

  analogReadResolution(12);

  showOnDisplay("System Booting");
  generateDeviceId();
  connectWiFi();

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  firebaseLogin();
  registerDeviceIfNeeded();

  pairedCached = isPaired();
  if(!pairedCached) {
    createPairingEntry();
    showOnDisplay("Pair Device", deviceId);
  }
  Serial.println("Device Ready");
  Serial.println("Device ID: " + deviceId);
}
// Loop
void loop() {
  if(digitalRead(RESET_BUTTON_PIN) == LOW) {
    ESP.restart();
  }

  if (Firebase.ready()) {
    bool currentPaired = isPaired();

    if (!lastPairedState && currentPaired) {
      onPaired();
    }

    if(lastPairedState && !currentPaired) {
      Serial.println("Unpaired remotely");
      showOnDisplay("Unpaired", "Remotely");
      delay(500);
      onUnpaired();
    }
    pairedCached = currentPaired;
    lastPairedState = currentPaired;
    
  }
  
  if(!pairedCached) {
    showOnDisplay("Pair Code: ",deviceId);
    delay(500);
    return;
  }

  if (millis() - lastHeartBeat > 15000) { // 15 sec
    FirebaseJson json;
    json.set(".sv", "timestamp");

    Firebase.RTDB.set(&fbda,
      "/devices/" + deviceId + "/lastOnline",
      &json
    );
    lastHeartBeat = millis();
  }

  if(millis() - lastCommandCheck >= COMMAND_INTERVAL) {
    checkRemoteCommands();
    lastCommandCheck = millis();
  }

  updateSpray();

  if(!monitoringEnabled) {
    showOnDisplay("Monitoring", "Paused");
  }
  else {
    // Monitoring cycle (2 hours)
    if(millis() - lastMonitorTime >= MONITOR_INTERVAL) {

      lastMonitorTime = millis();

      fetchCrop();
      showOnDisplay("Monitoring", selectedCrop);

      float temperature = dht.readTemperature();
      float humidity = dht.readHumidity();
      int soilMoisture = map(analogRead(SOIL_PIN),0, 4095, 100, 0);

      if(isnan(temperature) || isnan(humidity)) {
        updateStatus("Sensor Error");
        return;
      }

      TelemetryPacket p{temperature, humidity, soilMoisture, millis()};
      bufferPush(p);

      while(WiFi.status() == WL_CONNECTED) {
        TelemetryPacket pkt;
        if(!bufferPop(pkt)) break;

        if(!Firebase.ready()) {
          bufferPush(pkt);
          break;
        }

        uploadSensorData(pkt.temperature, pkt.humidity, pkt.soilMoisture);
      }

      if(!autoCaptureEnabled) {
        updateStatus("AutoCapture OFF");
        return;
      }

      // Sending data to AI engine(model)
      StaticJsonDocument<256> outgoing;
      outgoing["crop"] = selectedCrop;
      outgoing["temperature"] = temperature;
      outgoing["humidity"] = humidity;
      outgoing["soil"] = soilMoisture;
      // outgoing["autoCapture"] = autoCaptureEnabled;

      serializeJson(outgoing, Serial);
      Serial.println();

      unsigned long waitStart = millis();
      while(!Serial.available() && millis() - waitStart < 5000) {
        // allow OTA / Wifi / Firebase
      }

      if(!Serial.available()) {
        updateStatus("AI Offline");
        return;
      }

      StaticJsonDocument<256> incoming;

      if(deserializeJson(incoming, Serial.readStringUntil('\n'))) {
        updateStatus("AI Error");
        return;
      }

      lastInfectionProb = incoming["infection_probability"];
      lastSeverity = incoming["severity"].as<String>();
      lastRisk = incoming["risk_level"].as<String>();
      lastAIUpdate = millis();

      uploadInfectionData(lastInfectionProb, lastSeverity, lastRisk);

      bool aiFresh = lastAIUpdate > 0 && (millis() - lastAIUpdate <= AI_FRESHNESS_WINDOW);

      if(lastInfectionProb != -1 &&
        aiFresh &&
        canSpray(lastInfectionProb)) {
        startSpray();
      }
    }
  }
}

// Connecting wifi
void connectWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while(WiFi.status() != WL_CONNECTED) {
    showOnDisplay("Connecting...");
    delay(500);
  }
  showOnDisplay("Wifi Connected");
}

// Firebase Authentication
void firebaseLogin() {
  if(!Firebase.signUp(&config, &auth, "", "")) {
    ESP.restart();
  }
  while(auth.token.uid == "") {
    Firebase.refreshToken(&config);
    delay(200);
  }
  deviceAuthUid = auth.token.uid.c_str();
}

void registerDeviceIfNeeded() {
  
  String base = "/devices/" + deviceId;

  if(Firebase.RTDB.getString(&fbda, base+"/deviceAuthUid")) {
    return;
  }

  FirebaseJson json;
  json.set("deviceAuthUid", deviceAuthUid);
  json.set("paired", false);
  json.set("createdAt", millis());
  
  Firebase.RTDB.updateNode(&fbda, base, &json);
}


// Display
void showOnDisplay(String l1, String l2) {
  if (l1 != lastLine1 || l2 != lastLine2) {
    lastLine1 = l1;
    lastLine2 = l2;
    display.clear();

    scrollPos1 = 0;
    scrollPos2 = 0;
    lastScroll1 = millis();
    lastScroll2 = millis();
  }
  scrollText(0,l1,scrollPos1, lastScroll1);
  scrollText(1,l2,scrollPos2, lastScroll2);
}
void scrollText(int row, String text, int &scrollPos, unsigned long &lastScroll) {
  if (text.length() <= 16) {
    if (scrollPos != -1) { 
      display.setCursor(0, row);
      display.print(text);
      for (int i = 0; i < (16 - text.length()); i++) display.print(" ");
      scrollPos = -1; 
    }
    return;
  }

  unsigned long now = millis();
  if (now - lastScroll >= SCROLL_INTERVAL) {
    lastScroll = now;

    display.setCursor(0, row);
    display.print(text.substring(scrollPos, scrollPos + 16));

    scrollPos++;
    if (scrollPos > text.length() - 16) {
      scrollPos = 0; 
    }
  }
}

// Pairing
bool isPaired() {
  if(!Firebase.ready()) return pairedCached;
  if(Firebase.RTDB.getBool(&fbda, "/devices/" + deviceId + "/paired")) {
    return fbda.boolData();
  }
  return pairedCached;
}

void onPaired() {
  Serial.println("Paired → entering monitoring mode");
  lastMonitorTime = millis() - MONITOR_INTERVAL;
  fetchCrop();
  showOnDisplay("Monitoring", selectedCrop);
}

void onUnpaired() {
  pairedCached = false;
  monitoringEnabled = false;
  autoCaptureEnabled = false;
  selectedCrop = "Unknown";

  showOnDisplay("Pair Code:", deviceId);
  createPairingEntry();
}

void createPairingEntry() {
  if(!Firebase.ready()) return;
  // if(pairedCached) return;
  Firebase.RTDB.setBool(&fbda, "/pairingCodes/" + deviceId, true);
}

// Command Listener
void checkRemoteCommands() {
  if(WiFi.status() != WL_CONNECTED || !Firebase.ready()) {
    return;
  }
  
  String base_path = "/devices/" + deviceId + "/commands";

  // Syncing dev flag
  if(Firebase.RTDB.getBool(&fbda, base_path + "/autoCaptureEnabled")) {
    autoCaptureEnabled = fbda.boolData();
  }

  // Sync autoCaptureDisabledAt
  if (Firebase.RTDB.getInt(&fbda, base_path + "/autoCaptureDisabledAt")) {
    autoCaptureDisabledAt = fbda.to<uint64_t>();
  }

  // Auto-restore AutoCapture after 20 minutes
  if (!autoCaptureEnabled && autoCaptureDisabledAt > 0) {
    if (millis() - autoCaptureDisabledAt >= AUTO_CAPTURE_OVERRIDE_WINDOW) {
      autoCaptureEnabled = true;

      Firebase.RTDB.setBool(&fbda,
        base_path + "/autoCaptureEnabled",
        true);

      Firebase.RTDB.deleteNode(&fbda,
        base_path + "/autoCaptureDisabledAt");
    }
  }

  // Spray command
  if(Firebase.RTDB.getBool(&fbda, base_path + "/sprayNow") && fbda.boolData()) {
    if(lastInfectionProb != -1 && 
      millis() - lastAIUpdate <= AI_FRESHNESS_WINDOW &&
      canSpray(lastInfectionProb)) {
      startSpray();
    }

    Firebase.RTDB.setBool(&fbda, base_path + "/sprayNow", false);
    ackCommand("sprayNow");
  }

  // Reset command
  if(Firebase.RTDB.getBool(&fbda, base_path + "/resetDevice") && fbda.boolData()) {
    Firebase.RTDB.setBool(&fbda, base_path + "/resetDevice", false);
    ackCommand("resetDevice");
    delay(1500);
    ESP.restart();
  }

  // Start/Stop Monitoring
  if(Firebase.RTDB.getBool(&fbda, base_path + "/startMonitoring")) {
    bool newState = fbda.boolData();

    if(!monitoringEnabled && newState) {
      Serial.println("Monitoring resumed remotely");
      lastMonitorTime = millis() - MONITOR_INTERVAL;
    }
    monitoringEnabled = newState;
  }
}

// Getting crop selected by user
void fetchCrop() {
  if (!Firebase.ready()) return;

  if (Firebase.RTDB.getString(&fbda, "/devices/" + deviceId + "/crop")) {
    selectedCrop = fbda.stringData();
    Serial.println("Crop: " + selectedCrop);
  } else {
    Serial.println("Failed to read crop");
  }
}

// Uploading sensor data to firebase
void uploadSensorData(float t, float h, int soil) {
  FirebaseJson json;
  json.set("temperature", t);
  json.set("humidity", h);
  json.set("soilMoisture", soil);

  Firebase.RTDB.updateNode(&fbda,
    "/devices/" + deviceId + "/sensors",
    &json);
  
  FirebaseJson ts;
  ts.set(".sv", "timestamp");
  Firebase.RTDB.set(&fbda,
    "/devices/" + deviceId + "/lastUpdated",
    &ts);
}

// Uploading infection data to firebase
void uploadInfectionData(int prob, String severity, String risk) {
  FirebaseJson json;
  json.set("probability", prob);
  json.set("severity", severity);
  json.set("riskLevel", risk);

  Firebase.RTDB.updateNode(&fbda,
    "/devices/" + deviceId + "/infection",
    &json);
}

// Spray checking
bool canSpray(int prob) {
  if(prob < 60) return false;
  if(lastSprayTime == 0) return true;
  return millis() - lastSprayTime >= COOLDOWN_PERIOD;
}

void startSpray() {
  spraying = true;
  sprayStartTime = millis();
  digitalWrite(SPRAY_PIN, HIGH);
}

void updateSpray() {
  if(spraying && millis() - sprayStartTime >= SPRAY_DURATION) {
    digitalWrite(SPRAY_PIN, LOW);
    spraying = false;
    lastSprayTime = millis();

    Firebase.RTDB.setInt(&fbda,
      "/devices/" + deviceId + "/pesticide/lastSprayedAt",
      millis());
    
    Firebase.RTDB.setString(&fbda,
      "/devices/" + deviceId + "/pesticide/status",
      "Sprayed");
    
    int count = 0;
    if(Firebase.RTDB.getInt(&fbda,
    "/devices/" + deviceId + "/pesticide/sprayCount")) {
      count = fbda.intData();
    }
  
    Firebase.RTDB.setInt(&fbda,
      "/devices/" + deviceId + "/pesticide/sprayCount",
      count + 1);
  }
}

//Command acknowledgement
void ackCommand(String cmd) {
  Firebase.RTDB.setInt(&fbda,
    "/devices/" + deviceId + "/commandsAck/" + cmd,
    millis());
}

// Updating status
void updateStatus(String status) {
  Firebase.RTDB.setString(&fbda,
    "/devices/" + deviceId + "/pesticide/status",
    status);
}