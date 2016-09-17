package io.github.giovibal.grpc;

import com.google.common.util.concurrent.SettableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.CallStreamObserver;

import java.util.concurrent.ExecutionException;

/**
 * Created by giovibal on 10/07/16.
 */
public class TestSvcClient {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 3000)
                .usePlaintext(true)
                .build();

        TestSvcGrpc.TestSvcBlockingStub blockingStub = TestSvcGrpc.newBlockingStub(channel);

        TestSvcGrpc.TestSvcStub asyncStub = TestSvcGrpc.newStub(channel);

        TestRes resp = blockingStub.test1(TestReq.newBuilder().setMsg("prova").build());
        System.out.println(resp);



        final SettableFuture<Void> finishFuture = SettableFuture.create();
        asyncStub.test2(TestReq.newBuilder().setMsg("prova").build(), new CallStreamObserver<TestRes>() {
            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setOnReadyHandler(Runnable runnable) {

            }

            @Override
            public void disableAutoInboundFlowControl() {

            }

            @Override
            public void request(int i) {

            }

            public void onNext(TestRes testRes) {
                System.out.println(testRes);
            }

            public void onError(Throwable throwable) {
                finishFuture.setException(throwable);
            }

            public void onCompleted() {
                System.out.println("completed.");
                finishFuture.set(null);
            }

            @Override
            public void setMessageCompression(boolean b) {

            }
        });

//        while (true) {
//
//        }
        finishFuture.get();
    }
}
