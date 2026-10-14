# AcademiX – Campus Communication & Workflow Platform

AcademiX is a native Android application designed to make academic communication and everyday university activities easier for students, teachers, and teaching assistants (TAs).

The idea behind AcademiX is simple: bring important academic information, course-related activities, and campus communication together in one place. Instead of relying on different platforms for notices, class routines, assignments, and communication, AcademiX aims to provide a more organized and convenient experience through a single mobile application.

The project is being developed as part of our university coursework using Kotlin, XML layouts, and Firebase services.

## Project Overview

- **Project Name:** AcademiX
- **Project Type:** Native Android Application
- **Programming Language:** Kotlin
- **UI Design:** XML Layouts
- **Database:** Firebase Cloud Firestore
- **Authentication:** Firebase Authentication
- **Platform:** Android

## Main Objectives

The main goal of AcademiX is to improve communication and simplify academic activities within a university environment.

The project aims to:

- Provide secure registration and login for university users.
- Maintain student profiles and academic information.
- Organize students into academic groups and course groups.
- Make academic notices and class schedule updates easier to access.
- Support communication between students and other authorized academic users.
- Provide a place for assignment distribution and submission.
- Allow students to submit academic help requests and follow their progress.
- Keep academic information organized and accessible from a mobile device.

## Features

### Implemented So Far

**1. User Registration**
- Registration form for collecting user and academic information.
- Input validation before account creation.
- Firebase Authentication integration.
- Email verification support.
- Temporary storage of registration information using SharedPreferences.

**2. User Login**
- Login using email and password.
- Student email format validation.
- Email verification check.
- Password reset email support.
- Firestore profile creation during the login flow.

**3. Home Dashboard**
- Dashboard layout with quick-access cards.
- Display of the logged-in user's profile information.
- Course information displayed from the user's Firestore profile.
- Today's class routine displayed from locally defined schedule data.
- Navigation to the available application sections.

**4. Profile**
- Display of user information, including name, email, student ID, and academic details.
- Logout functionality using Firebase Authentication.

**5. Basic Academic Screens**
- Notice screen layout.
- Assignment screen layout.
- Class routine screen with selectable days.
- Course cards and routine cards.

### Features Planned for Further Development

- Role-based access for students, teachers, and TAs.
- Academic group and course management.
- Real-time direct messaging and group communication.
- Firebase-backed notice publishing and viewing.
- Assignment creation, file upload, and submission management.
- Academic help requests and ticket status tracking.
- Push notifications for important academic updates.
- Improved security rules, testing, and final application polishing.

*Note: Some screens are currently at the UI or navigation stage. The planned features will be implemented and tested as development continues.*

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Application logic and event handling |
| XML | Designing Android screens |
| Android Studio | Development environment |
| Firebase Authentication | Account creation, login, and email verification |
| Cloud Firestore | Storing user profiles and future academic data |
| Material Components | UI components and visual styling |
| AndroidX | Android application and UI support libraries |

## Project Structure

The project is organized into Kotlin source files and XML resources.

```text
AcademiX/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/academix/
│   │       │   ├── LoginActivity.kt
│   │       │   ├── RegisterActivity.kt
│   │       │   ├── MainActivity.kt
│   │       │   ├── HomeFragment.kt
│   │       │   ├── NoticeFragment.kt
│   │       │   ├── AssignmentFragment.kt
│   │       │   ├── ProfileFragment.kt
│   │       │   ├── RoutineFragment.kt
│   │       │   ├── Course.kt
│   │       │   ├── CourseCatalog.kt
│   │       │   └── RoutineCatalog.kt
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── drawable/
│   │       │   ├── menu/
│   │       │   └── values/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── README.md
```

The structure above highlights the main files and folders. Additional Android resource and project configuration files may also be present.

## How the Application Works

The current application flow is:

1. A user opens AcademiX and reaches the login screen.
2. A new user can open the registration screen and provide the required information.
3. Firebase Authentication creates the account and sends an email verification message.
4. The user verifies their email and returns to the application to log in.
5. The application checks the authentication and email verification status.
6. The application creates or loads the user's Firestore profile as appropriate.
7. After successful login, the user can access the dashboard and available screens.

## Getting Started

To open and run AcademiX on your computer:

1. Install Android Studio.
2. Clone the repository or open the project folder in Android Studio.
3. Allow Android Studio to sync the Gradle dependencies.
4. Connect the application to the appropriate Firebase project.
5. Make sure Firebase Authentication and Cloud Firestore are configured.
6. Ensure the required Android SDK version is installed.
7. Run the application on an Android emulator or a compatible physical device.

### Firebase Configuration

The application uses Firebase services, so a valid Firebase Android configuration is required.

- Register the Android application in Firebase using the package name `com.example.academix`.
- Place the appropriate `google-services.json` file inside the `app/` directory.
- Enable Email/Password authentication in Firebase Authentication.
- Configure Cloud Firestore and publish suitable security rules before testing database operations.

**Security note:** Never commit passwords, private keys, service-account credentials, or other sensitive secrets to a public repository. Review the repository contents before pushing changes to GitHub.

## Current Development Status

AcademiX is under active development. The registration and login flow, Firebase integration, dashboard structure, profile screen, and basic academic screens have been developed.

The next stage focuses on improving role management, connecting more screens to Firestore, and implementing the remaining communication and academic workflow features.

The goal is to build the application incrementally, testing each feature before moving on to the next one.

## Future Goal

The long-term goal is to make AcademiX a practical campus companion where students can access academic information, communicate with classmates, follow important notices, manage assignments, and request academic assistance from one organized platform.

---

**AcademiX — Bringing academic communication and campus activities together in one place.**