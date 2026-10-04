package nexus.io.maxkb.service.kb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 函数库的存取规则：参数转换、上游遗留的字段写法、初始化参数裁剪和保存前校验。
 */
public class PythonFunctionServiceTest {

  @Test
  public void parameterTypesAreConverted() {
    assertEquals(12L, PythonFunctionService.convert("12", "int"));
    assertEquals(1.5d, PythonFunctionService.convert("1.5", "float"));
    assertEquals("text", PythonFunctionService.convert("text", "string"));
    assertEquals("1", ((JSONObject) PythonFunctionService.convert("{\"a\":1}", "dict")).getString("a"));
    assertEquals(2, ((JSONArray) PythonFunctionService.convert("[1,2]", "array")).size());
    assertNull(PythonFunctionService.convert(null, "int"));
  }

  /** 上游模板把输入字段列表存成了 JSON 字符串，读出来必须是数组，否则界面渲染不出参数。 */
  @Test
  public void stringifiedFieldListIsUnwrapped() {
    JSONArray fields = JSON.parseArray("[{\"name\":\"query\",\"type\":\"string\"}]");
    JSONArray wrapped = JSONArray.of(JSON.toJSONString(fields.get(0)));
    JSONArray normalized = PythonFunctionService.normalizeFields(wrapped);
    assertEquals(1, normalized.size());
    assertEquals("query", ((JSONObject) normalized.get(0)).getString("name"));
    assertEquals(0, PythonFunctionService.normalizeFields(null).size());
  }

  /** 初始化字段被删掉后，库里的旧值不能再传给 Python。 */
  @Test
  public void initParamsFollowTheInitFieldList() {
    JSONObject data = JSONObject.of("init_params", JSONObject.of("apikey", "secret", "gone", "old"));
    String fields = "[{\"field\":\"apikey\",\"input_type\":\"PasswordInput\"}]";
    JSONObject params = PythonFunctionService.normalizeInitParams(data, fields);
    assertEquals(1, params.size());
    assertEquals("secret", params.getString("apikey"));
  }

  @Test
  public void saveInputIsValidated() {
    assertNull(PythonFunctionService.validate(JSONObject.of("name", "add", "code", "def add(a, b):\n    return a + b",
        "input_field_list", JSON.parseArray("[{\"name\":\"a\",\"type\":\"int\",\"source\":\"custom\"}]"))));
    assertTrue(PythonFunctionService.validate(JSONObject.of("name", " ", "code", "pass")).contains("名称"));
    assertTrue(PythonFunctionService.validate(JSONObject.of("name", "add", "code", " ")).contains("代码"));
    assertTrue(PythonFunctionService.validate(JSONObject.of("name", "add", "code", "pass", "input_field_list",
        JSON.parseArray("[{\"name\":\"a\",\"type\":\"date\"}]"))).contains("参数类型"));
    assertTrue(PythonFunctionService.validate(JSONObject.of("name", "add", "code", "pass", "input_field_list",
        JSON.parseArray("[{\"name\":\"a\",\"type\":\"int\",\"source\":\"unknown\"}]"))).contains("参数来源"));
  }

  /** 密码类初始化参数在详情里打码，只保留首尾片段。 */
  @Test
  public void secretsAreMasked() {
    assertEquals("12********cdef", PythonFunctionService.mask("1234567890abcdef"));
    assertEquals("ab********", PythonFunctionService.mask("abc"));
  }
}
