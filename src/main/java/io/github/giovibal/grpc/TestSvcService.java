package io.github.giovibal.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by giovibal on 10/07/16.
 */
public class TestSvcService implements TestSvcGrpc.TestSvc {

    public void test1(TestReq request, StreamObserver<TestRes> responseObserver) {
        String msg = request.getMsg();
        System.out.println(msg);

        responseObserver.onNext(TestRes.newBuilder()
                .setResp("<<<<RESP 1>>> "+ msg)
                .build());

        responseObserver.onCompleted();
    }

    public void test2(TestReq request, StreamObserver<TestRes> responseObserver) {
        String msg = request.getMsg();
        System.out.println(msg);

        for(int i=1 ; i<=1000; i++) {
            responseObserver.onNext(TestRes.newBuilder()
                    .setResp("<<<<RESP " + i + ">>> " + msg)
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<PublishMessage> publish(StreamObserver<PublishAckMessage> responseObserver) {
        return new StreamObserver<PublishMessage>() {
            @Override
            public void onNext(PublishMessage publishMessage) {
                addMessage(publishMessage);
                PublishAckMessage publishAckMessage = PublishAckMessage.newBuilder()
                        .setId(publishMessage.getId())
                        .build();
                responseObserver.onNext(publishAckMessage);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void subscribe(SubscribeMessage sub, StreamObserver<PublishMessage> responseObserver) {
        String topic = sub.getTopic();
        while (true) {
            PublishMessage pm = getNextMessage(topic);
            if(pm != null) {
                responseObserver.onNext(pm);
            }
        }
    }

    private LinkedHashMap<String, Queue<PublishMessage>> bus = new LinkedHashMap<>();
    private void addMessage(PublishMessage m) {
        if(bus.containsKey(m.getTopic())) {
            Queue<PublishMessage> messages = bus.get(m.getTopic());
            if(messages == null) {
                messages = new ConcurrentLinkedDeque<>();
            }
            messages.add(m);
            bus.put(m.getTopic(), messages);
        }
        else {
            Queue<PublishMessage> messages = new ConcurrentLinkedDeque<>();
            messages.add(m);
            bus.put(m.getTopic(), messages);
        }
    }

    private PublishMessage getNextMessage(String topic) {
        if(bus.containsKey(topic)) {
            return bus.get(topic).poll();
        }
        return null;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerServiceDefinition ssd = TestSvcGrpc.bindService(new TestSvcService());
        final Server server = ServerBuilder
                .forPort(3000)
                .addService(ssd)
                .build();
        server.start();
        System.out.println("Server started: listen on port: 3000");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                server.shutdown();
                System.err.println("*** server shut down");
            }
        });

        server.awaitTermination();
    }
}
