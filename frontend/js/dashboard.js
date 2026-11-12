// dashboard.html  ->  GET /api/donations and GET /api/requests
const login = requireLogin();

const FOOD_TYPES = {
    'vegetarian': 'Vegetarian',
    'non-vegetarian': 'Non-Vegetarian',
    'packaged_foods': 'Packaged Foods',
    'fruits_vegetables': 'Fruits and Vegetables'
};

/**
 * Loads a list from the API and draws it as table rows.
 *   columns: functions that turn one record into the text of one cell
 * Shows "Loading...", "nothing yet" or an error with a Retry button as needed.
 */
async function loadTable(path, tableId, stateId, emptyText, columns) {
    const body = document.querySelector('#' + tableId + ' tbody');
    const table = document.getElementById(tableId);
    const state = document.getElementById(stateId);

    table.hidden = true;
    state.hidden = false;
    state.textContent = 'Loading...';

    try {
        const rows = await apiRequest('GET', path);
        body.replaceChildren();
        if (rows.length === 0) {
            state.textContent = emptyText;
            return;
        }
        rows.forEach((row) => {
            const tr = document.createElement('tr');
            columns.forEach((column) => {
                const td = document.createElement('td');
                td.textContent = column(row);      // textContent: user data is shown as text, never run as HTML
                tr.appendChild(td);
            });
            body.appendChild(tr);
        });
        state.hidden = true;
        table.hidden = false;
    } catch (error) {
        state.textContent = error.message + ' ';
        const retry = document.createElement('button');
        retry.type = 'button';
        retry.textContent = 'Retry';
        retry.addEventListener('click', () => loadTable(path, tableId, stateId, emptyText, columns));
        state.appendChild(retry);
    }
}

if (login) {
    document.getElementById('welcome').textContent = 'Welcome, ' + login.name;

    loadTable('/api/donations', 'donationsTable', 'donationsState',
        'No donations yet. Be the first to donate food.', [
            (d) => d.foodName,
            (d) => d.plates,
            (d) => FOOD_TYPES[d.foodType] || d.foodType,
            (d) => d.pickupAddress,
            (d) => d.pickupDate + ' ' + d.pickupTime,
            (d) => d.donorName + ' (' + d.contact + ')'
        ]);

    loadTable('/api/requests', 'requestsTable', 'requestsState',
        'No food requests yet.', [
            (r) => r.organization,
            (r) => FOOD_TYPES[r.foodType] || r.foodType,
            (r) => r.quantity,
            (r) => r.deliveryLocation,
            (r) => r.requiredDate + ' ' + r.deliveryTime,
            (r) => r.receiverName + ' (' + r.contact + ')'
        ]);
}
