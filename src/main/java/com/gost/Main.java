package com.gost;

import com.google.gson.Gson;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphGrpc.DgraphStub;
import io.dgraph.DgraphProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;


public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 链接地址
        ManagedChannel channel = ManagedChannelBuilder.forAddress("192.168.31.106", 9080).usePlaintext().build();
        DgraphStub stub = DgraphGrpc.newStub(channel);
        DgraphClient dgraphClient = new DgraphClient(stub);

        // Query
        String query =
                "query all($a: string){\n" + "all(func: eq(name, $a)) {\n" + "    name\n" + "  }\n" + "}";
        Map<String, String> vars = Collections.singletonMap("$a", "Alice");
        DgraphProto.Response res = dgraphClient.newTransaction().queryWithVars(query, vars);

        Gson gson = new Gson(); // For JSON encode/decode
        // Deserialize
        APP.People ppl = gson.fromJson(res.getJson().toStringUtf8(), APP.People.class);

        LOGGER.warn("people found: {}", ppl.all.size());

        ppl.all.forEach(person -> LOGGER.warn("people name: {}",person.name));

    }

}
