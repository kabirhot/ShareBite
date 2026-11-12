SHOW databases;
USE sharebite;
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15) NOT NULL,
    service VARCHAR(30) NOT NULL,
    password VARCHAR(255) NOT NULL
);

SELECT * FROM  users;

CREATE TABLE food_donor (
    donor_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    donor_name VARCHAR(100) NOT NULL,
    contact VARCHAR(15) NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    food_plates INT NOT NULL,
    food_type VARCHAR(50) NOT NULL,
    pickup_address TEXT NOT NULL,
    confirm_address TEXT NOT NULL,
    pickup_date DATE NOT NULL,
    pickup_time TIME NOT NULL,
    additional_info TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

SELECT * FROM food_donor;

CREATE TABLE food_receiver (
    receiver_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    receiver_name VARCHAR(100) NOT NULL,
    organization VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL,
    contact VARCHAR(15) NOT NULL,
    food_type VARCHAR(50) NOT NULL,
    quantity VARCHAR(100) NOT NULL,
    amount VARCHAR(50) NOT NULL,
    delivery_location TEXT NOT NULL,
    required_date DATE NOT NULL,
    delivery_time TIME NOT NULL,
    additional_requirements TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

SELECT  * FROM  food_receiver;

CREATE TABLE volunteers (
    volunteer_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    volunteer_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    contact VARCHAR(15) NOT NULL,
    location VARCHAR(255) NOT NULL,
    availability VARCHAR(30) NOT NULL,
    volunteer_work VARCHAR(50) NOT NULL,
    vehicle VARCHAR(10),
    vehicle_type VARCHAR(50),
    service_area VARCHAR(255) NOT NULL,
    additional_information TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

select * from volunteers ;

