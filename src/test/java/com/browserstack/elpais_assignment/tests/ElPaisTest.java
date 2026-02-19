package com.browserstack.elpais_assignment.tests;

import com.browserstack.elpais_assignment.base.BaseTest;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import io.github.cdimascio.dotenv.Dotenv;

public class ElPaisTest extends BaseTest {

    WebDriverWait wait;
    StringBuilder allTranslatedTitles = new StringBuilder();

    @BeforeClass
    @Parameters({"browser", "browserVersion", "os", "osVersion", "deviceName", "runLocal"})
    public void start(String browser, String browserVersion, String os, String osVersion, String deviceName, String runLocal) throws Exception {
        setup(browser, browserVersion, os, osVersion, deviceName, runLocal);
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(20));
    }

    @Test
    public void scrapeOpinionArticles() {

        getDriver().get("https://elpais.com/opinion/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        handleCookies();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article")));

        for (int i = 0; i < 5; i++) {

            try {

                // Re-fetch articles every loop (fix stale element)
                List<WebElement> articles =
                        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                                By.cssSelector("article")));

                if (articles.size() <= i) {
                    System.out.println("Less than 5 articles available.");
                    break;
                }

                WebElement titleLink =
                        articles.get(i).findElement(By.cssSelector("h2 a"));

                String title = titleLink.getText().trim();
                if (title.isEmpty()) continue;

                System.out.println("\n------ ARTICLE " + (i + 1) + " ------");
                System.out.println("Title (Spanish): " + title);

                String englishTitle = translateToEnglish(title);
                System.out.println("Title (English): " + englishTitle);

                allTranslatedTitles.append(englishTitle).append(" ");

                //  Only JS click (removed double click)
                ((JavascriptExecutor) getDriver())
                        .executeScript("arguments[0].click();", titleLink);

                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("article")));

                downloadArticleImage(i);
                extractArticleContent();

                getDriver().navigate().back();
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article")));

            } catch (StaleElementReferenceException e) {

                System.out.println("Retrying article " + (i + 1) + " due to stale element.");
                i--; // retry same article

            } catch (Exception e) {

                System.out.println("Error processing article " + (i + 1));
                e.printStackTrace();

                getDriver().get("https://elpais.com/opinion/");
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article")));
            }
        }

        analyzeTranslatedHeaders(allTranslatedTitles.toString());
    }

    // ================= COOKIES =================

    private void handleCookies() {
        try {
            WebElement acceptButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(.,'Aceptar') or contains(.,'Accept')]")
                    )
            );
            acceptButton.click();
            System.out.println("Cookies accepted.");
        } catch (Exception e) {
            System.out.println("No cookie popup found.");
        }
    }

    // ================= IMAGE DOWNLOAD =================

    private void downloadArticleImage(int index) {
        try {

            List<WebElement> images =
                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.cssSelector("article img")));

            if (!images.isEmpty()) {

                String imageUrl = images.get(0).getAttribute("src");

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    downloadImage(imageUrl, "article_" + (index + 1) + ".jpg");
                }

            } else {
                System.out.println("No image found for article " + (index + 1));
            }

        } catch (Exception e) {
            System.out.println("Image not available for article " + (index + 1));
        }
    }

    private void downloadImage(String imageUrl, String fileName) {
        try {

            URL url = new URL(imageUrl);
            InputStream in = url.openStream();

            File directory = new File("images");
            if (!directory.exists()) directory.mkdir();

            FileOutputStream out = new FileOutputStream("images/" + fileName);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            in.close();
            out.close();

            System.out.println("Image downloaded: " + fileName);

        } catch (Exception e) {
            System.out.println("Failed to download image: " + fileName);
        }
    }

    // ================= CONTENT EXTRACTION =================

    private void extractArticleContent() {

        try {

            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            List<WebElement> paragraphs =
                    getDriver().findElements(By.xpath("//article//p"));

            if (paragraphs.isEmpty()) {
                paragraphs =
                        getDriver().findElements(By.xpath("//div[contains(@class,'article')]//p"));
            }

            if (paragraphs.isEmpty()) {
                System.out.println("No paragraphs found.");
                return;
            }

            System.out.println("Content Preview:");

            int count = 0;

            for (WebElement para : paragraphs) {

                String text = para.getText().trim();

                if (!text.isEmpty()) {
                    System.out.println(text);
                    count++;
                }

                if (count == 5) break;
            }

        } catch (Exception e) {
            System.out.println("Error extracting article content.");
        }
    }

    // ================= TRANSLATION =================

    private String translateToEnglish(String text) {

        String apiKey = "298e9fdb66msh579519faebd9b1ap1e6afbjsn6b0ad64526c3";
        String apiHost = "rapid-translate-multi-traduction.p.rapidapi.com";
        try {

            URL url = new URL("https://rapid-translate-multi-traduction.p.rapidapi.com/t");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-RapidAPI-Key", apiKey);
            conn.setRequestProperty("X-RapidAPI-Host", apiHost);

            conn.setDoOutput(true);

            String jsonInput = "{"
                    + "\"from\": \"es\","
                    + "\"to\": \"en\","
                    + "\"q\": [\"" + text.replace("\"", "\\\"") + "\"]"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8")
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }

            br.close();

            String result = response.toString();
            return result.replace("[\"", "").replace("\"]", "");

        } catch (Exception e) {
            System.out.println("Translation failed.");
            return text;
        }
    }

    // ================= WORD FREQUENCY =================

    private void analyzeTranslatedHeaders(String text) {

        System.out.println("\n------ TRANSLATED HEADER ANALYSIS ------");

        text = text.toLowerCase().replaceAll("[^a-zA-Z ]", "");
        String[] words = text.split("\\s+");

        Map<String, Integer> wordCount = new HashMap<>();

        for (String word : words) {
            if (word.length() > 2) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        boolean found = false;

        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            if (entry.getValue() > 2) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
                found = true;
            }
        }

        if (!found) {
            System.out.println("No words repeated more than twice.");
        }
    }

    @AfterClass
    public void close() {
        tearDown();
    }
}
