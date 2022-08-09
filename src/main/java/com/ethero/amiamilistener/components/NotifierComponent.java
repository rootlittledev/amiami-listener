package com.ethero.amiamilistener.components;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Calendar;

@Component
@Slf4j
public class NotifierComponent {

    private WebDriver driver;

    @Value("${listener.url}")
    private String url;

    @Value("${listener.count.current}")
    private Integer currentCount;

    @Value("${listener.publisher.topic}")
    private String topic;

    @PostConstruct
    private void init() throws IOException {

        System.setProperty("webdriver.chrome.driver", "src/main/resources/drivers/chromedriver.exe");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        chromeOptions.setPageLoadTimeout(Duration.ofSeconds(40));

        driver = new ChromeDriver(chromeOptions);

        FileInputStream serviceAccount =
                new FileInputStream("amiami-bot-firebase-adminsdk-adddj-8be8fcf8ce.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);

    }

    @Scheduled(fixedRate = 20000)
    public void checkFigure() throws FirebaseMessagingException {
        Elements elements = getFigureElements();

        if (elements.size() > currentCount) {
            log.info("New figure");
            sendMessage();
        } else {
            log.info("No figure updates");
        }
    }

    private Elements getFigureElements() {
        driver.get(url);

        Element page = Jsoup.parse(driver.getPageSource());

        return page.getElementsByClass("newly-added-items__item nomore");
    }

    private void sendMessage() throws FirebaseMessagingException {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle("Figure update")
                        .setBody("It's here")
                        .setImage("https://i.imgur.com/RK5ydEW.png")
                        .build())
                .putData("time", Calendar.getInstance().getTime().toString())
                .setTopic(topic)
                .build();

        log.debug("Sending update message");
        String response = FirebaseMessaging.getInstance().send(message);
        log.debug("Message send with response: {}", response);
    }
}
