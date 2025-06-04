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

    const data = {
      avg_speed: parseFloat(feed.field1),
      mileage: parseFloat(feed.field2),
      total_distance: parseFloat(feed.field3),
      co2_saved: parseFloat(feed.field4),
      battery_level: parseFloat(feed.field5),
      dte_progress: parseFloat(feed.field6),
    };

    await db.ref(FIREBASE_PATH).update(data);
    console.log("✅ Data pushed to Firebase:", data);
  } catch (error) {
    console.error("❌ Error fetching or pushing data:", error.message);
  }
};

function startSync() {
  setInterval(fetchAndPush, 15000); // run every 15 seconds
}

module.exports = { startSync };