package nexus.io.maxkb.service.kb;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import nexus.io.maxkb.vo.CredentialVo;
import nexus.io.maxkb.vo.ModelVo;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelCatalogTest {
  @Test public void customRelayModelIdReachesActualRequest() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicReference<JSONObject> captured = new AtomicReference<>();
    server.createContext("/v1/chat/completions", exchange -> {
      captured.set(JSON.parseObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
      byte[] response = "{\"id\":\"test\",\"model\":\"vendor/custom:latest\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"OK\"},\"finish_reason\":\"stop\"}]}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    });
    server.start();
    try {
      CredentialVo credential = new CredentialVo();
      credential.setApi_base("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
      credential.setApi_key("test-key");
      MaxKbModelService.validateModel(new ModelVo().setModel_type("LLM").setModel_name("vendor/custom:latest").setCredential(credential));
      assertEquals("vendor/custom:latest", captured.get().getString("model"));
      assertFalse(captured.get().getBooleanValue("stream"));
    } finally {
      server.stop(0);
    }
  }

  @Test public void customIdsAreNotRestrictedToTheCatalog() {
    assertEquals("vendor/new-model:free", ModelCatalogService.modelId(" vendor/new-model:free "));
    assertEquals("https://example.com/v1", ModelCatalogService.baseUrl("https://example.com/v1/"));
    for (String invalid : new String[] {"file:///secret", "https://user:key@example.com/v1", "https://example.com?key=secret"}) {
      try {
        ModelCatalogService.baseUrl(invalid);
        fail("must reject unsafe credential URL");
      } catch (IllegalArgumentException expected) {
        assertNotNull(expected);
      }
    }
  }

  @Test public void otherPlatformsNeverInheritGiteeCredentials() {
    CredentialVo credential = ModelCatalogService.credential(nexus.io.db.activerecord.Row.by("id", 9999L).set("credential", "{}"));
    assertNull(credential.getApi_key());
    assertNull(credential.getApi_base());
  }

  @Test public void embeddingValidationUsesCustomIdAndRejectsWrongDimensions() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicReference<JSONObject> captured = new AtomicReference<>();
    server.createContext("/v1/embeddings", exchange -> {
      captured.set(JSON.parseObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
      byte[] body = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      CredentialVo credential = new CredentialVo();
      credential.setApi_base("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
      credential.setApi_key("test-key");
      try {
        MaxKbModelService.validateModel(new ModelVo().setModel_type("EMBEDDING").setModel_name("vendor/custom-embedding").setCredential(credential));
        fail("Wrong vector space must be rejected");
      } catch (IllegalArgumentException expected) {
        assertTrue(expected.getMessage().contains("1024"));
      }
      assertEquals("vendor/custom-embedding", captured.get().getString("model"));
      assertEquals(1024, captured.get().getIntValue("dimensions"));
    } finally {
      server.stop(0);
    }
  }
}
