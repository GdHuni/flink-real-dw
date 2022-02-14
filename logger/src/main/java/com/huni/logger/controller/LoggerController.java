package com.huni.logger.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author huni
 * @Classname LoggerController
 * @Description 日志控制层
 * @Date 2022/2/14 11:14
 */
@Controller
@RequestMapping("/log")
@Slf4j
public class LoggerController {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @RequestMapping("test")
    @ResponseBody
    public String test(){
        System.out.println("sucess");
        return "sucess";
    }

    @RequestMapping("applog")
    @ResponseBody
    public String getAppLog(@RequestParam("param") String jsonStr){
        //落盘
        log.info(jsonStr);
        //写入kafka
        kafkaTemplate.send("ods_base_log", jsonStr);
        return "sucess";
    }

}
