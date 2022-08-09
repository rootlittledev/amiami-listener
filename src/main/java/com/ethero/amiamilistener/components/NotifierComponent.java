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
import org.jsoup.nodes.Node;
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
import java.util.List;
import java.util.Optional;

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

        driver = new ChromeDriver(chromeOptions);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(50));

        FileInputStream serviceAccount =
                new FileInputStream("amiami-bot-firebase-adminsdk-adddj-8be8fcf8ce.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);

    }

    @Scheduled(fixedRate = 60000)
    public void checkFigure() throws FirebaseMessagingException {
        Elements elements = getFigureElements();

        String figurePath = getPath(elements);

        log.debug("Figures present: {}", elements.size());

        if (elements.size() > currentCount) {
            log.info("New figure");
            sendMessage(figurePath);
        } else {
            log.info("No figure updates");
        }
    }

    private String getPath(Elements elements) {
        List<Node> parentNodes = elements.stream().map(element -> element.childNode(0).childNodes()
                        .stream().filter(el -> el.attr("class").equals("newly-added-items__item__tag-list"))
                        .toList()).flatMap(List::stream)
                .toList();
        List<Node> childNodes = parentNodes.stream().map(Node::childNodes).flatMap(List::stream).toList();
        Optional<Node> figurePath = childNodes.stream().flatMap(el -> el.childNodes().stream()).filter(el -> el.attr("#text").equals("Pre-owned") && !el.parent().attr("style").equals("display: none;")).findFirst();

        if (figurePath.isPresent()) {
            String path = figurePath.get().parent().parent().parent().attr("href");
            log.debug("Figure url: {}", path);
            return "https://www.amiami.com" + path;
        } else {
            log.debug("Couldn't get figure url");
            return "https://www.amiami.com/eng/search/list/?s_keywords=le%20malin";
        }
    }

    private Elements getFigureElements() {
        driver.get(url);

        Element page = Jsoup.parse(driver.getPageSource());

        return page.getElementsByClass("newly-added-items__item nomore");
    }

    private void sendMessage(String figurePath) throws FirebaseMessagingException {

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle("Figure update")
                        .setBody("It's here")
                        .setImage("https://i.imgur.com/RK5ydEW.png")
                        .build())
                .putData("url", figurePath)
                .putData("image", "https://c.tenor.com/M4ORXivVGfIAAAAd/azur-lane-le-malin.gif")
                .setTopic(topic)
                .build();

        log.debug("Sending update message");
        String response = FirebaseMessaging.getInstance().send(message);
        log.debug("Message send with response: {}", response);
    }
}
