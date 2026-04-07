package com.example;

import com.example.grpc.KvProto;
import com.example.grpc.KvServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolFactory;

import java.util.Arrays;
import java.util.List;

public class KvServiceImpl extends KvServiceGrpc.KvServiceImplBase {

    private final TarantoolBoxClient client;

    public KvServiceImpl() throws Exception {
        this.client = TarantoolFactory.box().build();
    }

    @Override
    public void count(KvProto.CountRequest req, StreamObserver<KvProto.CountResponse> obs) {
        try {
            var result = client.eval("return box.space.KV:len()").join();
            long count = ((Number) result.get().get(0)).longValue();


            obs.onNext(KvProto.CountResponse.newBuilder().setCount(count).build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void put(KvProto.PutRequest req, StreamObserver<KvProto.PutResponse> obs) {
        try {
            String key = req.getKey();
            byte[] value = req.hasValue() ? req.getValue().toByteArray() : null;

            client.space("KV")
                    .replace(Arrays.asList(key, value))
                    .join();

            obs.onNext(KvProto.PutResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void get(KvProto.GetRequest req, StreamObserver<KvProto.GetResponse> obs) {
        try {
            var result = client.space("KV").select(req.getKey()).join();
            var rows = result.get();

            if (rows.isEmpty()) {
                obs.onError(Status.NOT_FOUND
                        .withDescription("Key not found: " + req.getKey())
                        .asRuntimeException());
                return;
            }

            var tuple = (io.tarantool.mapping.Tuple) rows.get(0);
            var builder = KvProto.GetResponse.newBuilder();

            List<?> data = (List<?>) tuple.get();
            byte[] value = (byte[]) data.get(1);

            if (value != null) {
                builder.setValue(
                        com.google.protobuf.BytesValue.of(
                                com.google.protobuf.ByteString.copyFrom(value)
                        )
                );
            }

            obs.onNext(builder.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void delete(KvProto.DeleteRequest req, StreamObserver<KvProto.DeleteResponse> obs) {
        try {
            client.space("KV").delete(List.of(req.getKey())).join();
            obs.onNext(KvProto.DeleteResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void range(KvProto.RangeRequest req, StreamObserver<KvProto.RangeResponse> obs) {
        try {
            var result = client.eval(
                    "local since, to = ...; " +
                            "local res = {}; " +
                            "for _, t in box.space.KV.index.primary:pairs(since, {iterator='GE'}) do " +
                            "  if t[1] > to then break end; " +
                            "  table.insert(res, {t[1], t[2]}); " +
                            "end; " +
                            "return res",
                    List.of(req.getKeySince(), req.getKeyTo())
            ).join();

            var rows = result.get();
            if (rows != null && !rows.isEmpty()) {
                for (Object row : (List<?>) rows.get(0)) {
                    List<?> tuple = (List<?>) row;
                    String key = (String) tuple.get(0);
                    byte[] value = tuple.size() > 1 ? (byte[]) tuple.get(1) : null;

                    var builder = KvProto.RangeResponse.newBuilder().setKey(key);
                    if (value != null) {
                        builder.setValue(
                                com.google.protobuf.BytesValue.of(
                                        com.google.protobuf.ByteString.copyFrom(value)
                                )
                        );
                    }
                    obs.onNext(builder.build());
                }
            }

            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}
