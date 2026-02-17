package com.browserstack.elpais_assignment.tests;

import com.browserstack.elpais_assignment.base.BaseTest;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.List;
import java.io.*;
import java.net.URL;

import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;




public class ElPaisTest extends BaseTest {

    WebDriverWait wait;

    @BeforeClass
    public void start() {
        setup();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @Test
    public void scrapeOpinionArticles() {

        driver.get("https://elpais.com/opinion/");

        // Wait for body
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

        // Accept cookies if present
        handleCookies();

        // Wait for articles
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));

        for (int i = 0; i < 5; i++) {

            try {

                // Re-fetch articles every loop to avoid stale reference
                List<WebElement> articles = driver.findElements(By.cssSelector("article"));

                if (articles.size() <= i) {
                    System.out.println("Less than 5 articles available.");
                    break;
                }

                WebElement article = articles.get(i);

                // Get title link (more reliable than just h2)
                WebElement titleLink = article.findElement(By.cssSelector("h2 a"));
                String title = titleLink.getText().trim();

                if (title.isEmpty()) {
                    System.out.println("Skipping article " + (i + 1) + " (empty title)");
                    continue;
                }

                System.out.println("\n------ ARTICLE " + (i + 1) + " ------");
                System.out.println("Title (Spanish): " + title);

                String englishTitle = translateToEnglish(title);
                System.out.println("Title (English): " + englishTitle);


                // Click article
                titleLink.click();

                // Wait for article content to load
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));

                // Get cover image
                try {
                    WebElement imageElement = driver.findElement(By.cssSelector("article img"));
                    String imageUrl = imageElement.getAttribute("src");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        downloadImage(imageUrl, "article_" + (i + 1) + ".jpg");
                    } else {
                        System.out.println("No image found for article " + (i + 1));
                    }

                } catch (Exception e) {
                    System.out.println("Image not available for article " + (i + 1));
                }


                // Try main content selector first
                List<WebElement> paragraphs = driver.findElements(
                        By.cssSelector("article p")
                );

                if (paragraphs.isEmpty()) {
                    System.out.println("No paragraphs found.");
                } else {

                    System.out.println("Content Preview:");

                    int printed = 0;

                    for (WebElement para : paragraphs) {

                        String text = para.getText().trim();

                        if (!text.isEmpty()) {
                            System.out.println(text);
                            printed++;
                        }

                        if (printed == 5) break;
                    }
                }

                // Go back
                driver.navigate().back();

                wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));

            } catch (Exception e) {
                System.out.println("Error processing article " + (i + 1));
                driver.navigate().back();
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("article")));
            }
        }

    }

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

    private void downloadImage(String imageUrl, String fileName) {
        try {
            URL url = new URL(imageUrl);
            InputStream in = url.openStream();

            File directory = new File("images");
            if (!directory.exists()) {
                directory.mkdir();
            }

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

    private String translateToEnglish(String text) {

        String apiKey = "";
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
                    + "\"q\": \"" + text + "\""
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

            // Response comes as array: ["Translated Text"]
            String result = response.toString();
            return result.replace("[\"", "").replace("\"]", "");

        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }



    @AfterClass
    public void close() {
        tearDown();
    }
}
