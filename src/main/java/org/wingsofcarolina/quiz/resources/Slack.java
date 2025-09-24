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

  private String notificationUrl = null;
  private String slackApiBaseUrl;

  private QuizConfiguration config;

  public Slack(QuizConfiguration config) {
    this.config = config;
    this.slackApiBaseUrl = config.getSlackApiBaseUrl();

    // Build notification webhook URL from the configuration
    if (
      config.getSlackNotify() != null &&
      !config.getSlackNotify().isEmpty() &&
      !"none".equals(config.getSlackNotify())
    ) {
      if (config.getSlackNotify().startsWith("https://")) {
        notificationUrl = config.getSlackNotify();
      } else {
        // Assume it's in the token format (T/B/X) and convert to webhook URL
        String[] tokenParts = config.getSlackNotify().split("/");
        if (tokenParts.length == 3) {
          notificationUrl =
            slackApiBaseUrl +
            "/services/" +
            tokenParts[0] +
            "/" +
            tokenParts[1] +
            "/" +
            tokenParts[2];
        } else {
          throw new RuntimeException("Bad Slack notification token, shutting down!");
        }
      }
    } else {
      LOG.warn(
        "Slack notification channel is disabled (token set to 'none' or not configured)"
      );
      notificationUrl = null;
    }

    Slack.instance = this;
    LOG.info("Slack service initialized with API base URL: {}", slackApiBaseUrl);
  }

  public static Slack instance() {
    return Slack.instance;
  }

  public void sendMessage(String message) {
    //LOG.info("Sending : QUIZ: {}", message);
    if (config.getMode().contentEquals("PROD")) {
      if (notificationUrl != null && !notificationUrl.trim().isEmpty()) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(notificationUrl);
        String json = "{\"text\":\"QUIZ: " + escapeJson(message) + "\"}";
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
      } else {
        LOG.warn("Slack notification URL not configured - message not sent: {}", message);
      }
    }
  }

  public void sendFeedback(User user, Long questionId, String feedback) {
    //LOG.info("Sending : QUIZ: Feedback : {} : {} : {}", user.getName(), questionId, feedback);
    if (config.getMode().contentEquals("PROD")) {
      if (notificationUrl != null && !notificationUrl.trim().isEmpty()) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(notificationUrl);
        String json =
          "{\"text\":\"QUIZ: Feedback : " +
          escapeJson(user.getName()) +
          " : " +
          questionId +
          " : " +
          escapeJson(feedback) +
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
      } else {
        LOG.warn(
          "Slack notification URL not configured - feedback not sent: {} : {} : {}",
          user.getName(),
          questionId,
          feedback
        );
      }
    }
  }

  private String escapeJson(String text) {
    if (text == null) return "";
    return text
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t");
  }
}
