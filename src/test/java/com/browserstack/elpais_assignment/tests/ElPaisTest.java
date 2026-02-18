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

public class ElPaisTest extends BaseTest {

    WebDriverWait wait;
    StringBuilder allTranslatedTitles = new StringBuilder();

    @BeforeClass
    public void start() {
        setup();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @Test
    public void scrapeOpinionArticles() {

        driver.get("https://elpais.com/opinion/");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

        handleCookies();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));

        for (int i = 0; i < 5; i++) {

            try {
                List<WebElement> articles = driver.findElements(By.cssSelector("article"));

                if (articles.size() <= i) {
                    System.out.println("Less than 5 articles available.");
                    break;
                }

                WebElement article = articles.get(i);
                WebElement titleLink = article.findElement(By.cssSelector("h2 a"));

                String title = titleLink.getText().trim();
                if (title.isEmpty()) continue;

                System.out.println("\n------ ARTICLE " + (i + 1) + " ------");
                System.out.println("Title (Spanish): " + title);

                String englishTitle = translateToEnglish(title);
                System.out.println("Title (English): " + englishTitle);

                allTranslatedTitles.append(englishTitle).append(" ");

                titleLink.click();
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));

                downloadArticleImage(i);
                extractArticleContent();

                driver.get("https://elpais.com/opinion/");
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));

            } catch (Exception e) {
                System.out.println("Error processing article " + (i + 1));
                e.printStackTrace();
                driver.get("https://elpais.com/opinion/");
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));
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

            List<WebElement> images = driver.findElements(By.cssSelector("article img"));

            if (images.size() > 0) {
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

            List<WebElement> paragraphs = driver.findElements(By.xpath("//article//p"));

            if (paragraphs.size() == 0) {
                paragraphs = driver.findElements(
                        By.xpath("//div[contains(@class,'article')]//p")
                );
            }

            if (paragraphs.size() == 0) {
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
            e.printStackTrace();
        }
    }

    // ================= TRANSLATION =================

    private String translateToEnglish(String text) {

        String apiKey = "";
        String apiHost = "";

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
                    + "\"q\": [\"" + text + "\"]"
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
