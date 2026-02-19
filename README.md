# El País Opinion Scraper – Selenium + BrowserStack Assignment

##  Overview

This project demonstrates web scraping, API integration, text processing, and cross-browser testing using Selenium.

The automation script performs the following:

- Navigates to **El País (Spanish news website)**
- Ensures website language is Spanish
- Scrapes the first 5 articles from the **Opinion** section
- Extracts:
    - Article title (Spanish)
    - Article content (Spanish)
    - Cover image (if available)
- Downloads article cover images locally
- Translates article titles to English using a Translation API
- Analyzes translated headers and identifies words repeated more than twice
- Executes cross-browser testing locally and on BrowserStack (5 parallel threads)

---

##  Tech Stack

- Java
- Selenium WebDriver
- TestNG
- Maven
- BrowserStack Automate
- Translation API (RapidAPI / Google Translate API)

---

##  Project Structure

elpais-assignment/
│
├── src/main/java
│ ├── base
│ ├── pages
│ └── utils
│
├── src/test/java
│ └── tests
│
├── testng.xml
├── pom.xml
└── README.md


---

##  How to Run Locally

###  Clone the Repository

```bash```
git clone <your-repo-url>
cd elpais-assignment

###  Install Dependencies
mvn clean install

## Run Tests Locally

Set runLocal=true in testng.xml or configuration file.

Then execute:

mvn test

## Run on BrowserStack
###  1️⃣ Set Environment Variables

Mac/Linux:

export BROWSERSTACK_USERNAME=your_username
export BROWSERSTACK_ACCESS_KEY=your_access_key


Windows (PowerShell):

setx BROWSERSTACK_USERNAME "your_username"
setx BROWSERSTACK_ACCESS_KEY "your_access_key"

### 2️⃣ Execute BrowserStack Suite

Ensure parallel="tests" and thread-count="5" in testng.xml.

Then run:

mvn test -DsuiteXmlFile=testng.xml

###  Browser Coverage
- Desktop Browsers

Chrome (Windows 11)

Firefox (Windows 11)

Edge (Windows 11)

- Mobile Browsers

Google Pixel 7 – Chrome (Android 13)

iPhone 14 – Safari (iOS 16)

Parallel execution: 5 threads

 ##  Translation Logic

Article titles are translated from Spanish to English using a translation API

If API fails or rate limit is exceeded, original text is retained

Translated titles are processed to identify words repeated more than twice

Repeated words and their occurrence counts are printed in console

##  Image Handling

If an article contains a cover image:

Image URL is extracted

Image is downloaded and saved locally

##  Word Analysis Logic

From translated titles:

Words are normalized (lowercase, punctuation removed)

Words occurring more than twice across all titles are identified

Output format:

Word: example | Count: 3

##  Cross-Browser Execution

Tests validated locally

Tests executed on BrowserStack using 5 parallel threads

Supports scalable parallel execution configuration

##  Notes

BrowserStack credentials are not stored in code (environment variables used)

Framework supports configurable parallel execution

Clean separation of base setup, page logic, and utilities

##  Submission

This repository contains:

Complete source code

BrowserStack configuration

Parallel execution setup

Documentation for local and cloud execution

##  Author

Shubham Shinare
