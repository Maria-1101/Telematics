const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const twilio = require('twilio');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// ⚠️ Replace these with actual values or use environment variables (recommended)
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const serviceSid = process.env.TWILIO_SERVICE_SID;

const client = twilio(accountSid, authToken);

// ✅ Send OTP
app.post('/send-otp', (req, res) => {
    const { phoneNumber } = req.body;

    if (!phoneNumber) {
        return res.status(400).json({ error: 'Phone number is required' });
    }

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

// ✅ Verify OTP
app.post('/verify-otp', (req, res) => {
    const { phoneNumber, otp } = req.body;

    if (!phoneNumber || !otp) {
        return res.status(400).json({ error: 'Phone number and OTP are required' });
    }

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

// Test endpoint
app.get('/', (req, res) => {
    res.send('OTP service is running!');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`);
});