package com.litongjava.maxkb.service.spider;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.searxng.SearxngResult;
import com.litongjava.searxng.SearxngSearchClient;
import com.litongjava.searxng.SearxngSearchParam;
import com.litongjava.searxng.SearxngSearchResponse;

public class SearxngSearchService {

  public RespBodyVo search(String endpoint, SearxngSearchParam param, Boolean fetch, Integer limit) {
    SearxngSearchResponse searchResponse = SearxngSearchClient.search(endpoint, param);
    List<SearxngResult> results = searchResponse.getResults();
    List<WebPageContent> pages = new ArrayList<>();
    for (SearxngResult searxngResult : results) {
      String title = searxngResult.getTitle();
      String url = searxngResult.getUrl();
      String content = searxngResult.getContent();
      pages.add(new WebPageContent(title, url, content));
    }
    if (fetch != null && fetch) {
      if (limit == null) {
        pages = Aop.get(PlaywrightService.class).spiderAsync(pages);
      } else {
        pages = Aop.get(AiRankerService.class).filter(pages, param.getQ(), limit);
        pages = Aop.get(PlaywrightService.class).spiderAsync(pages);
      }
    }
    return RespBodyVo.ok(pages);
  }
}
