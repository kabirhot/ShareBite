// food-receiver.html  ->  POST /api/requests
if (requireLogin()) {
    blockPastDates();
    const form = document.getElementById('receiverForm');
    connectForm(form, '/api/requests', () => {
        form.reset();
        showMessage('Your food request has been submitted. You can see it on the dashboard.', 'success');
    });
}
