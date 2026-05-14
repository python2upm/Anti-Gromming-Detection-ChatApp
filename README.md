## ChatApp
An Android-based conversation application built using Java in Android Studio, with Firebase as the backend service. It provides real-time messaging, secure authentication, and cloud-hosted data storage to deliver a smooth and reliable chat experience.

## Features
1. Sign In & Sign Up Using Firestore
2. Sign Out
3. Display User List
4. RealTime Chat
5. Recent Conversations
6. User Availability
7. Notification (Auto-refresh Firebase access token)

## Demo
[![Download](https://img.shields.io/badge/Download-v1.0.0-blue?style=flat-square)](https://github.com/zilitye/ChatApp/releases/download/v1.0.0/app-release.apk)

Install and run `app-release.apk` requires Android 5.0 or higher

![demo](image.png)

## How To Use
1. Create a Firebase Project

2. Download the `google-services.json` file from your Firebase project.

3. Place `google-services.json` it inside your app’s `app/` directory in Android Studio.

4. Navigate to Project Settings.

5. Select the Service Account tab.

6. Click on Generate New Private Key to download the `service_account.json` file.

7. Place `service_account.json` it inside your app’s `app/src/main/assets` directory in Android Studio.



