package io.github.giovibal.grpc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by giovibal on 17/09/16.
 */
public class PubSub {
    private LinkedHashMap<String, List<Listener>> subscriptions = new LinkedHashMap<>();

    public void publish(PublishMessage m) {
        String topic = m.getTopic();
        List<Listener> listeners = subscriptions.get(topic);
        for(Listener l : listeners)
            l.onMessage(m);
    }

    public void subscribe(SubscribeMessage sub, Listener listener) {
        String topic = sub.getTopic();
        if(subscriptions.containsKey(topic)) {
            List<Listener> listeners = subscriptions.get(topic);
            if(listeners == null)
                listeners = new ArrayList<>();
            listeners.add(listener);
            subscriptions.put(topic, listeners);
        }
        else {
            List<Listener> listeners = new ArrayList<>();
            listeners.add(listener);
            subscriptions.put(topic, listeners);
        }
    }

    public interface Listener {
        void onMessage(PublishMessage m);
    }
}
