# Android Monitoring Library & Client

## Overview
    
This repository hosts a comprehensive solution for real-time logging and monitoring of Android devices, consisting of two primary components:

1. **monitoring-library:** An Android monitoring library written in Kotlin that uses RabbitMQ to send detailed app logs and system metrics.

2. **monitoring-client:** A Node.js script that consumes and processes logs from the Android devices using the monitoring-library.

## Project Description

The Android Monitoring Library provides a robust and efficient way to monitor and log Android device metrics and application behaviors in real-time. It leverages RabbitMQ, a high-performance messaging protocol, to ensure reliable data transmission between the Android app and backend services.

### Key Features:

- **Real-Time Logging:** Captures logs related to device performance, application behavior, and system metrics.

- **RabbitMQ Integration:** Transmits logs securely and efficiently using RabbitMQ (via MQTT) to a specified server.

- **Backend Compatibility:** Logs can be consumed by various backend services for processing, analysis, and visualization.

The **monitoring-client** complements the library by providing a Node.js script that listens to RabbitMQ queues, processes incoming logs, and enables integration with other systems like databases, web interfaces, or alerting mechanisms.

## Getting Started

1. **Monitoring Library (Android)**

    The monitoring-library is designed to be integrated into your Android application. It enables the application to send logs and device metrics to a RabbitMQ server.

    ### Installation:

    Currently, the library can be installed in one of the following ways:

    - **Publishing to Maven Local:**

        1. Clone the repository.

        2. Navigate to the `monitoring-library` directory.

        3. Run the following command to build and publish the library to your local Maven repository.
            ```bash
            ./gradlew clean assembleRelease publishReleasePublicationToMavenLocal
            ```

        4. Add the following dependency to your Android project's `build.gradle (module-level)` file:
            ```bash
            dependencies {
                implementation 'com.andresuryana.amlib:core:1.0.0'
                implementation 'com.andresuryana.amlib:logging:1.0.0'
            }
            ```

    - **Using a Compiled AAR File:**

        1. Clone the repository.

        2. Navigate to the `monitoring-library` directory.

        3. Build the AAR file by running the following command:

            ```bash
            ./gradlew assembleRelease
            ```

        4. Locate the generated AAR file in the `build/outputs/aar/` directory.

        5. Add the AAR file to your Android project by placing it in the libs directory and adding the following to your build.gradle file:

            ```bash
            repositories {
                flatDir {
                    dirs 'libs'
                }
            }

            dependencies {
                implementation(name: 'android-monitoring-library', ext: 'aar')
            }
            ```

    ### Usage:

    - Initialize the library in your application code.
    - Configure the RabbitMQ server details.
    - Start sending logs from your application.

2. **Monitoring Client (Node.js)**

    The **monitoring-client** is a Node.js script that consumes logs sent by the monitoring-library to the RabbitMQ server.

    ### Installation:

    Clone the repository.
    Navigate to the monitoring-client directory.
    Run npm install to install the necessary dependencies.


    ### Usage:

    - Run the script to start consuming logs from the RabbitMQ server:
        ```bash
        npm run start-logger -- --deviceId "1234567890"
        ```

## Contribution

I welcome contributions! If you find issues or have suggestions for improvements, please open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.