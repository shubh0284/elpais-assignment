package com.browserstack.elpais_assignment.base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.MutableCapabilities;
import org.testng.annotations.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;

public class BaseTest {

    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public WebDriver getDriver() {
        return driver.get();
    }


    @Parameters({"browser", "browserVersion", "os", "osVersion", "deviceName", "runLocal"})
    @BeforeClass
    public void setup( @Optional("chrome") String browser,
                      @Optional("") String browserVersion,
                      @Optional("") String os,
                      @Optional("") String osVersion,
                      @Optional("") String deviceName,
                      @Optional("true") String runLocal) throws Exception {
        if (runLocal.equalsIgnoreCase("true")) {

            // ================= LOCAL EXECUTION =================

            if (browser.equalsIgnoreCase("chrome")) {
                driver.set(new org.openqa.selenium.chrome.ChromeDriver());

            } else if (browser.equalsIgnoreCase("firefox")) {
                driver.set(new org.openqa.selenium.firefox.FirefoxDriver());

            } else if (browser.equalsIgnoreCase("edge")) {
                driver.set(new org.openqa.selenium.edge.EdgeDriver());

            } else {
                throw new Exception("Unsupported local browser: " + browser);
            }

            getDriver().manage().window().maximize();
            System.out.println("Running test locally on: " + browser);

        } else {


            Dotenv dotenv = Dotenv.load();
            String username = dotenv.get("BROWSERSTACK_USERNAME");
            String accessKey = dotenv.get("BROWSERSTACK_ACCESS_KEY");

            if (username == null || accessKey == null) {
                throw new Exception("CRITICAL: Credentials not found in .env file!");
            }

            MutableCapabilities capabilities = new MutableCapabilities();
            HashMap<String, Object> bstackOptions = new HashMap<>();

            // ---------------- MOBILE CONFIG ----------------
            if (!deviceName.isEmpty()) {

                capabilities.setCapability("browserName", browser);

                bstackOptions.put("deviceName", deviceName);
                bstackOptions.put("osVersion", osVersion);
                bstackOptions.put("realMobile", true);

                bstackOptions.put("sessionName", "Mobile - " + deviceName);
            }

            // ---------------- DESKTOP CONFIG ----------------
            else {

                capabilities.setCapability("browserName", browser);
                capabilities.setCapability("browserVersion", browserVersion);

                bstackOptions.put("os", os);
                bstackOptions.put("osVersion", osVersion);

                bstackOptions.put("sessionName", "Desktop - " + browser);
            }

            // Common BrowserStack Options
            bstackOptions.put("projectName", "Shubham ElPais Automation");
            bstackOptions.put("buildName", "Build-v1.0-" + LocalDate.now());
            bstackOptions.put("local", false);

            capabilities.setCapability("bstack:options", bstackOptions);

            driver.set(new RemoteWebDriver(
                    new URL("https://" + username + ":" + accessKey +
                            "@hub.browserstack.com/wd/hub"),
                    capabilities));
        }
    }



    @AfterClass
    public void tearDown() {
        if (getDriver() != null) {
            getDriver().quit();
        }
    }

}
