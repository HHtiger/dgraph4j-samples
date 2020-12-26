package com.gost;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphGrpc.DgraphStub;
import io.dgraph.DgraphProto;
import io.dgraph.DgraphProto.Operation;
import io.dgraph.DgraphProto.Response;
import io.dgraph.Transaction;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class APP {
    private static final String TEST_HOSTNAME = "192.168.31.106";
    private static final int TEST_PORT = 9080;

    private static DgraphClient createDgraphClient(boolean withAuthHeader) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(TEST_HOSTNAME, TEST_PORT).usePlaintext().build();
        DgraphStub stub = DgraphGrpc.newStub(channel);

        if (withAuthHeader) {
            Metadata metadata = new Metadata();
            metadata.put(
                    Metadata.Key.of("auth-token", Metadata.ASCII_STRING_MARSHALLER), "the-auth-token-value");
            stub = MetadataUtils.attachHeaders(stub, metadata);
        }

        return new DgraphClient(stub);
    }

    public static void main(final String[] args) {
        DgraphClient dgraphClient = createDgraphClient(false);

        // Initialize
        dgraphClient.alter(Operation.newBuilder().setDropAll(true).build());

        // Set schema
        String schema = "name: string @index(exact) .";
        Operation operation = Operation.newBuilder().setSchema(schema).build();
        dgraphClient.alter(operation);

        Gson gson = new Gson(); // For JSON encode/decode
        Transaction txn = dgraphClient.newTransaction();
    try {
      // Create data
      Person p = new Person();
      p.name = "Alice";

      // Serialize it
      String json = gson.toJson(p);

      // Run mutation
      DgraphProto.Mutation mutation =
          DgraphProto.Mutation.newBuilder().setSetJson(ByteString.copyFromUtf8(json.toString())).build();
      txn.mutate(mutation);
      txn.commit();

    } finally {
      txn.discard();
    }
        // Query
        String query =
                "query all($a: string){\n" + "all(func: eq(name, $a)) {\n" + "    name\n" + "  }\n" + "}";
        Map<String, String> vars = Collections.singletonMap("$a", "Alice");
        Response res = dgraphClient.newTransaction().queryWithVars(query, vars);

        // Deserialize
        People ppl = gson.fromJson(res.getJson().toStringUtf8(), People.class);

        // Print results
        System.out.printf("people found: %d\n", ppl.all.size());
        ppl.all.forEach(person -> System.out.println(person.name));
    }

    static class Person {
        String name;

        Person() {}
    }

    static class People {
        List<Person> all;

        People() {}
    }
}
