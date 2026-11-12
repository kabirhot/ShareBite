// signin.html  ->  POST /api/auth/otp/send, /api/auth/otp/verify, /api/auth/signup
const form = document.getElementById('signinForm');
const sendBtn = document.getElementById('sendOtpBtn');
const verifyBtn = document.getElementById('verifyOtpBtn');
let otpVerified = false;

// Which page each service goes to after signing in
const SERVICE_PAGES = {
    fooddonor: 'food-donor.html',
    foodreceiver: 'food-receiver.html',
    volunteers: 'volunteers.html'
};

sendBtn.addEventListener('click', () => {
    if (!form.email.reportValidity()) return;      // email must be filled in first
    withButtonLocked(sendBtn, async () => {
        try {
            const reply = await apiRequest('POST', '/api/auth/otp/send',
                { email: form.email.value, purpose: 'SIGNUP' });
            otpVerified = false;
            let text = reply.message;
            if (reply.demoOtp) text += ' Demo mode: your OTP is ' + reply.demoOtp;
            showMessage(text, 'info');
        } catch (error) {
            showMessage(error.message, 'error');
        }
    });
});

verifyBtn.addEventListener('click', () => {
    if (!form.email.reportValidity() || !form.otp.reportValidity()) return;
    withButtonLocked(verifyBtn, async () => {
        try {
            const reply = await apiRequest('POST', '/api/auth/otp/verify',
                { email: form.email.value, purpose: 'SIGNUP', otp: form.otp.value });
            otpVerified = true;
            showMessage(reply.message + ' Now choose a service and press SUBMIT.', 'success');
        } catch (error) {
            showMessage(error.message, 'error');
        }
    });
});

// If the email is changed after verifying, the OTP has to be verified again.
form.email.addEventListener('input', () => { otpVerified = false; });

form.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!otpVerified) {
        showMessage('Please verify your email with the OTP first.', 'error');
        return;
    }
    withButtonLocked(form.querySelector('button[type="submit"]'), async () => {
        try {
            const data = formToObject(form);
            delete data.otp;                       // already verified, not needed again
            const reply = await apiRequest('POST', '/api/auth/signup', data);
            saveLogin(reply);
            location.href = SERVICE_PAGES[reply.service] || 'dashboard.html';
        } catch (error) {
            showFieldErrors(form, error.fields);
            showMessage(error.message, 'error');
        }
    });
});
