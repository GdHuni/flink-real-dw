package com.huni.app.ods;

import com.alibaba.ververica.cdc.connectors.mysql.MySQLSource;
import com.alibaba.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.alibaba.ververica.cdc.debezium.DebeziumSourceFunction;
import com.huni.utils.KafkaConnectUtil;
import com.huni.utils.MyDebeziumDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author huni
 * @Classname FlinkCDC
 * @Description 通过flink获取binlog日志输出到kafka ods层
 * @Date 2022/2/16 11:54
 */
public class FlinkCDC {
    public static void main(String[] args) throws Exception {
        //1.获取环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        //1.1 设置CK&状态后端
        //env.setStateBackend(new FsStateBackend("hdfs://hadoop102:8020/gmall-flink-210325/ck"));
        //env.enableCheckpointing(5000L);
        //env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        //env.getCheckpointConfig().setCheckpointTimeout(10000L);
        //env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);
        //env.getCheckpointConfig().setMinPauseBetweenCheckpoints(3000);

        //2.通过flinkcdc构建sourcefunction

        DebeziumSourceFunction<String> mySqlSource = MySQLSource.<String>builder()
                .hostname("hadoop102")
                .port(3306)
                .username("root")
                .password("000000")
                .databaseList("gmall-210325-flink")
                .deserializer(new MyDebeziumDeserializationSchema())
                .startupOptions(StartupOptions.latest())
                .build();



        DataStreamSource<String> streamSource = env.addSource(mySqlSource);
        //3.打印数据并将数据写入Kafka
        streamSource.print();
        String sinkTopic = "ods_base_db";
        streamSource.addSink(KafkaConnectUtil.getFlinkKafkaProducer(sinkTopic));

        //4.启动任务
        env.execute("FlinkCDC");

    }
}
