// volunteers.html  ->  POST /api/volunteers
if (requireLogin()) {
    const form = document.getElementById('volunteerForm');
    connectForm(form, '/api/volunteers', (saved) => {
        form.reset();
        showMessage('Thank you, ' + saved.volunteerName + '! You are registered as a volunteer.', 'success');
    });
}
