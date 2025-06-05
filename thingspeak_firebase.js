const axios = require("axios");
const admin = require("firebase-admin");
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
 
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: process.env.FIREBASE_DB_URL
});
 
const db = admin.database();
 
const THINGSPEAK_API_KEY = "RFILA6YNLV13M5H7";
const CHANNEL_ID = "2979545";
const FIREBASE_PATH = "HomeFragment/Anna Maria James";
 
const safeParse = (value) => {
  const num = parseFloat(value);
  return isNaN(num) ? 0 : num;
};
 
// ⏰ Cache last ThingSpeak timestamp
let lastThingSpeakTimestamp = null;
 
const fetchAndPush = async () => {
  try {
    const response = await axios.get(
      `https://api.thingspeak.com/channels/${CHANNEL_ID}/feeds.json?api_key=${THINGSPEAK_API_KEY}&results=1`
    );
 
    const feed = response.data.feeds[0];
    if (!feed) {
      console.warn("⚠️ No data received from ThingSpeak");
      return;
    }
 
    const currentTimestamp = feed.created_at;
 
    // ✅ Skip if this entry has already been pushed
    if (currentTimestamp === lastThingSpeakTimestamp) {
      console.log("⏸ No new data in ThingSpeak — skipping push to Firebase");
      return;
    }
 
    // ⏱ Update cache
    lastThingSpeakTimestamp = currentTimestamp;
 
    const data = {
      avg_speed: safeParse(feed.field1),
      mileage: safeParse(feed.field2),
      total_distance: safeParse(feed.field3),
      co2_saved: safeParse(feed.field4),
      battery_level: safeParse(feed.field5),
      dte_progress: safeParse(feed.field6),
      last_updated: new Date().toISOString()
    };
 
    await db.ref(FIREBASE_PATH).update(data);
    console.log("✅ New ThingSpeak data pushed to Firebase:", data);
 
  } catch (error) {
    console.error("❌ Error fetching or pushing data:", error.message);
  }
};
 
function startSync() {
  setInterval(fetchAndPush, 1000); // or 15000 (15s) for production
}
 
module.exports = { startSync };