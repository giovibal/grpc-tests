package io.github.giovibal.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by giovibal on 10/07/16.
 */
public class TestSvcSubscriber {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 3000)
                .usePlaintext(true)
                .build();

        TestSvcGrpc.TestSvcStub asyncStub = TestSvcGrpc.newStub(channel);

        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<PublishAckMessage> responseObserver = new StreamObserver<PublishAckMessage>() {
            @Override
            public void onNext(PublishAckMessage publishAckMessage) {
                System.out.println(publishAckMessage);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };
        SubscribeMessage req = SubscribeMessage.newBuilder()
                .setTopic("test/topic")
                .build();
        asyncStub.subscribe(req, new StreamObserver<PublishMessage>() {
            @Override
            public void onNext(PublishMessage publishMessage) {
                System.out.println(publishMessage);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {

            }
        });

        // Receiving happens asynchronously
        finishLatch.await(1, TimeUnit.MINUTES);
    }
}
