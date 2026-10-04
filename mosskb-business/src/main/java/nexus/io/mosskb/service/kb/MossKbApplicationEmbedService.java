package nexus.io.mosskb.service.kb;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.tio.utils.hutool.StrUtil;

/**
 * 浮窗嵌入：把对话页面做成一段脚本，第三方页面用 script 标签引入后得到右下角的悬浮入口。
 *
 * 脚本正文是静态的，与页面相关的取值（协议、主机、令牌、白名单、图标地址）以一段 JSON 注入，
 * 由浏览器执行；服务端只做出参校验与取值，不参与页面渲染。
 */
public class MossKbApplicationEmbedService {

  /** 允许的协议：只有 http 与 https 能拼进脚本里的地址。 */
  private static final List<String> SUPPORTED_PROTOCOLS = List.of("http", "https");

  /** 主机名（含可选端口）。取值来自查询串，限制字符集后拼进地址，避免注入。 */
  private static final Pattern HOST = Pattern.compile("[A-Za-z0-9._-]{1,253}(:[0-9]{1,5})?");

  /** 浮窗图标：随前端一起发布的图片，与前端同源。 */
  private static final String FLOAT_ICON_PATH = "/ui/MossKB.png";

  /** 后续对话要带上的入参：编排里声明为接口入参的变量，以及提问人标识。 */
  private static final String ASKER = "asker";

  private static final String FIND_ACCESS_TOKEN = """
      select application_id, is_active, white_active, white_list
      from moss_kb_application_access_token
      where access_token = ?
        and deleted = 0
      """;

  private static final String FIND_WORK_FLOW = """
      select work_flow
      from moss_kb_application
      where id = ?
      """;

  /**
   * 渲染浮窗脚本。链接被停用、参数不合法时同样返回脚本，由脚本在控制台给出原因并保持页面不动。
   */
  public String script(String protocol, String host, Long token, Map<String, Object> params) {
    String scheme = StrUtil.isBlank(protocol) ? null : protocol.toLowerCase();
    boolean valid = SUPPORTED_PROTOCOLS.contains(scheme) && StrUtil.isNotBlank(host) && HOST.matcher(host).matches()
        && token != null;
    Row accessToken = valid ? Db.findFirst(FIND_ACCESS_TOKEN, token) : null;
    if (accessToken == null || !Boolean.TRUE.equals(accessToken.getBoolean("is_active"))) {
      JSONObject denied = new JSONObject();
      denied.put("isAuth", false);
      return render(denied);
    }

    Long applicationId = accessToken.getLong("application_id");
    JSONObject config = new JSONObject();
    config.put("isAuth", true);
    config.put("protocol", scheme);
    config.put("host", host);
    config.put("token", token.toString());
    config.put("query", apiInputQuery(applicationId, params));
    config.put("floatIcon", scheme + "://" + host + FLOAT_ICON_PATH);
    config.put("whiteActive", Boolean.TRUE.equals(accessToken.getBoolean("white_active")));
    config.put("whiteList", whiteList(accessToken.getStringArray("white_list")));
    return render(config);
  }

  /** 配置以 JSON 注入，脚本正文保持静态，取值不参与脚本拼接。 */
  private String render(JSONObject config) {
    return "window.__MOSSKB_EMBED_CONFIG__ = " + config.toJSONString() + ";\n" + SCRIPT;
  }

  /** 白名单去掉空值，保持配置时的顺序。 */
  private List<String> whiteList(String[] values) {
    List<String> list = new ArrayList<>();
    if (values != null) {
      for (String value : values) {
        if (StrUtil.isNotBlank(value)) {
          list.add(value);
        }
      }
    }
    return list;
  }

  /**
   * 拼出附加到对话地址后面的入参：编排里声明为接口入参的变量按声明取值，另外带上提问人标识。
   * 只透传应用自己声明过的变量，避免任意参数被带进对话页。
   */
  private String apiInputQuery(Long applicationId, Map<String, Object> params) {
    if (params == null || params.isEmpty()) {
      return "";
    }
    Set<String> names = new LinkedHashSet<>(apiInputVariables(applicationId));
    names.add(ASKER);
    StringBuilder query = new StringBuilder();
    for (String name : names) {
      append(query, name, params.get(name));
    }
    return query.toString();
  }

  private void append(StringBuilder query, String name, Object value) {
    if (value == null) {
      return;
    }
    String text = value.toString();
    if (StrUtil.isBlank(text)) {
      return;
    }
    query.append('&').append(name).append('=').append(URLEncoder.encode(text, StandardCharsets.UTF_8));
  }

  /** 编排基础节点上声明的接口入参变量。 */
  private List<String> apiInputVariables(Long applicationId) {
    List<String> variables = new ArrayList<>();
    String workFlow = Db.queryStr(FIND_WORK_FLOW, applicationId);
    if (StrUtil.isBlank(workFlow)) {
      return variables;
    }
    JSONObject json = null;
    try {
      json = JSON.parseObject(workFlow);
    } catch (RuntimeException e) {
      return variables;
    }
    JSONArray nodes = json == null ? null : json.getJSONArray("nodes");
    if (nodes == null) {
      return variables;
    }
    for (int i = 0; i < nodes.size(); i++) {
      JSONObject node = nodes.getJSONObject(i);
      if (node == null || !"base-node".equals(node.getString("id"))) {
        continue;
      }
      collectInputVariables(node.getJSONObject("properties"), variables);
    }
    return variables;
  }

  private void collectInputVariables(JSONObject properties, List<String> variables) {
    if (properties == null) {
      return;
    }
    JSONArray fields = properties.getJSONArray("api_input_field_list");
    boolean declaredByApi = fields != null;
    if (!declaredByApi) {
      fields = properties.getJSONArray("input_field_list");
    }
    if (fields == null) {
      return;
    }
    for (int i = 0; i < fields.size(); i++) {
      JSONObject field = fields.getJSONObject(i);
      if (field == null) {
        continue;
      }
      if (!declaredByApi && !"api_input".equals(field.getString("assignment_method"))) {
        continue;
      }
      String variable = field.getString("variable");
      if (StrUtil.isNotBlank(variable) && !variables.contains(variable)) {
        variables.add(variable);
      }
    }
  }

  /**
   * 脚本正文。所有取值都来自上面注入的配置对象，正文本身固定不变。
   */
  private static final String SCRIPT = """
      (function () {
        var config = window.__MOSSKB_EMBED_CONFIG__ || {};
        if (!config.isAuth) {
          console.error('MossKB: 公开访问链接不存在或已停用');
          return;
        }
        if (config.whiteActive && config.whiteList.indexOf(window.location.origin) < 0) {
          console.error('MossKB: 当前页面来源不在白名单内 ' + window.location.origin);
          return;
        }

        var GUIDE_KEY = 'mosskbGuideTip';
        var rootId = 'mosskb-' + Math.random().toString(36).slice(2, 10);
        var chatUrl = config.protocol + '://' + config.host + '/ui/#/chat/' + config.token + '?mode=embed'
            + config.query;

        var ICON_ENLARGE = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"'
            + ' fill="#646a73"><path d="M7.15209 11.5968C7.31481 11.4341 7.57862 11.4341 7.74134 11.5968L8.3306 12.186C8.49332'
            + ' 12.3487 8.49332 12.6126 8.3306 12.7753L4.99615 16.1086H7.3665C7.59662 16.1086 7.78316 16.2952 7.78316'
            + ' 16.5253V17.3586C7.78316 17.5887 7.59662 17.7753 7.3665 17.7753H3.05584C2.82572 17.7753 2.61738 17.682'
            + ' 2.46658 17.5312C2.31578 17.3804 2.2225 17.1721 2.2225 16.9419V12.6421C2.2225 12.412 2.40905 12.2255'
            + ' 2.63917 12.2255H3.4725C3.70262 12.2255 3.88917 12.412 3.88917 12.6421V14.8586L7.15209 11.5968ZM16.937'
            + ' 2.22217C17.1671 2.22217 17.3754 2.31544 17.5262 2.46625C17.677 2.61705 17.7703 2.82538 17.7703'
            + ' 3.0555V7.35531C17.7703 7.58543 17.5837 7.77198 17.3536 7.77198H16.5203C16.2902 7.77198 16.1036'
            + ' 7.58543 16.1036 7.35531V5.13888L12.8407 8.40068C12.678 8.5634 12.4142 8.5634 12.2515 8.40068L11.6622'
            + ' 7.81142C11.4995 7.64871 11.4995 7.38489 11.6622 7.22217L14.9966 3.88883H12.6263C12.3962 3.88883'
            + ' 12.2096 3.70229 12.2096 3.47217V2.63883C12.2096 2.40872 12.3962 2.22217 12.6263 2.22217H16.937Z"/></svg>';

        var ICON_RESTORE = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"'
            + ' fill="#646a73"><path d="M7.507 11.6645C7.73712 11.6645 7.94545 11.7578 8.09625 11.9086C8.24706 12.0594'
            + ' 8.34033 12.2677 8.34033 12.4978V16.7976C8.34033 17.0277 8.15378 17.2143 7.92366 17.2143H7.09033C6.86021'
            + ' 17.2143 6.67366 17.0277 6.67366 16.7976V14.5812L3.41075 17.843C3.24803 18.0057 2.98421 18.0057 2.82149'
            + ' 17.843L2.23224 17.2537C2.06952 17.091 2.06952 16.8272 2.23224 16.6645L5.56668 13.3311H3.19634C2.96622'
            + ' 13.3311 2.77967 13.1446 2.77967 12.9145V12.0811C2.77967 11.851 2.96622 11.6645 3.19634 11.6645H7.507ZM16.5991'
            + ' 2.1572C16.7619 1.99448 17.0257 1.99448 17.1884 2.1572L17.7777 2.74645C17.9404 2.90917 17.9404 3.17299'
            + ' 17.7777 3.33571L14.4432 6.66904H16.8136C17.0437 6.66904 17.2302 6.85559 17.2302 7.08571V7.91904C17.2302'
            + ' 8.14916 17.0437 8.33571 16.8136 8.33571H12.5029C12.2728 8.33571 12.0644 8.24243 11.9136 8.09163C11.7628'
            + ' 7.94082 11.6696 7.73249 11.6696 7.50237V3.20257C11.6696 2.97245 11.8561 2.7859 12.0862 2.7859H12.9196C13.1497'
            + ' 2.7859 13.3362 2.97245 13.3362 3.20257V5.419L16.5991 2.1572Z"/></svg>';

        var ICON_CLOSE = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"'
            + ' fill="#646a73"><path d="M9.95317 8.73169L15.5511 3.13376C15.7138 2.97104 15.9776 2.97104 16.1403'
            + ' 3.13376L16.7296 3.72301C16.8923 3.88573 16.8923 4.14955 16.7296 4.31227L11.1317 9.9102L16.7296'
            + ' 15.5081C16.8923 15.6708 16.8923 15.9347 16.7296 16.0974L16.1403 16.6866C15.9776 16.8494 15.7138'
            + ' 16.8494 15.5511 16.6866L9.95317 11.0887L4.35524 16.6866C4.19252 16.8494 3.9287 16.8494 3.76598'
            + ' 16.6866L3.17673 16.0974C3.01401 15.9347 3.01401 15.6708 3.17673 15.5081L8.77465 9.9102L3.17673'
            + ' 4.31227C3.01401 4.14955 3.01401 3.88573 3.17673 3.72301L3.76598 3.13376C3.9287 2.97104 4.19252'
            + ' 2.97104 4.35524 3.13376L9.95317 8.73169Z"/></svg>';

        var STYLES = [
          '.mosskb-float{position:fixed;right:30px;bottom:30px;width:64px;height:64px;border-radius:50%;overflow:hidden;'
              + 'cursor:pointer;z-index:9998;box-shadow:0 6px 16px rgba(31,35,41,.18);}',
          '.mosskb-float img{display:block;width:100%;height:100%;}',
          '.mosskb-window{display:none;position:fixed;right:16px;bottom:16px;width:450px;height:600px;'
              + 'max-width:calc(100vw - 32px);max-height:calc(100vh - 32px);border-radius:8px;overflow:hidden;'
              + 'background:#eff0f1;box-shadow:0 4px 8px rgba(31,35,41,.1);z-index:9999;}',
          '.mosskb-window.mosskb-open{display:block;}',
          '.mosskb-window.mosskb-enlarge{width:50%;height:100%;right:0;bottom:0;border-radius:0;}',
          '.mosskb-frame{display:block;width:100%;height:100%;border:none;}',
          '.mosskb-operate{position:absolute;top:18px;right:15px;display:flex;align-items:center;line-height:18px;}',
          '.mosskb-operate span{display:flex;margin-left:15px;cursor:pointer;}',
          '.mosskb-guide{position:fixed;right:110px;bottom:30px;width:260px;padding:20px 22px;border-radius:6px;'
              + 'background:#3370ff;color:#fff;font-size:14px;line-height:22px;z-index:9999;}',
          '.mosskb-guide strong{display:block;font-size:16px;font-weight:500;margin-bottom:6px;}',
          '.mosskb-guide button{margin-top:12px;border:none;border-radius:4px;background:#fff;color:#3370ff;'
              + 'padding:3px 12px;cursor:pointer;}',
          '.mosskb-guide .mosskb-guide-close{position:absolute;right:12px;top:10px;cursor:pointer;font-size:16px;}'
        ];

        function scoped(rules) {
          var text = '';
          for (var i = 0; i < rules.length; i++) {
            text += '#' + rootId + ' ' + rules[i];
          }
          return text;
        }

        function hideGuide(root) {
          var guide = root.querySelector('.mosskb-guide');
          if (guide) {
            root.removeChild(guide);
          }
          window.localStorage.setItem(GUIDE_KEY, '1');
        }

        function showGuide(root) {
          if (window.localStorage.getItem(GUIDE_KEY)) {
            return;
          }
          var guide = document.createElement('div');
          guide.className = 'mosskb-guide';
          guide.innerHTML = '<span class="mosskb-guide-close">x</span><strong>有问题随时问我</strong>'
              + '点右下角的图标就能开始对话。<button type="button">我知道了</button>';
          root.appendChild(guide);
          guide.querySelector('.mosskb-guide-close').onclick = function () {
            hideGuide(root);
          };
          guide.querySelector('button').onclick = function () {
            hideGuide(root);
          };
        }

        function bind(root) {
          var button = root.querySelector('.mosskb-float');
          var win = root.querySelector('.mosskb-window');
          var enlarge = root.querySelector('.mosskb-enlarge');
          var restore = root.querySelector('.mosskb-restore');
          var close = root.querySelector('.mosskb-close');
          restore.style.display = 'none';
          button.onclick = function () {
            win.classList.add('mosskb-open');
            button.style.display = 'none';
            hideGuide(root);
          };
          close.onclick = function () {
            win.classList.remove('mosskb-open');
            button.style.display = 'block';
          };
          enlarge.onclick = function () {
            win.classList.add('mosskb-enlarge');
            enlarge.style.display = 'none';
            restore.style.display = 'flex';
          };
          restore.onclick = function () {
            win.classList.remove('mosskb-enlarge');
            restore.style.display = 'none';
            enlarge.style.display = 'flex';
          };
        }

        function init() {
          var root = document.createElement('div');
          root.id = rootId;
          root.innerHTML = '<div class="mosskb-float"><img src="' + config.floatIcon + '" alt="MossKB"></div>'
              + '<div class="mosskb-window">'
              + '<iframe class="mosskb-frame" src="' + chatUrl + '" allow="microphone"></iframe>'
              + '<div class="mosskb-operate">'
              + '<span class="mosskb-enlarge" title="放大">' + ICON_ENLARGE + '</span>'
              + '<span class="mosskb-restore" title="还原">' + ICON_RESTORE + '</span>'
              + '<span class="mosskb-close" title="关闭">' + ICON_CLOSE + '</span>'
              + '</div></div>';
          var style = document.createElement('style');
          style.appendChild(document.createTextNode(scoped(STYLES)));
          root.appendChild(style);
          document.body.appendChild(root);
          bind(root);
          showGuide(root);
        }

        if (document.readyState === 'loading') {
          document.addEventListener('DOMContentLoaded', init);
        } else {
          init();
        }
      })();
      """;
}
