package org.example.concurrent.collection;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Language;
import com.aerospike.client.Value;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.ExecuteTask;
import com.aerospike.client.task.RegisterTask;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author wenbo.zhangw
 * @date 2023/1/9 10:03
 */
public class LuaTest {

    private static final AerospikeClient CLIENT = new AerospikeClient("10.58.12.66", 3000);

    private static final String NAMESPACE = "ns1";

    private static final String SET = "clear_bin_test";

    private static final String PACKAGE_NAME_LUA = "clear_bin.lua";
    private static final String PACKAGE_NAME = "clear_bin";

    private static final String FUNCTION_NAME = "clear";

    public static void main(String[] args) {
        registerUdf();

        Statement stmt = new Statement();
        stmt.setNamespace(NAMESPACE);
        stmt.setSetName(SET);
        stmt.setBinNames("foo", "bar");
        ExecuteTask task = CLIENT.execute(null, stmt, PACKAGE_NAME, FUNCTION_NAME, Value.get(Lists.newArrayList("foo", "bar")));

        task.waitTillComplete();

        System.out.println("lua execute success!");
    }

    private static void registerUdf() {
        try {
            String file = String.join("\n", Files.readAllLines(Paths.get("/Users/td/IdeaProjects/private/public-workspace/concurrent/src/main/resources/clear_bin.lua")));
            // remove old lua
            CLIENT.removeUdf(null, PACKAGE_NAME_LUA);
            System.out.println("lua remove success!");

            // register new lua
            RegisterTask task = CLIENT.registerUdfString(null, file, PACKAGE_NAME_LUA, Language.LUA);

            task.waitTillComplete();

            System.out.println("lua register success!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
