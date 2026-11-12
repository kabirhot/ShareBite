// login.html  ->  POST /api/auth/login
const form = document.getElementById('loginForm');

form.addEventListener('submit', (event) => {
    event.preventDefault();
    withButtonLocked(form.querySelector('button[type="submit"]'), async () => {
        try {
            const reply = await apiRequest('POST', '/api/auth/login', formToObject(form));
            saveLogin(reply);                      // remember the token for the next pages
            location.href = 'dashboard.html';
        } catch (error) {
            showFieldErrors(form, error.fields);
            showMessage(error.message, 'error');
        }
    });
});
