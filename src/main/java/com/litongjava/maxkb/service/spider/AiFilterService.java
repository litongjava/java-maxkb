package com.litongjava.maxkb.service.spider;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.api.GeminiService;
import com.litongjava.searxng.WebPageConteont;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.tag.TagUtils;

// @Slf4j
public class AiFilterService {
  public List<WebPageConteont> filter(List<WebPageConteont> pages, String question, Integer limit) {
    //3.选择
    Kv kv = Kv.by("limit", limit).set("quesiton", question).set("search_result", JsonUtils.toJson(pages));
    String fileName = "WebSearchSelectPrompt.txt";
    String prompt = PromptEngine.renderToString(fileName, kv);
    //log.info("WebSearchSelectPrompt:{}", prompt);

    String selectResultContent = Aop.get(GeminiService.class).generate(prompt);
    List<String> outputs = TagUtils.extractOutput(selectResultContent);
    String titleAndLinks = outputs.get(0);
    if ("not_found".equals(titleAndLinks)) {
      return null;
    }

    //4.send to client
    String[] split = titleAndLinks.split("\n");
    List<WebPageConteont> citationList = new ArrayList<>();
    for (int i = 0; i < split.length; i++) {
      String[] split2 = split[i].split("~~");
      citationList.add(new WebPageConteont(split2[0], split2[1]));
    }
    return citationList;
  }
}
