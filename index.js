const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const twilio = require('twilio');
const SibApiV3Sdk = require('sib-api-v3-sdk'); // Brevo SDK

// Import the thingspeak sync module
const { startSync } = require('./thingspeak_firebase');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// Twilio setup
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const serviceSid = process.env.TWILIO_SERVICE_SID;
const client = twilio(accountSid, authToken);

// Brevo setup
const brevoApiKey = process.env.BREVO_API_KEY;
const senderEmail = process.env.SENDER_EMAIL; // Must be verified in Brevo
const defaultClient = SibApiV3Sdk.ApiClient.instance;
defaultClient.authentications['api-key'].apiKey = brevoApiKey;
const brevoEmailApi = new SibApiV3Sdk.TransactionalEmailsApi();

// In-memory store for email OTPs
const otpStore = {};

// ✅ SMS OTP - Send
app.post('/send-otp', (req, res) => {
    const { phoneNumber } = req.body;
    if (!phoneNumber) return res.status(400).json({ error: 'Phone number is required' });

    client.verify.services(serviceSid)
        .verifications
        .create({ to: phoneNumber, channel: 'sms' })
        .then(verification => {
            console.log(`OTP sent to ${phoneNumber}. SID: ${verification.sid}`);
            res.json({ success: true, message: 'OTP sent successfully', sid: verification.sid });
        })
        .catch(err => {
            console.error('Error sending OTP:', err);
            res.status(500).json({ success: false, message: 'Failed to send OTP', error: err.message });
        });
});

// ✅ SMS OTP - Verify
app.post('/verify-otp', (req, res) => {
    const { phoneNumber, otp } = req.body;
    if (!phoneNumber || !otp) return res.status(400).json({ error: 'Phone number and OTP are required' });

    client.verify.services(serviceSid)
        .verificationChecks
        .create({ to: phoneNumber, code: otp })
        .then(verification_check => {
            if (verification_check.status === 'approved') {
                res.json({ success: true, message: 'OTP verified successfully' });
            } else {
                res.status(401).json({ success: false, message: 'Invalid or expired OTP' });
            }
        })
        .catch(err => {
            console.error('Error verifying OTP:', err);
            res.status(500).json({ success: false, message: 'Failed to verify OTP', error: err.message });
        });
});

// ✅ Email OTP - Send (using Brevo)
app.post('/send-email-otp', async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'Email is required' });

    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    otpStore[email] = otp;

    const emailData = {
        to: [{ email }],
        sender: { name: 'Your App', email: senderEmail },
        subject: 'Your OTP Verification Code',
        textContent: `Your OTP is: ${otp}`,
    };

    try {
        await brevoEmailApi.sendTransacEmail(emailData);
        console.log(`Email OTP sent to ${email}: ${otp}`);
        res.status(200).json({ success: true, message: 'OTP sent to email' });
    } catch (error) {
        console.error('Email OTP send error:', error);
        res.status(500).json({ success: false, message: 'Failed to send OTP', error: error.message });
    }
});

// ✅ Email OTP - Verify
app.post('/verify-email-otp', (req, res) => {
    const { email, otp } = req.body;
    if (!email || !otp) return res.status(400).json({ error: 'Email and OTP are required' });

    if (otpStore[email] === otp) {
        delete otpStore[email];
        return res.status(200).json({ success: true, message: 'Email verified successfully' });
    } else {
        return res.status(401).json({ success: false, message: 'Invalid OTP' });
    }
});

// ✅ Test route
app.get('/', (req, res) => {
    res.send('OTP service (Twilio + Brevo) is running!');
});

// Start server and also start the ThingSpeak sync
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`);
    startSync();  // start ThingSpeak => Firebase syncing in the background
});