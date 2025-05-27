const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const app = express();
const port = 3000;
 
app.use(cors());
app.use(bodyParser.json());
 
// Temporary store for OTPs (use a database in production)
const otpStore = {};
 
const generateOTP = () => {
  return Math.floor(100000 + Math.random() * 900000).toString(); // 6-digit OTP
};
 
// ✅ Send OTP
app.post('/send-otp', (req, res) => {
  const { phone } = req.body;
 
  if (!phone) {
    return res.status(400).json({ error: 'Phone number is required' });
  }
 
  const otp = generateOTP();
  otpStore[phone] = otp;
 
  // In production, integrate with an SMS provider (e.g., Twilio)
  console.log(`OTP for ${phone}: ${otp}`);
 
  return res.json({ success: true, message: 'OTP sent successfully' });
});
 
// ✅ Verify OTP
app.post('/verify-otp', (req, res) => {
  const { phone, otp } = req.body;
 
  if (!phone || !otp) {
    return res.status(400).json({ error: 'Phone number and OTP are required' });
  }
 
  if (otpStore[phone] === otp) {
    delete otpStore[phone]; // OTP used, remove it
    return res.json({ success: true, message: 'OTP verified successfully' });
  } else {
    return res.status(401).json({ success: false, message: 'Invalid OTP' });
  }
});
 
// Test endpoint (optional)
app.get('/', (req, res) => {
  res.send('OTP Service is running');
});
 
app.listen(port, () => {
  console.log(`Server running on http://localhost:${port}`);
});