package com.litongjava.maxkb.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class MaxKbApplicationVo {
  private Long id;
  private String name;
  private String desc;
  private Long model_id;
  private Integer dialogue_number;
  private String prologue;
  private List<Long> dataset_id_list;
  private MaxKbDatasetSettingVo dataset_setting;
  private MaxKbModelSetting model_setting;
  private MaxKbModelParamsSetting model_params_setting;
  private Boolean problem_optimization;
  private String problem_optimization_prompt;
  private Long stt_model_id;
  private Long tts_model_id;
  private Boolean stt_model_enable;
  private Boolean tts_model_enable;
  private String tts_type;
  private String type;
}
