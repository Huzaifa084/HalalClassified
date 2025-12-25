# **📱 Halal Qurbani Animal Marketplace App**

**Frontend Requirements & App Flow (Using Supabase)**

---

## **1️⃣ Purpose of This Document**

This document defines:

* Complete **app flow**

* **Screen behavior**

* **User interactions**

* **Data expectations from backend**

This document is meant for **frontend development only**.

❌ No backend coding  
 ❌ No SQL / API code  
 ✅ Clear usage of **Supabase services**

---

## **2️⃣ Mandatory Technology Constraint**

⚠ **Supabase MUST be used** for this application.

Frontend developer must integrate with Supabase for:

* Authentication

* Database data

* Image storage

* Real-time chat

No custom backend server will be used.

---

## **3️⃣ Target Platform**

* Mobile Application

* Country: **Pakistan only**

* Users: Buyers & Sellers (same role)

---

## **4️⃣ User Roles**

There are **no separate roles**.

✔ Every logged-in user can:

* Post ads

* View ads

* Chat

* Call sellers

* Save favorites

---

## **5️⃣ Authentication & Account Flow (Supabase Auth)**

### **5.1 First App Launch**

Show:

* App logo

* Buttons:

  * **Continue with Google**

  * **Sign up with Email**

  * **Login**

---

### **5.2 Google Signup (Supabase OAuth)**

* App uses **Supabase Google Sign-In**

* Automatically fetch:

  * First Name

  * Last Name

  * Email

* App MUST ask for:

  * Date of Birth (manual input)

➡ After completion → Home Screen

---

### **5.3 Email & Password Signup (Supabase Auth)**

User must enter:

* First Name

* Last Name

* Email

* Password

* Date of Birth

➡ Successful signup → Home Screen

---

### **5.4 Login Persistence**

* Supabase session should remain active

* App must:

  * Remember logged-in accounts

  * Allow selecting an already logged-in account

  * Allow adding a new account

---

## **6️⃣ App Navigation Structure**

Bottom Navigation Tabs:

1. Home

2. Chats

3. Sell (Post Ad)

4. My Ads

5. Profile

---

## **7️⃣ Home Screen Requirements**

### **7.1 Category Section**

Categories to display:

* Cows

* Bulls

* Goats

* Sheep

* Camels

* Buffaloes

➡ Clicking category fetches ads **from Supabase database**

---

### **7.2 City Filter (Pakistan Only)**

* List of all cities of Pakistan

* Search city by name

* Options:

  * Individual city

  * **All Pakistan**

➡ Ads reload from Supabase based on city filter

---

### **7.3 Ads Listing**

Each ad card shows:

* Image (from Supabase Storage)

* Price

* Category

* City

* Short description

* Favorite icon (❤️)

---

## **8️⃣ Ad Detail Screen**

Displays:

* Image gallery (Supabase Storage images)

* Price

* Category

* Breed

* Gender

* Description

* City

* Seller name

### **Action Buttons:**

* 💬 Chat

* 📞 Call

* ❤️ Favorite

---

### **Call Button Behavior**

* Copy phone number

* Open device dialer

---

## **9️⃣ In-App Chat (Supabase Realtime)**

* Chat opens per ad

* One buyer ↔ one seller

* Real-time text messaging

* Message timestamp visible

* No audio/video features

---

## **🔟 Post Ad Flow (Sell)**

### **Mandatory Inputs:**

* Upload multiple images (Supabase Storage)

* Select category

* Select gender

* Select breed (based on category)

* Description

* Phone number

* Price

* Select city (Pakistan cities list)

### **Optional Inputs:**

* Age

* Weight

* Vaccination status

* Delivery availability

➡ On submit:

* Ad is saved in Supabase

* Ad becomes visible to all users

---

## **1️⃣1️⃣ My Ads Screen**

* Fetch ads posted by logged-in user from Supabase

* Each ad has:

  * Edit

  * Delete

  * Enable / Disable

---

## **1️⃣2️⃣ Favorites Screen**

* Shows ads marked favorite by user

* Stored and fetched from Supabase

* Same UI as home listings

---

## **1️⃣3️⃣ Profile / Account Screen**

Displays:

* First Name

* Last Name

* Email (read-only)

* Phone number

User actions:

* Edit name & phone

* Logout

* Switch account

---

## **1️⃣4️⃣ UI/UX Guidelines**

* OLX-like clean design

* Card-based ads

* Large images

* Simple layouts

* Easy for non-technical users

---

## **1️⃣5️⃣ App Rules & Constraints**

* Only halal animals

* Only Pakistan cities

* No payment gateway

* No admin panel

* No web version

---

## **1️⃣6️⃣ Expected User Flow Summary**

1. App launch

2. Signup / Login (Supabase)

3. Browse ads by category & city

4. View ad details

5. Chat or call seller

6. Post own ads

7. Manage ads & profile

---

## **1️⃣7️⃣ Completion Checklist (For Frontend)**

✔ Supabase Auth integrated  
 ✔ Google login working  
 ✔ Ads fetched from Supabase  
 ✔ Images from Supabase Storage  
 ✔ Realtime chat working  
 ✔ Filters working  
 ✔ Favorites working

