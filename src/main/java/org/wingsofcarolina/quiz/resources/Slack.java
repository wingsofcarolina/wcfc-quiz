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

  private String webhookUrl = null;

  private QuizConfiguration config;

  public Slack(QuizConfiguration config) {
    this.webhookUrl = config.getSlackWebhookUrl();
    Slack.instance = this;
    this.config = config;
  }

  public static Slack instance() {
    return Slack.instance;
  }

  public void sendMessage(String message) {
    //LOG.info("Sending : QUIZ: {}", message);
    if (
      config.getMode().contentEquals("PROD") &&
      webhookUrl != null &&
      !webhookUrl.trim().isEmpty()
    ) {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(webhookUrl);
      String json = "{\"text\":\"QUIZ: " + message + "\"}";
      HttpEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
      httpPost.setEntity(stringEntity);
      try {
        CloseableHttpResponse response = httpclient.execute(httpPost);
        if (response.getStatusLine().getStatusCode() != 200) {
          LOG.error("Failed to successfully send message to Slack");
        }
      } catch (IOException e) {
        LOG.error("IOException while sending message to Slack", e);
      }
    } else if (config.getMode().contentEquals("PROD")) {
      LOG.warn("Slack webhook URL not configured - message not sent: {}", message);
    }
  }

  public void sendFeedback(User user, Long questionId, String feedback) {
    //LOG.info("Sending : QUIZ: Feedback : {} : {} : {}", user.getName(), questionId, feedback);
    if (
      config.getMode().contentEquals("PROD") &&
      webhookUrl != null &&
      !webhookUrl.trim().isEmpty()
    ) {
      CloseableHttpClient httpclient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(webhookUrl);
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
        LOG.error("IOException while sending feedback to Slack", e);
      }
    } else if (config.getMode().contentEquals("PROD")) {
      LOG.warn(
        "Slack webhook URL not configured - feedback not sent: {} : {} : {}",
        user.getName(),
        questionId,
        feedback
      );
    }
  }
}
