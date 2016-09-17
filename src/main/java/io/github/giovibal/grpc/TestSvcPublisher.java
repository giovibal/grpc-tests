package io.github.giovibal.grpc;

import com.google.common.util.concurrent.SettableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by giovibal on 10/07/16.
 */
public class TestSvcPublisher {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 3000)
                .usePlaintext(true)
                .build();

//        TestSvcGrpc.TestSvcBlockingStub blockingStub = TestSvcGrpc.newBlockingStub(channel);

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
        StreamObserver<PublishMessage> requestObserver = asyncStub.publish(responseObserver);
        try {
            // Send numPoints points randomly selected from the features list.
            for (int i = 0; i < 10; ++i) {
                PublishMessage pm = PublishMessage.newBuilder()
                        .setId(i)
                        .setTopic("test/topic")
                        .setPayload("una prova "+ i)
                        .build();
                requestObserver.onNext(pm);
                Thread.sleep(500);
                if (finishLatch.getCount() == 0) {
                    // RPC completed or errored before we finished sending.
                    // Sending further requests won't error, but they will just be thrown away.
                    return;
                }
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // Receiving happens asynchronously
        finishLatch.await(1, TimeUnit.MINUTES);
    }
}
