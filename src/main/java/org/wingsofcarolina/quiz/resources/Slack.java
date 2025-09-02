package org.wingsofcarolina.quiz.resources;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.QuizConfiguration;
import org.wingsofcarolina.quiz.domain.User;

public class Slack {

  private static final Logger LOG = LoggerFactory.getLogger(Slack.class);

  private static Slack instance = null;

  // WCFC #quiz channel : https://hooks.slack.com/services/REDACTED
  // WCFC #notification : https://hooks.slack.com/services/REDACTED
  // Planez.co #notification channel : https://hooks.slack.com/services/REDACTED
  private String URL = null;

  private QuizConfiguration config;

  public Slack(QuizConfiguration config) {
    // TODO Eventually make this configurable, but for now always go to Planez.co
    this.URL =
      "https://hooks.slack.com/services/REDACTED"; // WCFC #notification
    Slack.instance = this;
    this.config = config;
  }

  public static Slack instance() {
    return Slack.instance;
  }

  public void sendMessage(String message) {
    //LOG.info("Sending : QUIZ: {}", message);
    if (config.getMode().contentEquals("PROD")) {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(URL);
      String json = "{\"text\":\"QUIZ: " + message + "\"}";
      HttpEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
      httpPost.setEntity(stringEntity);
      try {
        CloseableHttpResponse response = httpclient.execute(httpPost);
        if (response.getStatusLine().getStatusCode() != 200) {
          LOG.error("Failed to successfully send message to Slack");
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public void sendFeedback(User user, Long questionId, String feedback) {
    //LOG.info("Sending : QUIZ: Feedback : {} : {} : {}", user.getName(), questionId, feedback);
    if (config.getMode().contentEquals("PROD")) {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(URL);
      String json =
        "{\"text\":\"QUIZ: Feedback : " +
        user.getName() +
        " : " +
        questionId +
        " : " +
        feedback +
        "\"}";
      HttpEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
      httpPost.setEntity(stringEntity);
      try {
        CloseableHttpResponse response = httpclient.execute(httpPost);
        if (response.getStatusLine().getStatusCode() != 200) {
          LOG.error("Failed to successfully send message to Slack");
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
