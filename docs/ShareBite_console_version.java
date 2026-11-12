import java.util.*;

class Database {

    static String email = "abc@gmail.com"; // extracted from database for each user
    static String pswd = "ABCD09";
    static String contact = "1234567891"; // we use static when a value belongs to the class itseld , not just an object
                                          // og the clas. we access it using the class name
}

class Login {
    static String user, userp; // extracted from database for each user
    int ch;
    Scanner sc = new Scanner(System.in);

    Login() { // constructor

    }

    void input() {
        System.out.println("Enter email id:");
        user = sc.nextLine();
        System.out.println("Enter password");
        userp = sc.nextLine();
    }

    public boolean verify() { // confrim : no parameters , ADD LOOP
        if (Database.email.compareTo(user) != 0) {// Checking user
                                                  // credentials
                                                  // using static variable
                                                  // from
                                                  // dummy database
            System.out.println("Email not found!");
            return false;
        } else if (Database.pswd.compareTo(userp) != 0) {
            System.out.println("Invalid Password!");
            return false;
        } else {
            System.out.println("Login Successful!");
            return true;
        }

    }

    void submitInfo() {
        if (verify())
            System.out.println("Data submitted successfully.\n");
        else
            System.out.println("Data not submitted.");

    }

    void showOptions() {
        System.out.println("1.Forgot Password\n2)New here? Sign in!\n Enter your choice.");
        ch = sc.nextInt();
        sc.nextLine();

        switch (ch) {
            case 1:
                ForgetPassword f = new ForgetPassword(user);
                f.verifyEmail();
                f.generateOTP();
                f.verifyOTP();

                break;
            case 2:
                SignIn s = new SignIn();
                s.inputData();
                s.validateInfo();
                s.generateOTP();
                s.verifyOTP();
                s.serviceInput();
                s.connect();
                s.submitInfo();

                break;
            default:
                System.out.println("Invalid Choice");
        }

    }
}

class ForgetPassword {
    String enteredEmail;
    int otp, enteredOTP;
    Scanner sc = new Scanner(System.in);

    ForgetPassword(String user) {
        this.enteredEmail = user;
    }

    void verifyEmail() // CONFIRM
    {
        if (enteredEmail.compareTo(Login.user) == 0)// Directly used email from another class. We could have even
                                                    // passes it as parameters while creating constructor , rigth?
            System.out.println("Email Verified!");
        else
            System.out.println("Incorrect Email ID!");
    }

    void generateOTP() {
        otp = (int) ((Math.random() * 900000) + 100000);
        // System.out.println("OTP Generated:");
    }

    void verifyOTP() {
        System.out.println("Enter OTP:");
        enteredOTP = sc.nextInt();
        sc.nextLine();

        if (enteredOTP == otp)
            System.out.println("Login Successful!");
        else
            System.out.println("Login Unsuccessful!");
    }

}

class Donor {
    Scanner sc = new Scanner(System.in);
    String name, foodName, pAddress, ConfirmPAddress, addInfo, contact, date, time, FoodType;
    int noOfPlates;

    // constructor
    void inputData() {
        System.out.println("Donor Name:");
        name = sc.nextLine();
        System.out.println("Contact Number:");
        contact = sc.nextLine();
        System.out.println("Food Name:");
        foodName = sc.nextLine();
        System.out.println("Number of Food Plates:");
        noOfPlates = sc.nextInt();
        sc.nextLine();
        System.out.println("Enter food type:");
        FoodType = sc.nextLine();
        System.out.println("Pickup Address:");
        pAddress = sc.nextLine();
        System.out.println("Confirm Pickup Address:");
        ConfirmPAddress = sc.nextLine();
        if ((pAddress.equalsIgnoreCase(ConfirmPAddress)))
            System.out.println("Addresses matched");
        else
            System.out.println("Addresses do not match.");
        System.out.println("Enter Pick Up Date:");
        date = sc.nextLine();
        System.out.println("Enter Pick Up Time:");
        time = sc.nextLine();
        System.out.println("Enter additional information:");
        addInfo = sc.nextLine();
    }

    boolean validateInfo() {
        if ((this.name).isEmpty()) {
            System.out.println("Invalid Input!");
            return false;
        }
        if ((this.contact).isEmpty()) {
            System.out.println("Invalid Input!");
            return false;
        }

        if ((this.foodName).isEmpty()) {
            System.out.println("Invalid Input!");
            return false;
        }

        if ((this.noOfPlates) <= 0) {
            System.out.println("Invalid Input!");
            return false;
        }
        if ((this.pAddress).isEmpty()) {
            System.out.println("Invalid Input!");
            return false;
        }
        if ((this.ConfirmPAddress).isEmpty()) {
            System.out.println("Invalid Input!");
            return false;
        }
        return true;

    }

    void submitInfo() {
        if (validateInfo())
            System.out.println("Submitted");
        else
            System.out.println("Invalid!!");

    }

}

class Receiver {
    Scanner sc = new Scanner(System.in);
    String rName, orgName, email, contact, foodType, location, date, time, req;
    int quantity, plates;
    float amount;

    // constructor
    void inputData() {
        System.out.println("Enter Receiver's name :");
        rName = sc.nextLine();
        System.out.println("Enter Organization/NGO's Name :");
        orgName = sc.nextLine();
        System.out.println("Enter email-id");
        email = sc.nextLine();
        System.out.println("Enter Receiver's Contact Number:");
        contact = sc.nextLine();
        System.out.println("Enter Required Food Type:");
        foodType = sc.nextLine();
        System.out.println("Enter Required Quantity(plates) :");
        quantity = sc.nextInt();
        System.out.println("Enter Number of Plates:");
        plates = sc.nextInt();
        System.out.println("Enter Amount:");
        amount = sc.nextFloat();
        sc.nextLine();
        System.out.println("Enter Delivery Location:");
        location = sc.nextLine();
        System.out.println("Enter Pick-Up Date :");
        date = sc.nextLine();
        System.out.println("Enter Delivery Time :");
        time = sc.nextLine();
        System.out.println("Enter Additional Requirements:");
        req = sc.nextLine();

    }

    boolean validateInfo() {
        if (rName.isEmpty()) {
            System.out.println("Invalid Name!");
            return false;
        }
        if (orgName.isEmpty()) {
            System.out.println("Invalid Organization Name!");
            return false;
        }
        if (email.isEmpty()) {
            System.out.println("Invalid Email!");
            return false;
        }
        if (contact.isEmpty()) {
            System.out.println("Invalid Contact Number!");
            return false;
        }
        if (foodType.isEmpty()) {
            System.out.println("Invalid Food Type!");
            return false;
        }
        if (quantity <= 0) {
            System.out.println("Invalid Quantity!");
            return false;
        }
        if (plates <= 0) {
            System.out.println("Invalid Plates!");
            return false;
        }
        if (amount <= 0) {
            System.out.println("Invalid Amount!");
            return false;
        }
        if (location.isEmpty()) {
            System.out.println("Invalid Location!");
            return false;
        }
        if (date.isEmpty()) {
            System.out.println("Invalid Date!");
            return false;
        }
        if (time.isEmpty()) {
            System.out.println("Invalid Time!");
            return false;
        }
        if (req.isEmpty()) {
            System.out.println("Invalid Requirements!");
            return false;
        }
        return true;
    }

    void submitInfo() {
        if (validateInfo())
            System.out.println("Submitted");
        else
            System.out.println("Invalid!!");

    }

}

class Volunteer {
    Scanner sc = new Scanner(System.in);

    String name, email, contact, location, availability, vehicle, vType, serviceArea, info;

    void inputData() {
        System.out.println("Enter Volunteer's name :");
        name = sc.nextLine();
        System.out.println("Enter email-id");
        email = sc.nextLine();
        System.out.println("Enter Volunteer's Contact Number:");
        contact = sc.nextLine();
        System.out.println("Enter volunteer availability:");
        availability = sc.nextLine();
        System.out.println("Do you have a vehicle?");
        vehicle = sc.nextLine();
        System.out.println("Enter vehicle type:");
        vType = sc.nextLine();
        System.out.println("Enter service area:");
        serviceArea = sc.nextLine();
        System.out.println("Enter Additional Information:");
        info = sc.nextLine();

    }

    boolean validateInfo() {
        if (name.isEmpty()) {
            System.out.println("Invalid Name!");
            return false;
        }
        if (email.isEmpty()) {
            System.out.println("Invalid Email!");
            return false;
        }
        if (contact.isEmpty()) {
            System.out.println("Invalid Contact Number!");
            return false;
        }
        if (location.isEmpty()) {
            System.out.println("Invalid Location!");
            return false;
        }

        if (availability.isEmpty()) {
            System.out.println("Invalid Availability!");
            return false;
        }
        return true;
    }

    void submitInfo() {
        if (validateInfo())
            System.out.println("Submitted");
        else
            System.out.println("Invalid!!");

    }

}

class Dashboard {
    Scanner sc = new Scanner(System.in);

    void showDashboard() {
        System.out.println("DASHBOARD");
        System.out.println("1.Login");
        System.out.println("2. Sign in");
        System.out.println("3.About Us");
        System.out.println("4.Logout");
        int choice = sc.nextInt();
        sc.nextLine();
        switch (choice) {
            case 1:
                Login l = new Login();// confrim parameter
                l.input();
                l.submitInfo();
                break;
            case 2:
                SignIn s = new SignIn();
                s.inputData();
                s.validateInfo();
                s.generateOTP();
                s.verifyOTP();
                s.serviceInput();
                s.connect();
                break;
            case 3:
                showAboutUs();
                break;
            case 4:
                System.out.println("Logout Successful!");
                break;
            default:
                System.out.println("Invalid Choice!");
        }

    }

    void showAboutUs() {
        System.out.println("================ ABOUT US ================");
        System.out.println("ShareBite is a platform dedicated to connecting");
        System.out.println("food donors (restaurants, events, individuals)");
        System.out.println("with NGOs, receivers, and volunteers to reduce");
        System.out.println("food waste and fight hunger.");
        System.out.println("==========================================");
    }

}

class SignIn {
    Scanner sc = new Scanner(System.in);
    int otp, enteredOTP;
    String email, name, contact, service;

    void inputData() {
        System.out.println("Enter Name");
        name = sc.nextLine();
        System.out.println("Enter email id:");
        email = sc.nextLine();
        System.out.println("Enter phone number");
        contact = sc.nextLine();
    }

    boolean validateInfo() {
        if (name.isEmpty()) {
            System.out.println("Invalid Name!");
            return false;
        }
        if (email.isEmpty()) {
            System.out.println("Invalid Email ID!");
            return false;
        }
        if (contact.isEmpty()) {
            System.out.println("Invalid Contact Number!");
            return false;
        }
        return true;
    }

    void generateOTP() {
        otp = (int) ((Math.random() * 900000) + 100000);
        System.out.println("OTP Generated : " + otp);
    }

    void verifyOTP() {
        System.out.println("Enter OTP:");
        enteredOTP = sc.nextInt();
        sc.nextLine();
        if (enteredOTP == otp)
            System.out.println("Login Successful!");
        else
            System.out.println("Login Unsuccessful!");

    }

    void serviceInput() {
        System.out.println("Select service:1)Donate Food \n2)Receive Food\n3)Volunteer\n");
        service = sc.nextLine();
    }

    // check service
    boolean validateService() {
        if (service.isEmpty()) {
            System.out.println("Invalid input!");
            return false;
        }
        return true;

    }

    void connect() {

        if (validateService()) {
            if (service.equalsIgnoreCase("Donor")) {
                Donor d = new Donor();
                d.inputData();
                d.submitInfo();
            } else if (service.equalsIgnoreCase("Receiver")) {
                Receiver r = new Receiver();
                r.inputData();
                r.submitInfo();
            } else if (service.equalsIgnoreCase("Volunteer")) {
                Volunteer v = new Volunteer();
                v.inputData();
                v.submitInfo();
            }

        } else
            System.out.println("Invalid input!");

    }

    void submitInfo() {
        if (validateInfo())
            System.out.println("Submitted");
        else
            System.out.println("Invalid!!");

    }

}

public class ShareBite { // primary class

    public static void main(String args[]) {
        Dashboard d = new Dashboard();
        d.showDashboard();
    }

}
