package com.huni.app.dwd;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huni.utils.KafkaConnectUtil;
import io.debezium.data.Json;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.io.Serializable;

/**
 * @author huni
 * @Classname LogDBApp
 * @Description 业务数据库dwd层
 * @Date 2022/2/16 15:41
 */
public class LogDbApp {
    public static void main(String[] args) throws Exception {
        //1.获取flink运行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //1.1 设置CK&状态后端
        //env.setStateBackend(new FsStateBackend("hdfs://hadoop102:8020/gmall-flink-210325/ck"));
        //env.enableCheckpointing(5000L);
        //env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        //env.getCheckpointConfig().setCheckpointTimeout(10000L);
        //env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);
        //env.getCheckpointConfig().setMinPauseBetweenCheckpoints(3000);

        //2.获取kafka数据
        String topic = "";
        String groupId = "";
        DataStreamSource<String> dataStreamSource = env.addSource(KafkaConnectUtil.getFlinkKafkaConsumer(topic, groupId));
        //3.测流输出脏数据
        OutputTag outputTag = new OutputTag<String>("Dirty"){};
        SingleOutputStreamOperator<JSONObject> process = dataStreamSource.process(new ProcessFunction<String, JSONObject>() {
            @Override
            public void processElement(String value, Context ctx, Collector<JSONObject> out) throws Exception {
                try {
                    JSONObject json = JSONObject.parseObject(value);
                    out.collect(json);
                } catch (Exception e) {
                    ctx.output(outputTag, value);
                }
            }
        });
        //打印脏数据
        process.getSideOutput(outputTag).print();
        //4.新老用户校验 状态编程
        SingleOutputStreamOperator<JSONObject> jsonObjWithNewFlagDS = process.keyBy(json -> json.getJSONObject("common").getString("mid")).map(
                new RichMapFunction<JSONObject, JSONObject>() {
                    //制作状态
                    private ValueState<String> valueState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        valueState = getRuntimeContext().getState(new ValueStateDescriptor<String>("value-state", String.class));
                    }

                    @Override
                    public JSONObject map(JSONObject jsonObject) throws Exception {
                        //获取数据中的"is_new"标记
                        String isNew = jsonObject.getJSONObject("common").getString("is_new");
                        //判断是否为新用户，是的话在看以前的数据是否标记过了为老用户了，
                        if ("1".equals(isNew)) {
                            if (valueState.value() == null) {
                                valueState.update("1");
                            } else {
                                jsonObject.getJSONObject("common").put("is_new", "0");
                            }
                        }
                        return jsonObject;
                    }
                });
        //TODO 5.分流  侧输出流  页面：主流  启动：侧输出流  曝光：侧输出流
        OutputTag startTag = new OutputTag("startTag"){};
        OutputTag displayTag = new OutputTag("displayTag"){};
        SingleOutputStreamOperator<String> pageDS = jsonObjWithNewFlagDS.process(new ProcessFunction<JSONObject, String>() {
            @Override
            public void processElement(JSONObject value, Context ctx, Collector<String> out) throws Exception {
                //获取启动日志字段
                String start = value.getString("start");
                if (start != null && start.length() > 0) {
                    ctx.output(startTag,value);
                }else{
                    //将数据写入页面日志主流
                    out.collect(value.toJSONString());
                    //取出数据中的曝光数据
                    JSONArray displays = value.getJSONArray("displays");
                    if (displays != null && displays.size() > 0) {

                        //获取页面ID
                        String pageId = value.getJSONObject("page").getString("page_id");

                        for (int i = 0; i < displays.size(); i++) {
                            JSONObject display = displays.getJSONObject(i);

                            //添加页面id
                            display.put("page_id", pageId);

                            //将输出写出到曝光侧输出流
                            ctx.output(displayTag, display.toJSONString());
                        }
                    }
                }
            }
        });

        //TODO 6.提取侧输出流
        DataStream<String> startDS = pageDS.getSideOutput(startTag);
        DataStream<String> displayDS = pageDS.getSideOutput(displayTag);


        startDS.addSink(KafkaConnectUtil.getFlinkKafkaProducer("dwd_start_log"));
        pageDS.addSink(KafkaConnectUtil.getFlinkKafkaProducer("dwd_page_log"));
        displayDS.addSink(KafkaConnectUtil.getFlinkKafkaProducer("dwd_display_log"));

        //TODO 8.启动任务
        env.execute("BaseLogApp");
    }

}
