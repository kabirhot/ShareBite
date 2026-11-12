// contact.html  ->  POST /api/contact   (no login needed)
const form = document.getElementById('contactForm');
connectForm(form, '/api/contact', (reply) => {
    form.reset();
    showMessage(reply.message, 'success');
});
