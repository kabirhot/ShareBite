// food-donor.html  ->  POST /api/donations
if (requireLogin()) {
    blockPastDates();
    const form = document.getElementById('donorForm');
    connectForm(form, '/api/donations', (saved) => {
        form.reset();
        showMessage('Thank you! Your donation of ' + saved.plates + ' plate(s) of ' + saved.foodName
            + ' has been recorded. You can see it on the dashboard.', 'success');
    });
}
