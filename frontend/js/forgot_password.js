// forgot_password.html  ->  POST /api/auth/otp/send, /api/auth/otp/verify, /api/auth/reset-password
const form = document.getElementById('forgotForm');
const sendBtn = document.getElementById('sendOtpBtn');
const verifyBtn = document.getElementById('verifyOtpBtn');
const newPasswordArea = document.getElementById('newPasswordArea');

sendBtn.addEventListener('click', () => {
    if (!form.email.reportValidity()) return;
    withButtonLocked(sendBtn, async () => {
        try {
            const reply = await apiRequest('POST', '/api/auth/otp/send',
                { email: form.email.value, purpose: 'RESET' });
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
            await apiRequest('POST', '/api/auth/otp/verify',
                { email: form.email.value, purpose: 'RESET', otp: form.otp.value });
            newPasswordArea.hidden = false;        // now show the "new password" box
            form.newPassword.required = true;
            form.newPassword.focus();
            showMessage('OTP verified. Enter your new password.', 'success');
        } catch (error) {
            showMessage(error.message, 'error');
        }
    });
});

form.addEventListener('submit', (event) => {
    event.preventDefault();
    if (newPasswordArea.hidden) {
        showMessage('Please verify the OTP first.', 'error');
        return;
    }
    withButtonLocked(document.getElementById('resetBtn'), async () => {
        try {
            const reply = await apiRequest('POST', '/api/auth/reset-password',
                { email: form.email.value, newPassword: form.newPassword.value });
            form.reset();
            newPasswordArea.hidden = true;
            showMessage(reply.message, 'success');
        } catch (error) {
            showFieldErrors(form, error.fields);
            showMessage(error.message, 'error');
        }
    });
});
