package com.ethero.amiamilistener.components;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

@Component
@Slf4j
public class NotifierComponent {

    private WebDriver driver;
    private final String TOPIC = "figure_update";
    private final int CURRENT_ELEMENTS = 3;


    @PostConstruct
    private void init() throws IOException {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");

        driver = new ChromeDriver(chromeOptions);

        FileInputStream serviceAccount =
                new FileInputStream("amiami-bot-firebase-adminsdk-adddj-8be8fcf8ce.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);

    }

    @Scheduled(fixedRate = 20000)
    public void checkFigure() throws FirebaseMessagingException {
        driver.get("https://www.amiami.com/eng/search/list/?s_keywords=le%20malin");

        Element page = Jsoup.parse(driver.getPageSource());

        Elements elements = page.getElementsByClass("newly-added-items__item nomore");

        if (elements.size() > CURRENT_ELEMENTS) {
            log.info("New figure");
            Message message = Message.builder()
                    .putData("time", Calendar.getInstance().getTime().toString())
                    .setTopic(TOPIC)
                    .build();

            log.debug("Sending update message");
            String response = FirebaseMessaging.getInstance().send(message);
            log.debug("Message send with response: {}", response);
        } else {
            log.info("No figure updates");
        }
    }
}
